package net.sf.mzmine.modules.masslistmethods.imagebuilder;

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
	
	
	public void createChart(TreeMap<Integer, Integer> signals, int decimals) {
		Iterator<Entry<Integer, Integer>> it = signals.entrySet().iterator();
		
		XYSeries series = new XYSeries("mz distr");
//		double[][] data = new double[signals.size()][2];

        double factor = Math.pow(10, decimals);
		
		for (int i = 0; i < signals.size() && it.hasNext(); i++) {
			Entry<Integer, Integer> e = it.next();
			double mz = ((double)e.getKey())/factor;
			double n = ((double)e.getValue());
			series.add(mz, n);
		}
		
		
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYLineChart("mz dist", "mz", "n", dataset);

//		XYBarDataset dataset = new XYBarDataset(new XYSeriesCollection(), 1);
//		JFreeChart chart = ChartFactory.createXYBarChart("Distribution", "m/z", false, "n", dataset);
		ChartPanel cp = new ChartPanel(chart);
		getPnChartView().add(cp, BorderLayout.CENTER);
		getPnChartView().revalidate();
		getPnChartView().repaint();
	}
}
