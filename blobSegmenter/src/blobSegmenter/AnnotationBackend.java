package blobSegmenter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.process.ImageProcessor;

public class AnnotationBackend {
	private String working_directory;
	private ProposedSegmentation[] proposed_segmentations;
	private ImageProcessor input_image;
	private ImageProcessor watershed_image;
	private ImagePlus annotation_interface;
	
	void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
	}
	
	void open(String name) {		
		// deserialize data for proposed segmentations
		proposed_segmentations = deserialize(working_directory + "/proposed_segmentations/" + name);

		// open up input image and store as byte image
		Opener opener = new Opener();  
		input_image = opener.openImage(working_directory + "/input_images/" + name).getProcessor();		
		Util.normalize_16bit_image_to_8bit_image(input_image);
		
		// open up watershed image
		watershed_image = opener.openImage(working_directory + "/watershed_images/" + name).getProcessor();
			
	}
	
	ImagePlus display() {
		int dimx=input_image.getWidth();
		int dimy=input_image.getHeight();

		IJ.newImage("Annotation interface", "RGB", dimx,  dimy, 1);
		annotation_interface = WindowManager.getImage("Annotation interface");
		
		for (int x=0;x<dimx;x++) {
			for (int y=0;y<dimy;y++) {
				int value = input_image.getPixel(x, y);
				value = packRGB((byte)value,(byte)value,(byte)value);
				annotation_interface.getProcessor().setValue(value);
				annotation_interface.getProcessor().drawPixel(x, y);				
			}
		}
		annotation_interface.updateAndDraw();
		return annotation_interface;
	}

	void drawSegmentation(int x,int y, String color) {
		// get watershed index
		int watershed_index = watershed_image.getPixel(x, y);
		IJ.log("watershed_index = "+watershed_index);
		if (watershed_index==0) {
			return;
		}

		// parse color
		int rgb_color = 0;
		Boolean is_erase = false;
		if (color.equals("red")) {
			rgb_color = packRGB((byte)254,(byte)0,(byte)0);
		} else if (color.equals("green")) {
			rgb_color = packRGB((byte)0,(byte)254,(byte)0);			
		} else if (color.equals("blue")) {
			rgb_color = packRGB((byte)0,(byte)0,(byte)254);			
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
	
	public ProposedSegmentation[] deserialize(String file_path) {
		ProposedSegmentation[] rn = null;
		try {
			FileInputStream fileIn = new FileInputStream(file_path);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			rn = (ProposedSegmentation[]) in.readObject();
			in.close();
			fileIn.close();
			return rn;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return rn;		
	}
	
}
