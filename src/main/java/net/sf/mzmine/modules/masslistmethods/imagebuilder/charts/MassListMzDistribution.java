package net.sf.mzmine.modules.masslistmethods.imagebuilder.charts;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MassListMzDistribution extends JFrame {

	private JPanel contentPane;
	private JPanel pnChartView;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MassListMzDistribution frame = new MassListMzDistribution();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public MassListMzDistribution() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		pnChartView = new JPanel();
		contentPane.add(pnChartView, BorderLayout.CENTER);
		pnChartView.setLayout(new BorderLayout(0, 0));
	}

	public JPanel getPnChartView() {
		return pnChartView;
	}
	
	
	public void createChart(TreeMap<Integer, Double> signals, int decimals) {
		
		XYSeries series = new XYSeries("mz distr");
//		double[][] data = new double[signals.size()][2];

        double factor = Math.pow(10, decimals);

		Iterator<Entry<Integer, Double>> it = signals.entrySet().iterator();
		for (int i = 0; i < signals.size() && it.hasNext(); i++) {
			Entry<Integer, Double> e = it.next();
			double mz = ((double)e.getKey())/factor;
			double n = (e.getValue());
			series.add(mz, n);
		}
		
		
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		SignalDistributionPlot cp = new SignalDistributionPlot("mz dist", "mz", "n", dataset);
//		JFreeChart chart = ChartFactory.createXYLineChart("mz dist", "mz", "n", dataset);
//		ChartPanel cp = new ChartPanel(chart);
		getPnChartView().add(cp, BorderLayout.CENTER);
		getPnChartView().revalidate();
		getPnChartView().repaint();
	}
}
