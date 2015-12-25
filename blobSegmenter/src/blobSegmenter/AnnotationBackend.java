package blobSegmenter;

import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.process.ImageProcessor;

public class AnnotationBackend {
	private String working_directory;
	private String name;
	
	private ProposedSegmentation[] proposed_segmentations;
	private ImageProcessor input_image;
	private ImageProcessor watershed_image;
	private ImagePlus annotation_interface;
	private TrainingData training_data;

	
	public void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
	}
	
	void open(String name) {
		this.name = name;
		
		// deserialize data for proposed segmentations
		ProposedSegmentation[] klass = null;
		proposed_segmentations = Util.deserialize(working_directory + "/proposed_segmentations/" + name, klass);

		// open up input image and store as byte image // TODO: store as byte image to save casts
		Opener opener = new Opener();  
		input_image = opener.openImage(working_directory + "/input_images/" + name).getProcessor();		
		Util.normalize_16bit_image_to_8bit_image(input_image);
		
		// open up watershed image and store
		watershed_image = opener.openImage(working_directory + "/watershed_images/" + name).getProcessor();
	
		// check if previous annotations exist
		String file_path = working_directory + "/training_data/" + name;
		File file = new File(file_path);
		if (file.exists()) {
			TrainingData klass2 = null;
			training_data = Util.deserialize(file_path, klass2);
			if (training_data == null) {
				training_data = new TrainingData(); // user might save empty data
			}
		} else {
			training_data = new TrainingData(); // empty training data if none exists
		}
	}
	
	ImagePlus display() {
		int dimx=input_image.getWidth();
		int dimy=input_image.getHeight();

		IJ.newImage("Annotation interface for "+name, "RGB", dimx,  dimy, 1);
		annotation_interface = WindowManager.getImage("Annotation interface for "+name);
		
		// draw base image
		for (int x=0;x<dimx;x++) {
			for (int y=0;y<dimy;y++) {
				int value = input_image.getPixel(x, y);
				value = packRGB((byte)value,(byte)value,(byte)value);
				annotation_interface.getProcessor().setValue(value);
				annotation_interface.getProcessor().drawPixel(x, y);				
			}
		}
		
		// draw previous annotations
		for (int i=0;i<training_data.size();i++) {
			int watershed_index = training_data.getWatershedIndex(i);
			int label = training_data.getLabel(i);
			IJ.log("label="+label);
			if (label==0) {
				drawSegmentation(watershed_index, "red");
			} else if (label==1) {
				drawSegmentation(watershed_index, "green");
			}			
		}
		
		annotation_interface.updateAndDraw();
		return annotation_interface;
	}

	void addTrainingData(int x,int y, int label) {
		// get watershed index
		int watershed_index = watershed_image.getPixel(x, y);
		if (watershed_index==0) {
			return;
		}
		
		// add training data
		training_data.add(proposed_segmentations[watershed_index], label);		
	}
	
	void removeTrainingData(int x,int y) {
		// get watershed index
		int watershed_index = watershed_image.getPixel(x, y);
		if (watershed_index==0) {
			return;
		}
		
		// add training data
		training_data.remove(proposed_segmentations[watershed_index]);		
	}
	
	void serializeTrainingData() {
		String output_file_path = working_directory + "/training_data/" + name;
		Util.serialize_object(training_data, output_file_path);
	}
	
	// color = "red", "green", "blue"
	// if not one of the three, draws in background color
	void drawSegmentation(int watershed_index, String color) {
		// parse color
		int rgb_color = 0;
		Boolean is_erase = false;
		if (color.equals("red")) {
			rgb_color = packRGB((byte)254,(byte)0,(byte)0);
		} else if (color.equals("green")) {
			rgb_color = packRGB((byte)0,(byte)254,(byte)0);			
		} else if (color.equals("blue")) {
			rgb_color = packRGB((byte)0,(byte)0,(byte)254);			
		} else if (color.equals("yellow")) {
			rgb_color = packRGB((byte)254,(byte)254,(byte)0);
		} else {
			is_erase = true;
		}
		
		// draw segmentation outline
		if (!is_erase) {
			annotation_interface.getProcessor().setValue(rgb_color);
		}
		for (int i=0;i<proposed_segmentations[watershed_index].segmentation_perimeter_x.size();i++) {
			int x_coordinate=proposed_segmentations[watershed_index].segmentation_perimeter_x.get(i);
			int y_coordinate=proposed_segmentations[watershed_index].segmentation_perimeter_y.get(i);
			if (is_erase) {
				rgb_color = input_image.getPixel(x_coordinate, y_coordinate);
				rgb_color = packRGB((byte)rgb_color,(byte)rgb_color,(byte)rgb_color);				
				annotation_interface.getProcessor().setValue(rgb_color);
			}
			annotation_interface.getProcessor().drawPixel(x_coordinate, y_coordinate);				
		}		
	}
	
	void drawSegmentation(int x,int y, String color) {
		// get watershed index
		int watershed_index = watershed_image.getPixel(x, y);
		if (watershed_index==0) {
			return;
		}
		
		//
		drawSegmentation(watershed_index, color);
	}
	
	void updateAndDraw() {
		annotation_interface.updateAndDraw();
	}
	
	// http://stackoverflow.com/questions/8091022/how-do-you-append-two-bytes-to-an-int
	// http://stackoverflow.com/questions/3764226/convert-rbg-char-array-to-rgb-int-value-in-java
	int packRGB(byte r, byte g, byte b) {
		//int rn = (r<<16) + (g<<8) + b;
		//return rn;
		int value = ((255 & 0xFF) << 24) | //alpha
	            (((int)r & 0xFF) << 16) | //red
	            (((int)g & 0xFF) << 8)  | //green
	            (((int)b & 0xFF) << 0); //blue		
		return value;
	}	
	// See http://imagej.net/Introduction_into_Developing_Plugins
	int [] unpackRGB(int value) {
        int red = value & 0xff;
        int green = (value >> 8) & 0xff;
        int blue = (value >> 16) & 0xff;
        int [] rn = {red,green,blue};
        return rn;
	}

	void draw(String file_path) {
        ImagePlus annotation_interface = IJ.createImage("segmentations", input_image.getWidth(), input_image.getHeight(), 1, 16);        
		//ImagePlus annotation_interface = NewImage.createShortImage("annotation interface", input_image.getWidth(), input_image.getHeight(), 1, 0);
		ImageProcessor ai = annotation_interface.getProcessor();
		ai.setValue(10);
		
		for (int i=1;i<proposed_segmentations.length;i++) {
			for (int j=0; j<proposed_segmentations[i].segmentation_perimeter_x.size();j++) {
				int x = proposed_segmentations[i].segmentation_perimeter_x.get(j);
				int y = proposed_segmentations[i].segmentation_perimeter_y.get(j);				
		        ai.drawPixel(x,y);
			}
		}
		IJ.save(annotation_interface,file_path);
	}
	
	void trainSVM(int probability, double gamma, double nu, double C, double eps) {
        System.out.println("running predict");

		// run svm and get labels
        SvmBackend svm_backend = new SvmBackend();
        svm_backend.set_working_directory(working_directory);
        svm_backend.set_svm_parameters(probability, gamma, nu, C, eps);
        svm_backend.read_training_data(); // always read and train on all images
        svm_backend.svmTrain();
        double [] labels = svm_backend.svmPredict(proposed_segmentations);

        // draw classifications
        System.out.println("drawing classification");

		for (int i=1;i<labels.length;i++) {			
			int watershed_index = proposed_segmentations[i].watershed_index;
			double label = labels[i];
			if (label==0) {// TODO: change this to -1
				drawSegmentation(watershed_index, "none");
			} else if (label==1) {
				drawSegmentation(watershed_index, "yellow");
			} else {
				System.out.println("Error: label is not 0,1");
			}
		}
		
        // draw annotations (overwriting classification)
        System.out.println("drawing annotation");
		for (int i=0;i<training_data.size();i++) {
			int watershed_index = training_data.getWatershedIndex(i);
			int label = training_data.getLabel(i);
			if (label==0) {
				drawSegmentation(watershed_index, "red");
			} else if (label==1) {
				drawSegmentation(watershed_index, "green");
			}			
		}
        System.out.println("done with drawing");
		updateAndDraw();
		
        
		
	}
	
}
