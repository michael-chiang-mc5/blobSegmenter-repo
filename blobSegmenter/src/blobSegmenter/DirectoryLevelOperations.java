package blobSegmenter;

import java.awt.Image;
import java.io.File;
import java.util.Vector;

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
		File localThreshold_folder = new File(working_directory + "/localThreshold_images/");
		if (!localThreshold_folder.exists()) {
			localThreshold_folder.mkdirs();
		}		
	}
	
	String[] get_list_of_image_files(String subdirectory) {
		File folder = new File(working_directory + "/" + subdirectory + "/");	
		String[] listOfFiles = folder.list();		
		return listOfFiles;
	}

	
	void preprocess_batch(double blur_sigma) {
		String[] input_images = get_list_of_image_files("input_images");
        for (int i=0;i<input_images.length;i++) {
            blur(working_directory+"/input_images/"+input_images[i],working_directory+"/blurred_images/"+input_images[i],blur_sigma);
            watershed(working_directory+"/blurred_images/"+input_images[i],working_directory+"/watershed_images/"+input_images[i]);
            localThresholding(working_directory+"/blurred_images/"+input_images[i],working_directory+"/watershed_images/"+input_images[i], working_directory+"/localThreshold_images/"+input_images[i]);
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
		ImagePlus resultStack = Watershed.computeWatershed( imp	, mask, 6 );
		
		// convert to 16-bit tiff (maybe this is unnecessary? This could pose problems if there is more than 2^16 watershed basins)
		ImageConverter ic = new ImageConverter(resultStack);
		ic.setDoScaling(false);
		ic.convertToGray16();
		resultStack.updateAndDraw();
		
		// save
		IJ.save(resultStack,output_file_path);
	}
	
	int sub2ind(int x, int y, int dimx, int dimy) {
		return y*dimx+x;
	}
	
	void localThresholding(String blur_path, String watershed_path, String output_path) {
		Opener opener = new Opener();
		ImagePlus blurred_image = opener.openImage(blur_path);	
		ImagePlus watershed_image = opener.openImage(watershed_path);	

		ImageProcessor wi = watershed_image.getProcessor();
		double min_val=wi.getMin();
		double max_val=wi.getMax();
        int width = wi.getWidth();
        int height = wi.getHeight();

        // Create sparse_indices, vector of integer vectors. Each integer vector is a list of pixel indices
    	Vector<CatchmentBasin> catchment_basins = new Vector<CatchmentBasin>((int)max_val+1);
    	for (int i=0;i<(int)max_val+1;i++) {
    		CatchmentBasin c = new CatchmentBasin(i);
    		catchment_basins.addElement(c);
    	}
   	   	
    	// sparse_indices[i] = [idx1,idx2,...,idxn] means that watershed basin i is composed of pixel indices
    	// idx1,idx2,etc.
        for (int x=0;x<width;x++) {
        	for (int y=0;y<height;y++) {
        		int pixelValue = wi.getPixel(x,y);
        		catchment_basins.get(pixelValue).addPixelCoodinate(x, y);
        	}
        }

        // threshold
        //catchment_basins.get(1).threshold(blurred_image.getProcessor(), wi, 1000, 1);
        catchment_basins.get(1).find_best_threshold(blurred_image.getProcessor(), wi, 10, 1);

        System.out.println("done");
        

        
        for (float i=1;i<=max_val;i++) {
        	
        }
		
	}
	
}

