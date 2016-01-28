package ldSegmenterPlugin;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;

import ij.plugin.frame.*;
import ij.*;
import ij.gui.*;

import ij.io.DirectoryChooser;

/**
	Interactive pipeline to run blob segmenter algorithm
*/
public class Annotate_ extends PlugInFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private Panel panel;
	private static Frame instance;
	private Random random;
	
	private String working_directory;
	private String image_name;

	private ImageCanvas canvas = null;
	
	AnnotationBackend annotation_backend;
	
	// svm parameters
	int probability = 1;
	double gamma = 0.5;
	double nu = 0.5;
	double C = 100;
	double eps = 0.001;   	
	
	public Annotate_() {
		super("Annotate");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		addKeyListener(IJ.getInstance());

		setLayout(new FlowLayout());
		panel = new Panel();
		panel.setLayout(new GridLayout(5, 1, 5, 5));
		addButton("Set workspace");		
		addButton("Open random image");
		addButton("Open specific image");
		addButton("Save annotations");
		addButton("Train SVM and apply to image");
		add(panel);
		
		random = new Random();
		
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


	class Runner extends Thread implements MouseListener { // inner class
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
			} else if (command.equals("Open random image")) {
				if (isLocked()) {return;}
				if (!isWorkingDirectorySet()) {return;}

				// close currently opened image
				if (canvas!=null) {
					canvas.getImage().close();
				}
				
				// get random image name
				LinkedList<String> image_names = new LinkedList<String>();
				File folder = new File(working_directory + "/watershed_images/");						
				for(File child : folder.listFiles()) {
				    if (!child.getAbsolutePath().endsWith(".tif")) {
				        continue;
				    }
				    image_names.add(child.getName());
				}
				int i = random.nextInt(image_names.size());
				image_name = image_names.get(i);
				
				// open annotation interface for image
				open_annotation_interface();
			} else if (command.equals("Open specific image")) {
				if (isLocked()) {return;}
				if (!isWorkingDirectorySet()) {return;}
				//working_directory="/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/";

				// close currently opened image
				if (canvas!=null) {
					canvas.getImage().close();
				}
				
				// get image to open
				GenericDialog gd = new GenericDialog("Open image");
				gd.addStringField("image name", "");
				gd.showDialog();
				if (gd.wasCanceled()) return;
				image_name = gd.getNextString();
				
				// open annotation interface for image
				open_annotation_interface();
			} else if (command.equals("Save annotations")) {
				if (isLocked()) {return;}
				if (!isWorkingDirectorySet()) {return;}
				annotation_backend.serializeTrainingData();
			} else if (command.equals("Train SVM and apply to image")) {
				if (isLocked()) {return;}
				if (!isWorkingDirectorySet()) {return;}
				
				// save current annotations
				annotation_backend.serializeTrainingData();

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
				annotation_backend.trainSVM(probability,gamma,nu,C,eps);
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
		
		private void open_annotation_interface() {
			annotation_backend = null;
			annotation_backend = new AnnotationBackend();
			annotation_backend.set_working_directory(working_directory);        
			annotation_backend.open(image_name);
			ImagePlus I = annotation_backend.display();
			canvas = I.getWindow().getCanvas();
			canvas.addMouseListener(this);
		}
		
		
		
		public void mouseClicked(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {		
		}
		public void mouseExited(MouseEvent e) {		
		}
		public void mousePressed(MouseEvent e) {
			int tool_id = Toolbar.getToolId();
			int x = e.getX();
			int y = e.getY();
			int offscreenX = canvas.offScreenX(x);
			int offscreenY = canvas.offScreenY(y);
			IJ.log("Mouse pressed: "+offscreenX+","+offscreenY+",event="+e.getModifiers()+","+Event.SHIFT_MASK + ", tool="+tool_id);
			
			// tool_id==12 means hand tool selected
			// e.getModifiers == 16 means normal left click
			// e.getModifiers == 17 means shift left click
			// e.getModifiers == 20 means command left click
			// e.getModifiers == 24 means option left click			
			if (tool_id==12 && e.getModifiers()==16 ) { // positive annotation
				if (isLocked()) {return;}
				annotation_backend.drawSegmentation(offscreenX,offscreenY,"green");
				annotation_backend.removeTrainingData(offscreenX, offscreenY);
				annotation_backend.addTrainingData(offscreenX, offscreenY, 1);
				annotation_backend.updateAndDraw();
			} else if (tool_id==12 && e.getModifiers()==17) { // negative annotation (shift)
				if (isLocked()) {return;}
				annotation_backend.drawSegmentation(offscreenX,offscreenY,"red");
				annotation_backend.removeTrainingData(offscreenX, offscreenY);
				annotation_backend.addTrainingData(offscreenX, offscreenY, 0);
				annotation_backend.updateAndDraw();				
			} else if (tool_id==12 && e.getModifiers()==20) { // remove annotation (shift)
				if (isLocked()) {return;}
				annotation_backend.drawSegmentation(offscreenX,offscreenY,"none");
				annotation_backend.removeTrainingData(offscreenX, offscreenY);
				annotation_backend.updateAndDraw();
			}

			
		}
		public void mouseReleased(MouseEvent e) {
		}
		public String modifiers(int flags) {
			String s = " [ ";
			if (flags == 0) return "";
			if ((flags & Event.SHIFT_MASK) != 0) s += "Shift ";
			if ((flags & Event.CTRL_MASK) != 0) s += "Control ";
			if ((flags & Event.META_MASK) != 0) s += "Meta (right button) ";
			if ((flags & Event.ALT_MASK) != 0) s += "Alt ";
			s += "]";
			if (s.equals(" [ ]"))
	 			s = " [no modifiers]";
			return s;
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
		if (canvas==null) {
			return false;
		}
		ImagePlus I = canvas.getImage();
		if (I.isLocked()) {
			IJ.showMessage("Wait until image is unlocked");
			return true;
		}
		return false;
	}
	private void lock() {
		ImagePlus I = canvas.getImage();
		I.lock();
	}
	private void unlock() {
		ImagePlus I = canvas.getImage();
		I.unlock();
	}
	
	
	
} //Preprocess_ class
