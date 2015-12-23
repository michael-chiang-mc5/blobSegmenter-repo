package blobSegmenter;

import ij.process.ImageProcessor;

public class Util {

	public static void normalize_16bit_image_to_8bit_image(ImageProcessor I) {
		int minVal = (int)I.getMin();
		int maxVal = (int)I.getMax();
		int width = I.getWidth();
		int height = I.getHeight();
		for (int x=0;x<width;x++) {
			for (int y=0;y<height;y++) {
				int value = I.getPixel(x, y);
				int normalized_value = 255*(value-minVal) / (maxVal - minVal);
				I.setValue(normalized_value);
				I.drawPixel(x, y);
			}
		}		
	}
	
}
