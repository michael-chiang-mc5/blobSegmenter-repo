package blobSegmenter;

import java.io.File;
import java.util.Vector;

// ImageJ imports
import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.*;
import ij.gui.NewImage;

// MorphoLibJ imports, see http://fiji.sc/MorphoLibJ
import inra.ijpb.watershed.Watershed;

public class DirectoryLevelOperations {
	private String working_directory;

	// Set working directory. Check for proper directory structure
	void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
		check_subfolder_exists("blurred_images");
		check_subfolder_exists("watershed_images");
		check_subfolder_exists("proposed_segmentations");
	}
	void check_subfolder_exists(String sub_directory) {		
		File folder = new File(working_directory + "/" + sub_directory + "/");
		if (!folder.exists()) {
			folder.mkdirs();
		}	
	}
	
	// Run preprocessing across all input images. Preprocessing is composed of:
	//   (1) blurring by blur_sigma
	//   (2) running watershed on blurred image
	//   (3) Finding a proposed lipid droplet segmentation in each watershed catchment basin
	//		 Each proposed segmentation is characterized.
	void preprocess_batch(double blur_sigma, int threshold_step_size, int dilate_radius) {
		String[] input_images = get_list_of_image_files("input_images");
        for (int i=0;i<input_images.length;i++) {
            blur(working_directory+"/input_images/"+input_images[i],working_directory+"/blurred_images/"+input_images[i],blur_sigma);
            watershed(working_directory+"/blurred_images/"+input_images[i],working_directory+"/watershed_images/"+input_images[i]);
            proposed_segmentations(working_directory+"/blurred_images/"+input_images[i],working_directory+"/watershed_images/"+input_images[i], working_directory+"/proposed_segmentations/"+input_images[i],threshold_step_size,dilate_radius);
        }
	}
	String[] get_list_of_image_files(String subdirectory) {
		File folder = new File(working_directory + "/" + subdirectory + "/");	
		String[] listOfFiles = folder.list();		
		return listOfFiles;
	}
	
	// Blurs a single image and saves blurred image
	// If an output image already exists, return without doing blur operation
	void blur(String input_file_path, String output_file_path, double blur_sigma) {
		// return if blurred image already exists
		File output_file = new File(output_file_path);
		if (output_file.exists()) {			
			return;
		}	
		
		// open up input image
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(input_file_path);

		// blur image
		imp.getProcessor().blurGaussian(blur_sigma);
		
		// save
		IJ.save(imp,output_file_path);
	}
	
	// Runs watershed on a single image and saves watershed image
	// if an output image already exists, return without doing anything
	void watershed(String input_file_path, String output_file_path) {
		// return if watershed image already exists
		File output_file = new File(output_file_path);
		if (output_file.exists()) {			
			return;
		}	
		
		// open up blurred image
		Opener opener = new Opener();
		ImagePlus imp = opener.openImage(input_file_path);	

		// invert image. This is so that basins form around bright droplets
		imp.getProcessor().invert();

		// run watershed
		int[] dimensions = imp.getDimensions();
		ImagePlus mask = NewImage.createByteImage("mask",dimensions[0],dimensions[1],1,0);
		ImagePlus watershed = Watershed.computeWatershed( imp	, mask, 6 );
		
		// convert watershed to 16-bit tiff (TODO: This could pose problems if there is more than 2^16 watershed basins)
		ImageConverter.setDoScaling(false);
		ImageConverter ic = new ImageConverter(watershed);
		ic.convertToGray16();
		watershed.updateAndDraw();
		
		// save
		IJ.save(watershed,output_file_path);
	}
	
	// Gets proposed segmentations on a single image and saves a serialized object containing proposed segmentations
	void proposed_segmentations(String blur_path, String watershed_path, String output_file_path, int threshold_step_size, int dilation_radius) {
		// return if output already exists
		File output_file = new File(output_file_path);
		if (output_file.exists()) {			
			return;
		}			
		
		// Open blurred and watershed image
		Opener opener = new Opener();
		ImagePlus blurred_image = opener.openImage(blur_path);	
		ImagePlus watershed_image = opener.openImage(watershed_path);	

		// get width/height of watershed/blurred image. Note that both these images should have same dimensions
		// also note that we saved watershed image as uint16
		int number_of_watershed_basins = (int)watershed_image.getProcessor().getMax();
        int width = watershed_image.getProcessor().getWidth();
        int height = watershed_image.getProcessor().getHeight();

        // Create sparse_indices, vector of integer vectors. Each integer vector is a list of pixel indices
    	Vector<CatchmentBasin> catchment_basins = new Vector<CatchmentBasin>(number_of_watershed_basins+1); // add 1 because we also record statistics for 0-basin (i.e., the boundaries)
    	for (int i=0;i<number_of_watershed_basins+1;i++) {
    		CatchmentBasin c = new CatchmentBasin(i,threshold_step_size,dilation_radius,blurred_image.getProcessor(),watershed_image.getProcessor());
    		catchment_basins.addElement(c);
    	}
   	   	
    	// Record the coordinates that compose each watershed basin
        for (int x=0;x<width;x++) {
        	for (int y=0;y<height;y++) {
        		int pixelValue = watershed_image.getProcessor().getPixel(x,y);
        		catchment_basins.get(pixelValue).addPixelCoodinate(x, y);
        	}
        }

        
        
        
        // create empty image with same dimensions as blurred/watershed image. We will display the proposed segmentations in this image
        // This image isn't used for anything, but is nice for showing how the algorithm works
        ImagePlus masks = IJ.createImage("segmentations", width, height, 1, 16);        
        for (int i=1;i<number_of_watershed_basins+1;i++) {
            System.out.println(i+"/"+number_of_watershed_basins);
            catchment_basins.get(i).setData();
            catchment_basins.get(i).draw_segmentation_mask(masks.getProcessor());
        }
        
        IJ.save(masks,"/Users/michaelchiang/Desktop/test.tif");
        
        

      
		
	}
	
}

