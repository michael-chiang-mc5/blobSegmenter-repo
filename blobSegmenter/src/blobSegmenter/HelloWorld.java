package blobSegmenter;

import java.io.File;

public class HelloWorld {
	
    public static void main(String[] args) {

        
        DirectoryLevelOperations d = new DirectoryLevelOperations();
        d.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");
        d.preprocess_batch(1.0);

        //d.blur_batch(1.0);
		

        
        //d.watershed("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/blurred_images/92.tif", "/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/watershed_images/92.tif");
        
        System.out.println("done");

        
        
    }
}
