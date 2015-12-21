package blobSegmenter;


public class HelloWorld {
	
    public static void main(String[] args) {

        
        DirectoryLevelOperations d = new DirectoryLevelOperations();
        d.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");
        
        double blur_sigma=1.0;
        int threshold_step_size=10;
        int dilate_radius=1;
        d.preprocess_batch(blur_sigma,threshold_step_size,dilate_radius);

        //d.blur_batch(1.0);
		

        
        //d.watershed("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/blurred_images/92.tif", "/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/watershed_images/92.tif");
        
        System.out.println("done");

        
        
    }
}
