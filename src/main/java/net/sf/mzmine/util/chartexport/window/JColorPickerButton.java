package net.sf.mzmine.util.chartexport.window;

// ColorPicker.java
// A quick test of the JColorChooser dialog.
//
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;

public class JColorPickerButton extends JButton { 
	JDialog   dialog;
	JColorChooser chooser = new JColorChooser(); 
	Component parentFrame; 
	// changelistener
	protected ColorChangedListener colorChangedListener;

	public JColorPickerButton(Component parentFrame) {   
		super();
		this.parentFrame = parentFrame;   
		setPreferredSize(new Dimension(25, 25));
		this.addActionListener(new ActionListener() { 
			@Override
			public void actionPerformed(ActionEvent e) {
				showDialog();
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		//super.paint(g);
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.WHITE);
		g.drawRect(0, 0, getWidth(), getHeight());
	}





	public void colorChanged(Color color) {
		this.setBackground(color);
		if(colorChangedListener!=null) colorChangedListener.colorChanged(color); 
	}
	

	class OkListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Color color = chooser.getColor(); 
			colorChanged(color);
		}
	}
	class CancelListener implements ActionListener {
		public void actionPerformed(ActionEvent e) { 
		}
	} 
	
	public void showDialog() {  
		chooser.setColor(this.getBackground());
		//
		// New Dialog
		try {
			if(dialog == null)
				dialog   = JColorChooser.createDialog(
						parentFrame, // parent comp
						"Pick A Color",  // dialog title
						false,        // modality
						chooser,    
						new OkListener(), 
						new CancelListener());

			dialog.setVisible(true);
			//  
		} catch(Exception ex) {
			ex.printStackTrace(); 
		}
		if(dialog!=null) {
			dialog.setVisible(true);
		}
	} 
	
	public void addColorChangedListener(ColorChangedListener colorChangedListener) {
		this.colorChangedListener = colorChangedListener;
	}

	public Color getColor() { 
		return getBackground();
	}
	/**
	 * same as color changed
	 * @param c
	 */
	public void setColor(Color c) {
		colorChanged(c);
	}
}
