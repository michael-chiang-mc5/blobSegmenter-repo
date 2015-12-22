package blobSegmenter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

public class AnnotationBackend {
	private String working_directory;
	
	void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
	}
	
	void open(String name) {		
		// deserialize
		ProposedSegmentation[] proposed_segmentations = deserialize(working_directory + "/proposed_segmentations/" + name);
        System.out.println(proposed_segmentations[1].segmentation_perimeter_x.get(1));

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
