package blobSegmenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;

public class Util {

	public static void normalize_16bit_image_to_8bit_image(ImageProcessor I) {
		int minVal = (int) I.getMin();
		int maxVal = (int) I.getMax();
		int width = I.getWidth();
		int height = I.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int value = I.getPixel(x, y);
				int normalized_value = 255 * (value - minVal) / (maxVal - minVal);
				I.setValue(normalized_value);
				I.drawPixel(x, y);
			}
		}
	}

	public static void serialize_object(Object obj, String output_file_path) {
		try {
			FileOutputStream fileOut = new FileOutputStream(output_file_path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String file_path, T obj) {
		T rn = null;
		try {
			FileInputStream fileIn = new FileInputStream(file_path);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			rn = (T) in.readObject();
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
	
	public static int[] get_image_dimensions(String file_path) {
		Opener opener = new Opener();  
		ImagePlus I = opener.openImage(file_path);
		int dimx = I.getProcessor().getWidth();
		int dimy = I.getProcessor().getHeight();
		int [] rn = new int[]{dimx,dimy};
		return rn;
	}
	
}
