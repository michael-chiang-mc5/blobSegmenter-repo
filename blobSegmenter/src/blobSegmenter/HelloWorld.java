package blobSegmenter;

import ij.IJ;
import ij.ImagePlus;

public class HelloWorld {
	
    public static void main(String[] args) {

        
        PreprocessingBackend preprocessing_backend = new PreprocessingBackend();
        preprocessing_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        preprocessing_backend.create_working_directory_substructure();
        double blur_sigma=1.0;
        int threshold_step_size=10;
        int dilate_radius=1;
        preprocessing_backend.preprocess_batch(blur_sigma,threshold_step_size,dilate_radius);

        AnnotationBackend annotation_backend = new AnnotationBackend();
        annotation_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        annotation_backend.open("94.tif");
        annotation_backend.draw("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/94.tif");

        SvmBackend svm_backend = new SvmBackend();
        svm_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        svm_backend.read_training_data(); // always read and train on all images
        //svm_backend.svmTrain();

        /*

        
        int [] dim = Util.get_image_dimensions("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/input_images/94.tif");
	    int dimx=dim[0];
	    int dimy=dim[1];
	    ImagePlus masks = IJ.createImage("output", dimx, dimy, 1, 16);
        //svm_backend.svmPredict("94.tif",masks.getProcessor());
        //IJ.save(masks,"/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/visualize_final_segmentations/94.tif");
        */

        System.out.println("done");
    }
}
