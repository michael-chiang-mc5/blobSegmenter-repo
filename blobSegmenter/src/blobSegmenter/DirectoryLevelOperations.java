package blobSegmenter;

import java.awt.Image;
import java.io.File;

// ImageJ imports
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.*;
import ij.gui.NewImage;

// MorphoLibJ imports, see http://fiji.sc/MorphoLibJ
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.watershed.Watershed;

public class DirectoryLevelOperations {
	private String working_directory;
	
	void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
		sanitize_working_directory();
	}
	
	void sanitize_working_directory() {		
		File blurred_folder = new File(working_directory + "/blurred_images/");
		if (!blurred_folder.exists()) {
			blurred_folder.mkdirs();
		}
		File watershed_folder = new File(working_directory + "/watershed_images/");
		if (!watershed_folder.exists()) {
			watershed_folder.mkdirs();
		}		
		
	}
	
	File[] get_list_of_input_image_files() {
		File folder = new File(working_directory + "/input_images/");	
		File[] listOfFiles = folder.listFiles();
		return listOfFiles;
	}

	
	void blur_batch(double blur_sigma) {
		File[] input_images = get_list_of_input_image_files();
		for (File file : input_images) {
			blur(file.getAbsolutePath(),working_directory + "/blurred_images/" + file.getName(),blur_sigma);
		}	
	}
	
	void blur(String input_file_path, String output_file_path, double blur_sigma) {
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(input_file_path);
		ImageProcessor ip = imp.getProcessor(); // ImageProcessor from ImagePlus 
		ip.blurGaussian(blur_sigma);
		IJ.save(imp,output_file_path);
	}
	
	void watershed(String input_file_path, String output_file_path) {
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(input_file_path);	
		int[] dimensions = imp.getDimensions();
		ImageProcessor ip = imp.getProcessor(); 
		ip.invert();
		
		ImagePlus mask = NewImage.createByteImage("mask",dimensions[0],dimensions[1],1,0);
		//IJ.save(mask,output_file_path);
		
		ImagePlus resultStack = Watershed.computeWatershed( imp	, mask, 6 );
		IJ.save(resultStack,output_file_path);

	}
	
}

