package blobSegmenter;

import java.io.File;

public class HelloWorld {
	
    public static void main(String[] args) {

        
        DirectoryLevelOperations d = new DirectoryLevelOperations();
        d.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");

        d.sanitize_working_directory();
        
        File[] listOfFiles = d.get_list_of_input_image_files();
        d.blur_batch(1.0);
		
		//d.blur("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/input_images/92.tif", "/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/output_images/92.tif",1.0);

        
        d.watershed("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/blurred_images/92.tif", "/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/watershed_images/92.tif");
        
        System.out.println("done");

        
        
    }
}
