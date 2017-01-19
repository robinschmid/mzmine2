package net.sf.mzmine.util.chartexport.window;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class DialogLoggerUtil {

	/*
	 * Dialogs
	 */
	public static void showErrorDialog(Component parent, String message, Exception e) {
		JOptionPane.showMessageDialog(parent, message+" \n"+e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE); 
	}
	public static void showErrorDialog(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE); 
	}
	public static void showMessageDialog(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE); 
	}
	
	public static boolean showDialogYesNo(Component parent, String title, String text) {
		Object[] options = {"Yes", "No"};
        int n = JOptionPane.showOptionDialog(parent,
			    text,
			    title,
			    JOptionPane.YES_NO_OPTION,
			    JOptionPane.QUESTION_MESSAGE,
			    null,
			    options,
			    options[0]); 
        return n==0;
	}
	
	/**
	 * shows a message dialog just for a few given milliseconds
	 * @param parent
	 * @param title
	 * @param message
	 * @param time
	 */
	public static void showMessageDialogForTime(JFrame parent, String title, String message, long time) {
		TimeDialog dialog = new TimeDialog(parent, time);
		dialog.setLayout(new FlowLayout(FlowLayout.LEFT));
		dialog.add(new JLabel(message));
		dialog.setTitle(title);
		dialog.pack();
		centerOnScreen(dialog, true);
		dialog.startDialog();
	}
	
	/**
	 *  Center on screen ( abslute true/false (exact center or 25% upper left) )o
	 * @param c
	 * @param absolute
	 */
	public static void centerOnScreen(final Component c, final boolean absolute) {
	    final int width = c.getWidth();
	    final int height = c.getHeight();
	    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    int x = (screenSize.width / 2) - (width / 2);
	    int y = (screenSize.height / 2) - (height / 2);
	    if (!absolute) {
	        x /= 2;
	        y /= 2;
	    }
	    c.setLocation(x, y);
	}

	/**
	 *  Center on parent ( absolute true/false (exact center or 25% upper left) )
	 * @param child
	 * @param absolute
	 */
	public static void centerOnParent(final Window child, final boolean absolute) {
	    child.pack();
	    boolean useChildsOwner = child.getOwner() != null ? ((child.getOwner() instanceof JFrame) || (child.getOwner() instanceof JDialog)) : false;
	    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    final Dimension parentSize = useChildsOwner ? child.getOwner().getSize() : screenSize ;
	    final Point parentLocationOnScreen = useChildsOwner ? child.getOwner().getLocationOnScreen() : new Point(0,0) ;
	    final Dimension childSize = child.getSize();
	    childSize.width = Math.min(childSize.width, screenSize.width);
	    childSize.height = Math.min(childSize.height, screenSize.height);
	    child.setSize(childSize);        
	    int x;
	    int y;
	    if ((child.getOwner() != null) && child.getOwner().isShowing()) {
	        x = (parentSize.width - childSize.width) / 2;
	        y = (parentSize.height - childSize.height) / 2;
	        x += parentLocationOnScreen.x;
	        y += parentLocationOnScreen.y;
	    } else {
	        x = (screenSize.width - childSize.width) / 2;
	        y = (screenSize.height - childSize.height) / 2;
	    }
	    if (!absolute) {
	        x /= 2;
	        y /= 2;
	    }
	    child.setLocation(x, y);
	}
	
	//################################################################################################################
	// internal dialog classes
	private static class TimeDialog extends JDialog implements Runnable {
		long time;
		
		public TimeDialog(JFrame parent, long time) {
			super(parent);
			this.time = time;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				this.setVisible(false);
				this.dispose(); 
			}
		}
		
		public void startDialog() { 
			setVisible(true);
			new Thread(this).start();
		}
	}
}
