package ldSegmenterPlugin;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Vector;

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
	
	public static int[] get_min_and_max_pixel_value(Vector<Integer> x, Vector<Integer> y, ImageProcessor I) {
		int minVal = Integer.MAX_VALUE;
		int maxVal = Integer.MIN_VALUE;
		for (int i=0;i<x.size();i++) {
			int x_coordinate = x.get(i);
			int y_coordinate = y.get(i);
			int value = I.getPixel(x_coordinate, y_coordinate);
			if (value>maxVal) {
				maxVal = value;
			}
			if (value<minVal) {
				minVal = value;
			}
		}
		return new int[]{minVal,maxVal};		
	}
	
	public static double get_mean_pixel_intensity(Vector<Integer> x_vector, Vector<Integer> y_vector, ImageProcessor I) {		
		double sum=0;
		for (int i=0;i<x_vector.size();i++) {
			int x = x_vector.get(i);
			int y = y_vector.get(i);
			sum+=I.getPixel(x, y);
		}		
		return sum/x_vector.size();
	}
	
	public static int get_median_pixel_intensity(Vector<Integer> x_vector, Vector<Integer> y_vector, ImageProcessor I) {		
		int [] storage = new int[x_vector.size()];
		for (int i=0;i<x_vector.size();i++) {
			int x = x_vector.get(i);
			int y = y_vector.get(i);
			storage[i] = I.getPixel(x, y);
		}		
	   Arrays.sort(storage);
	   return storage[(int) 0.5 * x_vector.size()];
	}
	
	public static double get_variance_pixel_intensity(Vector<Integer> x_vector, Vector<Integer> y_vector, ImageProcessor I) {		
		double mean = get_mean_pixel_intensity(x_vector, y_vector, I);
		int sum=0;
		for (int i=0;i<x_vector.size();i++) {
			int x = x_vector.get(i);
			int y = y_vector.get(i);
			int value = I.getPixel(x, y);
			sum += (value - mean) * (value - mean);
		}
		double sum_double = (double)sum;
		return sum_double/x_vector.size();
	}
	
	public static double get_m4_pixel_intensity(Vector<Integer> x_vector, Vector<Integer> y_vector, ImageProcessor I) {		
		double mean = get_mean_pixel_intensity(x_vector, y_vector, I);
		int sum=0;
		for (int i=0;i<x_vector.size();i++) {
			int x = x_vector.get(i);
			int y = y_vector.get(i);
			int value = I.getPixel(x, y);
			sum += (value - mean) * (value - mean) * (value - mean) * (value - mean);
		}
		double sum_double = (double)sum;
		return sum_double/x_vector.size();
	}
	
	public static double moment(Vector<Integer> x_vector, Vector<Integer> y_vector, int p, int q) {
		double xhat = 0;
		double yhat = 0;
		for (int i=0;i<x_vector.size();i++) {
			xhat += x_vector.get(i);
			yhat += y_vector.get(i);
		}
		xhat = xhat/x_vector.size();
		yhat = yhat/y_vector.size();

		double moment_pq = 0;
		for (int i=0;i<x_vector.size();i++) {
			moment_pq += Math.pow(x_vector.get(i) - xhat, p)*Math.pow(y_vector.get(i), q);
		}

		System.out.println("xhat="+xhat+",yhat="+yhat+",moment="+moment_pq);
		
		return moment_pq;
				
	}
	
	public static double moment_continuous(Vector<Integer> x_vector, Vector<Integer> y_vector, int p, int q) {	
		double M = 0;
		for (int i=0;i<x_vector.size();i++) {
			double x = x_vector.get(i);
			double y = y_vector.get(i);
			M += moment_helper(x,y,p,q);
			//System.out.println("x="+x+",y="+y+",p="+p+",q="+q+",M="+M);
		}
		return M;
	}	
	private static double moment_helper(double x, double y, int p, int q) {
		double x1 = x-0.5;
		double x2 = x+0.5;
		double y1 = y-0.5;
		double y2 = y+0.5;
		if (p==0 & q==0) {
			return 1;
		} else if (p==1 & q==0) {
			return 0.5 * (x2*x2 - x1*x1);
		} else if (p==0 & q==1) {
			return 0.5 * (y2*y2 - y1*y1);
		} else if (p==1 & q==1) {
			return 0.5 * (x2*x2 - x1*x1) * 0.5 * (y2*y2 - y1*y1);
		} else if (p==2 & q==0) {
			//System.out.println("x2="+x2+",x1="+x1+"pow="+Math.pow(x2-x1,3)+",M="+Math.pow(x2-x1,3));
			return 1.0 / 3.0 * ((x2*x2*x2)-(x1*x1*x1));
		} else if (p==0 & q==2) {
			return 1.0 / 3.0 * ((y2*y2*y2)-(y1*y1*y1));		
		} else {
			System.out.println("Error: invalid p,q");
			return 0;
		}
	}
	

	
	
	

	
	
}
