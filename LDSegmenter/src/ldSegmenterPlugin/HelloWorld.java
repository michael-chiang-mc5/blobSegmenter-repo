package ldSegmenterPlugin;

import libsvm.svm_parameter;

public class HelloWorld {
	
    public static void main(String[] args) {

        PreprocessingBackend preprocessing_backend = new PreprocessingBackend();
        preprocessing_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/example_working_directory");        
        preprocessing_backend.create_working_directory_substructure();
        double blur_sigma=1.0;
        int threshold_step_size=10;
        int dilate_radius=1;
        preprocessing_backend.preprocess_batch(blur_sigma,threshold_step_size,dilate_radius);

        /*      
        AnnotationBackend annotation_backend = new AnnotationBackend();
        annotation_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        annotation_backend.open("94.tif");
        annotation_backend.draw("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/94.tif");

        SvmBackend svm_backend = new SvmBackend();
        svm_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        svm_backend.read_training_data(); // always read and train on all images
		*/
    	
        /*
        SvmBackend svm_backend = new SvmBackend();
        svm_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");
        svm_backend.set_svm_parameters(1, 0.5, 0.5, 100, 0.001);
        svm_backend.read_training_data(); // always read and train on all images
        svm_backend.svmTrain();
        svm_backend.svmPredict_batch();		    	
		*/
        System.out.println("done");
    }
}
