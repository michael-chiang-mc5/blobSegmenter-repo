package blobSegmenter;


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

        System.out.println("done");

        
        
    }
}
