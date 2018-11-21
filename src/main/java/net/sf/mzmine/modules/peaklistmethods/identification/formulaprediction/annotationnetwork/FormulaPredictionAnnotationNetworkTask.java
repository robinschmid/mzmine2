/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.annotationnetwork;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.datamodel.identities.iontype.MSAnnotationNetworkLogic;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.createavgformulas.CreateAvgNetworkFormulasTask;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.datastructure.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.singlerow.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.singlerow.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.sort.FormulaSortParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.sort.FormulaSortTask;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScore;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class FormulaPredictionAnnotationNetworkTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;
  private double searchedMass;
  private PeakList peakList;
  private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
  private ParameterSet isotopeParameters, msmsParameters, ratiosParameters, rdbeParameters;
  private MZTolerance mzTolerance;
  private String message;
  private int totalRows;
  private AtomicInteger finishedNets = new AtomicInteger(0);
  // correct values by ppm offset to shift correct molecular formulae to the center
  // usefull if all exact masses are shifted by 4 ppm enter -4 ppm
  private double ppmOffset;
  private double isotopeNoiseLevel;
  private double minScore;
  private String massListName;
  private double minMSMSScore;
  private boolean sortResults;
  private FormulaSortTask sorter;
  private CreateAvgNetworkFormulasTask netFormulaMerger;

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  FormulaPredictionAnnotationNetworkTask(PeakList peakList, ParameterSet parameters) {

    /*
     * searchedMass = parameters.getParameter(
     * FormulaPredictionPeakListParameters.neutralMass).getValue();
     */
    this.peakList = peakList;
    mzTolerance = parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.mzTolerance)
        .getValue();
    elementCounts =
        parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.elements).getValue();

    ppmOffset =
        parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.ppmOffset).getValue();


    checkRDBE = parameters
        .getParameter(FormulaPredictionAnnotationNetworkParameters.rdbeRestrictions).getValue();
    if (checkRDBE) {
      rdbeParameters =
          parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.rdbeRestrictions)
              .getEmbeddedParameters();
    }

    checkRatios = parameters
        .getParameter(FormulaPredictionAnnotationNetworkParameters.elementalRatios).getValue();
    if (checkRatios) {
      ratiosParameters =
          parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.elementalRatios)
              .getEmbeddedParameters();
    }


    checkIsotopes = parameters
        .getParameter(FormulaPredictionAnnotationNetworkParameters.isotopeFilter).getValue();
    if (checkIsotopes) {
      isotopeParameters =
          parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.isotopeFilter)
              .getEmbeddedParameters();
      isotopeNoiseLevel = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopeNoiseLevel).getValue();
      minScore = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();
    }

    checkMSMS =
        parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.msmsFilter).getValue();
    if (checkMSMS) {
      msmsParameters =
          parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.msmsFilter)
              .getEmbeddedParameters();
      massListName = msmsParameters.getParameter(MSMSScoreParameters.massList).getValue();
      minMSMSScore = msmsParameters.getParameter(MSMSScoreParameters.msmsMinScore).getValue();
    }

    sortResults =
        parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.sorting).getValue();
    if (sortResults) {
      FormulaSortParameters sortingParam =
          parameters.getParameter(FormulaPredictionAnnotationNetworkParameters.sorting)
              .getEmbeddedParameters();
      sorter = new FormulaSortTask(sortingParam);
    }

    // merger to create avg formulas
    netFormulaMerger = new CreateAvgNetworkFormulasTask(sorter);
    message = "Formula Prediction (MS annotation networks)";
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return finishedNets.get() / (double) totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return message;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // get all networks to run in parallel
    AnnotationNetwork[] nets = MSAnnotationNetworkLogic.getAllNetworks(peakList);
    totalRows = nets.length;
    if (totalRows == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No annotation networks found in this list. Run MS annotation");
      cancel();
      return;
    }

    // parallel
    Arrays.stream(nets).forEach(net -> {
      message = "Formula prediction on network " + net.getID();
      if (!isCanceled()) {
        predictFormulasForNetwork(net);
      }
      finishedNets.incrementAndGet();
    });

    logger.finest("Finished formula search for all networks");
    setStatus(TaskStatus.FINISHED);
  }

  public List<MolecularFormulaIdentity> predictFormulasForNetwork(AnnotationNetwork net) {
    for (Entry<PeakListRow, IonIdentity> e : net.entrySet()) {
      PeakListRow r = e.getKey();
      IonIdentity ion = e.getValue();
      if (!ion.getIonType().isUndefinedAdduct()) {
        List<MolecularFormulaIdentity> list = predictFormulas(r, ion.getIonType());
        if (!list.isEmpty()) {
          if (sortResults && sorter != null)
            sorter.sort(list, ion.getIonType().getMass(r.getAverageMZ()));
          ion.setMolFormulas(list);
        }
      }
    }
    // find best formula for neutral mol of network
    // add all that have the same mol formula in at least 2 different ions (rows)
    if (netFormulaMerger != null) {
      return netFormulaMerger.combineFormulasOfNetwork(net);
    }
    return null;
  }

  private List<MolecularFormulaIdentity> predictFormulas(PeakListRow row, IonType ion) {
    List<MolecularFormulaIdentity> resultingFormulas = new ArrayList<>();
    this.searchedMass = ion.getMass(row.getAverageMZ());
    // correct by ppm offset
    searchedMass += searchedMass * ppmOffset / 1E6;

    massRange = mzTolerance.getToleranceRange(searchedMass);

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
        massRange.upperEndpoint(), elementCounts);

    IMolecularFormula cdkFormula;
    while ((cdkFormula = generator.getNextFormula()) != null) {
      try {
        // ionized formula
        IMolecularFormula cdkFormulaIon = ion.addToFormula(cdkFormula);

        if (ion.getModCount() > 0)
          logger.info(MessageFormat.format("Checking type {0} as {1} ({3}) for neutral {2} ({4})",
              ion.toString(), MolecularFormulaManipulator.getString(cdkFormulaIon),
              MolecularFormulaManipulator.getString(cdkFormula),
              MolecularFormulaManipulator.getTotalExactMass(cdkFormulaIon),
              MolecularFormulaManipulator.getTotalExactMass(cdkFormula)));

        // Mass is ok, so test other constraints
        checkConstraints(resultingFormulas, cdkFormula, cdkFormulaIon, row, ion);
      } catch (CloneNotSupportedException e) {
        logger.log(Level.SEVERE, "Cannot copy cdk formula", e);
        throw new MSDKRuntimeException(e);
      }
    }

    return resultingFormulas;
  }

  private void checkConstraints(List<MolecularFormulaIdentity> resultingFormulas,
      IMolecularFormula cdkFormulaNeutralM, IMolecularFormula cdkFormulaIon,
      PeakListRow peakListRow, IonType ionType) {
    int charge = ionType.getCharge();

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormulaNeutralM, ratiosParameters);
      if (!check) {
        return;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormulaNeutralM);
    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check) {
        return;
      }
    }

    // Calculate isotope similarity score
    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    IsotopePattern predictedIsotopePattern = null;
    Double isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {
      final double detectedPatternHeight = detectedPattern.getHighestDataPoint().getIntensity();
      final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

      predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(cdkFormulaIon,
          minPredictedAbundance, charge, ionType.getPolarity());

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeParameters);
      if (isotopeScore < minScore) {
        return;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Double msmsScore = null;
    Map<DataPoint, String> msmsAnnotations = null;

    // there was a problem in the RoundRobinMoleculaFormulaGenerator (index out of range
    try {
      if (checkMSMS && peakListRow.getBestFragmentation() != null) {
        Scan msmsScan = peakListRow.getBestFragmentation();
        MassList ms2MassList = msmsScan.getMassList(massListName);
        if (ms2MassList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("The MS/MS scan #" + msmsScan.getScanNumber() + " in file "
              + msmsScan.getDataFile().getName() + " does not have a mass list called '"
              + massListName + "'");
          return;
        }

        MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormulaIon, msmsScan, msmsParameters);

        if (score != null) {
          msmsScore = score.getScore();
          msmsAnnotations = score.getAnnotation();

          // Check the MS/MS condition
          if (msmsScore < minMSMSScore) {
            return;
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING,
          () -> MessageFormat.format(
              "Error in MS/MS score calculation for ion formula {0} (for neutral M: {1})",
              MolecularFormulaManipulator.getString(cdkFormulaIon),
              MolecularFormulaManipulator.getString(cdkFormulaNeutralM)));
    }

    // Create a new formula entry
    final ResultFormula resultEntry = new ResultFormula(cdkFormulaNeutralM, predictedIsotopePattern,
        rdbeValue, isotopeScore, msmsScore, msmsAnnotations);

    // Add the new formula entry
    resultingFormulas.add(resultEntry);
  }

  @Override
  public void cancel() {
    super.cancel();

    // We need to cancel the formula generator, because searching for next
    // candidate formula may take a looong time
    if (generator != null) {
      generator.cancel();
    }

  }
}
