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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.parameters.ParameterSet;

public class DataPointProcessingQueue
    extends Vector<MZmineProcessingStep<DataPointProcessingModule>> {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(DataPointProcessingQueue.class.getName());

  private static final String DATA_POINT_PROCESSING_STEP_ELEMENT = "processingstep";
  private static final String METHOD_ELEMENT = "method";

  public static @Nonnull DataPointProcessingQueue loadfromXML(final @Nonnull Element xmlElement) {
    DataPointProcessingQueue queue = new DataPointProcessingQueue();

    // Get the loaded modules.
    final Collection<MZmineModule> allModules = MZmineCore.getAllModules();

    // Process the processing step elements.
    final NodeList nodes = xmlElement.getElementsByTagName(DATA_POINT_PROCESSING_STEP_ELEMENT);
    final int nodesLength = nodes.getLength();

    for (int i = 0; i < nodesLength; i++) {

      final Element stepElement = (Element) nodes.item(i);
      final String methodName = stepElement.getAttribute(METHOD_ELEMENT);
      logger.finest("loading method " + methodName);

      for (MZmineModule module : allModules) {
        if (module instanceof DataPointProcessingModule
            && module.getClass().getName().equals(methodName)) {

          // since the same module can be used in different ms levels, we need to clone the
          // parameter set, so we can have different values for every ms level
          ParameterSet parameterSet = MZmineCore.getConfiguration()
              .getModuleParameters(module.getClass()).cloneParameterSet();

          parameterSet.loadValuesFromXML(stepElement);
          queue.add(new MZmineProcessingStepImpl<DataPointProcessingModule>(
              (DataPointProcessingModule) module, parameterSet));
          // add to treeView
          break;
        }

      }

    }
    return queue;
  }

  public static @Nonnull DataPointProcessingQueue loadFromFile(@Nonnull File file) {
    try {
      Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
          .getDocumentElement();
      return loadfromXML(element);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      e.printStackTrace();
      return new DataPointProcessingQueue();
    }
  }

  public void saveToXML(final @Nonnull Element xmlElement) {

    final Document document = xmlElement.getOwnerDocument();

    // Process each step.
    for (final MZmineProcessingStep<?> step : this) {

      // Append a new batch step element.
      final Element stepElement = document.createElement(DATA_POINT_PROCESSING_STEP_ELEMENT);

      stepElement.setAttribute(METHOD_ELEMENT, step.getModule().getClass().getName());
      xmlElement.appendChild(stepElement);

      // Save parameters.
      final ParameterSet parameters = step.getParameterSet();
      if (parameters != null) {
        parameters.saveValuesToXML(stepElement);
      }
    }
  }

  public void saveToFile(final @Nonnull File file) {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      final Element element = document.createElement("DataPointProcessing");
      document.appendChild(element);

      // Serialize batch queue.
      this.saveToXML(element);

      // Create transformer.
      final Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Write to file and transform.
      transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));

      logger.finest("Saved " + this.size() + " processing step(s) to " + file.getName());

    } catch (ParserConfigurationException | TransformerFactoryConfigurationError
        | FileNotFoundException | TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }

  }

  /**
   * 
   * @return Returns true if the module list is initialized and > 0.
   */
  public boolean stepsValid() {
    if (!this.isEmpty())
      return true;
    return false;
  }

  /**
   * 
   * @param current A pointer to the current module.
   * @return Returns true if there is one or more steps, false if not.
   */
  public boolean hasNextStep(MZmineProcessingStep<DataPointProcessingModule> current) {
    if (this.contains(current)) {
      int index = this.indexOf(current);
      if (index + 1 < this.size()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param current A pointer to the current module.
   * @return Returns the next module in this PlotModuleCombo. If this pmc has no next module the
   *         return is null. Use hasNextModule to check beforehand.
   */
  public @Nullable MZmineProcessingStep<DataPointProcessingModule> getNextStep(
      @Nonnull MZmineProcessingStep<DataPointProcessingModule> current) {
    if (hasNextStep(current))
      return this.get(this.indexOf(current) + 1);
    return null;
  }

  /**
   * 
   * @return Returns the first module in this PlotModuleCombo. If the list of steps is not
   *         initialised, the return is null.
   */
  public @Nullable MZmineProcessingStep<DataPointProcessingModule> getFirstStep() {
    if (this.size() > 0) {
      return this.get(0);
    }
    return null;
  }

  public DataPointProcessingQueue clone() {
    DataPointProcessingQueue clone = new DataPointProcessingQueue();

    for (int i = 0; i < this.size(); i++) {
      clone.add(new MZmineProcessingStepImpl<DataPointProcessingModule>(this.get(i).getModule(),
          this.get(i).getParameterSet().cloneParameterSet()));
    }

    return clone;
  }
}
