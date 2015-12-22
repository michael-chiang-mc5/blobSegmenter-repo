package blobSegmenter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import ij.plugin.frame.*;
import ij.*;
import ij.process.*;
import ij.gui.*;

import ij.io.DirectoryChooser;

/**
	Interactive pipeline to run blob segmenter algorithm
*/
public class Preprocess_ extends PlugInFrame implements ActionListener {
	private Panel panel;
	private static Frame instance;
	
	private String working_directory;
	private float droplet_radius;
	private float droplet_distance;

	public Preprocess_() {
		super("Blob segmenter");
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
					PreprocessingBackend d = new PreprocessingBackend();
					d.set_working_directory(working_directory);
					
					
					GenericDialog gd = new GenericDialog("New Image");
					gd.addNumericField("Droplet radius (pixels): ", droplet_radius, 2);
					gd.addNumericField("Inter-droplet distance (pixels): ", droplet_distance, 2);
					gd.showDialog();
					if (gd.wasCanceled()) return;
					droplet_radius = (float)gd.getNextNumber();
					droplet_distance = (float)gd.getNextNumber();				
					
					blur();
				}
			}
			
			IJ.showStatus((System.currentTimeMillis()-startTime)+" milliseconds");
		}
		
		void blur() {
			float sigma = droplet_distance;
			
		}
		
		
		
	} // Runner inner class

} //Preprocess_ class
