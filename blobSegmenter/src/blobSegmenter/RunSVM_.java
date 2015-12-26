package blobSegmenter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;

import ij.plugin.frame.*;
import libsvm.svm_parameter;
import ij.*;
import ij.gui.*;

import ij.io.DirectoryChooser;

/**
	Interactive pipeline to run blob segmenter algorithm
*/
public class RunSVM_ extends PlugInFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private Panel panel;
	private static Frame instance;
	
	private String working_directory;
	private Boolean locked=false;
	
	// svm parameters
	int probability = 1;
	double gamma = 0.5;
	double nu = 0.5;
	double C = 100;
	double eps = 0.001;   	
	
	public RunSVM_() {
		super("Train an SVM and apply to all images");
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
		addButton("Train SVM and apply to all images");
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
				if (isLocked()) {return;}
				DirectoryChooser d = new DirectoryChooser("Choose working directory");
				working_directory = d.getDirectory();
			} else if (command.equals("Train SVM and apply to all images")) {
				if (isLocked()) {return;}
				if (!isWorkingDirectorySet()) {return;}
				
				// load previously used preprocessing parameters if they exist
				File previous_parameters = new File(working_directory + "/misc/svm_parameters.config");
				if (previous_parameters.exists()) {
					double [] klass = null;
					double [] previousParameters = Util.deserialize(working_directory + "/misc/svm_parameters.config", klass);
					probability = (int)previousParameters[0];
					gamma = (double)previousParameters[1];				
					nu = (double)previousParameters[2];
					C = (double)previousParameters[3];
					eps = (double)previousParameters[4];
				}
				
				// get svm parameters
				GenericDialog gd = new GenericDialog("Svm parameters");
				gd.addNumericField("probability: ", probability, 2);
				gd.addNumericField("gamma: ", gamma, 2);
				gd.addNumericField("nu: ", nu, 2);
				gd.addNumericField("C: ", C, 2);
				gd.addNumericField("eps: ", eps, 2);				
				gd.showDialog();
				if (gd.wasCanceled()) return;
				probability = (int)gd.getNextNumber();
				gamma = (double)gd.getNextNumber();				
				nu = (double)gd.getNextNumber();
				C = (double)gd.getNextNumber();
				eps = (double)gd.getNextNumber();				
				
				// run svm
				lock();

				// run svm and get labels
		        SvmBackend svm_backend = new SvmBackend();
		        svm_backend.set_working_directory(working_directory);
		        svm_backend.set_svm_parameters(probability, gamma, nu, C, eps);
		        svm_backend.read_training_data(); // always read and train on all images
		        svm_backend.svmTrain();
		        svm_backend.svmPredict_batch();			
				
				unlock();
				
		        // save svm parameters
		        double [] parameters = new double[]{probability,gamma,nu,C,eps};
		        Util.serialize_object(parameters, working_directory+"/misc/"+"svm_parameters.config");
		        
		        // alert user
				IJ.showMessage("SVM is finished");
				unlock();				
			}
			
			IJ.showStatus((System.currentTimeMillis()-startTime)+" milliseconds");
		}
		

		
		
	} // Runner inner class


	
	private Boolean isWorkingDirectorySet() {
		if (working_directory==null) {
			IJ.showMessage("working directory must be set");
			return false;
		} else {
			return true;
		}
	}
	private Boolean isLocked() {
		if (locked) {
			IJ.showMessage("Wait until image is unlocked");
			return true;
		} else {
			return false;
		}
	}
	private void lock() {
		locked=true;
	}
	private void unlock() {
		locked=false;
	}
	
	
	
} //Preprocess_ class
