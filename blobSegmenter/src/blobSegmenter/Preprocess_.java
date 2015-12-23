package blobSegmenter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import ij.plugin.frame.*;
import ij.*;
import ij.gui.*;

import ij.io.DirectoryChooser;

/**
	Interactive pipeline to run blob segmenter algorithm
*/
public class Preprocess_ extends PlugInFrame implements ActionListener {
	private Panel panel;
	private static Frame instance;
	
	private String working_directory;
	
	// preprocessing parameters
	private double blur_sigma;
	private int threshold_step_size;
    private int dilate_radius;
	
	public Preprocess_() {
		super("Preprocess");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		addKeyListener(IJ.getInstance());

		setLayout(new FlowLayout());
		panel = new Panel();
		panel.setLayout(new GridLayout(2, 1, 5, 5));
		addButton("Set workspace");		
		addButton("Preprocess");		
		add(panel);
		
		pack();
		GUI.center(this);
		setVisible(true);
	}
	
	void addButton(String label) {
		Button b = new Button(label);
		b.addActionListener(this);
		b.addKeyListener(IJ.getInstance());
		panel.add(b);
	}

	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		new Runner(label);
	}

	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;	
		}
	}


	class Runner extends Thread { // inner class
		private String command;
	
		Runner(String command) {
			super(command);
			this.command = command;
			setPriority(Math.max(getPriority()-2, MIN_PRIORITY));
			start();
		}
	
		public void run() {
			try {
				runCommand(command);
			} catch(OutOfMemoryError e) {
				IJ.outOfMemory(command);
			} catch(Exception e) {
				CharArrayWriter caw = new CharArrayWriter();
				PrintWriter pw = new PrintWriter(caw);
				e.printStackTrace(pw);
				IJ.log(caw.toString());
				IJ.showStatus("");
			}
		}
	
		void runCommand(String command) {
			IJ.showStatus(command + "...");
			long startTime = System.currentTimeMillis();
			if (command.equals("Set workspace")) {
				DirectoryChooser d = new DirectoryChooser("Choose working directory");
				working_directory = d.getDirectory();
			} else if (command.equals("Preprocess")) {
				if (working_directory==null) {
					IJ.showMessage("working directory must be set");
				} else {				
					// get preprocessing parameters
					GenericDialog gd = new GenericDialog("New Image");
					gd.addNumericField("blur sigma (pixels): ", blur_sigma, 2);
					gd.addNumericField("threshold_step_size (pixels): ", threshold_step_size, 2);
					gd.addNumericField("dilate_radius (pixels): ", dilate_radius, 2);				
					gd.showDialog();
					if (gd.wasCanceled()) return;
					blur_sigma = (double)gd.getNextNumber();
					threshold_step_size = (int)gd.getNextNumber();				
					dilate_radius = (int)gd.getNextNumber();
					
					// run preprocessing
			        PreprocessingBackend preprocessing_backend = new PreprocessingBackend();
			        preprocessing_backend.set_working_directory(working_directory);        
			        preprocessing_backend.create_working_directory_substructure();
			        preprocessing_backend.preprocess_batch(blur_sigma,threshold_step_size,dilate_radius);					
					
				}
			}
			
			IJ.showStatus((System.currentTimeMillis()-startTime)+" milliseconds");
		}
		
	
		
		
	} // Runner inner class

} //Preprocess_ class
