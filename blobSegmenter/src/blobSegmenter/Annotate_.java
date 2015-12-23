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
public class Annotate_ extends PlugInFrame implements ActionListener {
	private Panel panel;
	private static Frame instance;
	
	private String working_directory;
	private String image_name;

	private ImageCanvas canvas;

	AnnotationBackend annotation_backend;
	
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
		panel.setLayout(new GridLayout(3, 1, 5, 5));
		addButton("Set workspace");		
		addButton("Open random image");
		addButton("Open specific image");		
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
				DirectoryChooser d = new DirectoryChooser("Choose working directory");
				working_directory = d.getDirectory();
			} else if (command.equals("Open random image")) {
				if (working_directory==null) {
					IJ.showMessage("working directory must be set");
				} else {
					
				}
			} else if (command.equals("Open specific image")) {
				working_directory="/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/";
				if (working_directory==null) {
					IJ.showMessage("working directory must be set");
				} else {
					// get preprocessing parameters
					GenericDialog gd = new GenericDialog("Open image");
					gd.addStringField("image name", "94.tif");
					gd.showDialog();
					if (gd.wasCanceled()) return;
					image_name = gd.getNextString();
					
					// set up annotation interface
					annotation_backend = null;
					annotation_backend = new AnnotationBackend();
					annotation_backend.set_working_directory(working_directory);        
					annotation_backend.open(image_name);
					ImagePlus I = annotation_backend.display();
					canvas = I.getWindow().getCanvas();
					canvas.addMouseListener(this);
				}
			}
			
			IJ.showStatus((System.currentTimeMillis()-startTime)+" milliseconds");
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
				annotation_backend.drawSegmentation(offscreenX,offscreenY,"green");
				annotation_backend.updateAndDraw();
			} else if (tool_id==12 && e.getModifiers()==17) { // negative annotation (shift)
				annotation_backend.drawSegmentation(offscreenX,offscreenY,"red");
				annotation_backend.updateAndDraw();				
			} else if (tool_id==12 && e.getModifiers()==20) { // remove annotation (shift)
				annotation_backend.drawSegmentation(offscreenX,offscreenY,"none");
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


	



	
	
	
} //Preprocess_ class
