package ldSegmenterPlugin;

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

	private static final long serialVersionUID = 1L;
	private Panel panel;
	private static Frame instance;
	
	private String working_directory;
	private Boolean locked=false;
	
	// preprocessing parameters
	private double blur_sigma=1;
	private int threshold_step_size=10;
    private int dilate_radius=1;
	
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
				if (isLocked()) {return;}
				DirectoryChooser d = new DirectoryChooser("Choose working directory");
				working_directory = d.getDirectory();
			} else if (command.equals("Preprocess")) {
				if (isLocked()) {return;}
				if (!isWorkingDirectorySet()) {return;}

				lock();
				// load previously used preprocessing parameters if they exist
				File previous_parameters = new File(working_directory + "/misc/preprocessing_parameters.config");
				if (previous_parameters.exists()) {
					double [] klass = null;
					double [] previousParameters = Util.deserialize(working_directory + "/misc/preprocessing_parameters.config", klass);
					blur_sigma = previousParameters[0];
					threshold_step_size = (int)previousParameters[1];
					dilate_radius = (int)previousParameters[2];
				}
					
				GenericDialog gd = new GenericDialog("Preprocessing parameters");
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
				
		        // save preprocessing parameters
		        double [] parameters = new double[]{blur_sigma,threshold_step_size,dilate_radius};
		        Util.serialize_object(parameters, working_directory+"/misc/"+"preprocessing_parameters.config");
		        
		        // alert user
				IJ.showMessage("Preprocessing is finished. You may proceed to annotation");
				unlock();
			}
			
			IJ.showStatus((System.currentTimeMillis()-startTime)+" milliseconds");
		}
		
	
		
		
	} // Runner inner class

	
	private void lock() {
		locked = true;
	}
	private void unlock() {
		locked = false;
	}
	private Boolean isLocked() {
		if (locked) {
			IJ.showMessage("Wait until preprocessing is finished");
		}
		return locked;
	}
	private Boolean isWorkingDirectorySet() {
		if (working_directory==null) {
			IJ.showMessage("working directory must be set");
			return false;
		} else {
			return true;
		}
	}
	
} //Preprocess_ class
