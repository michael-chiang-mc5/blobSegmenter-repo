package blobSegmenter;
import java.util.Vector;

import ij.process.ImageProcessor;


public class CatchmentBasin {
	private Vector<Integer> sparse_x;
	private Vector<Integer> sparse_y;
	private int index;
	private int min_x;
	private int min_y;
	private int max_x;
	private int max_y;
	private int min_pixel_val;
	private int max_pixel_val;
	
	public CatchmentBasin(int index) {
		sparse_x = new Vector<Integer>(0);
		sparse_y = new Vector<Integer>(0);
		this.index = index;		
	}
	
	public void addPixelCoodinate(int x, int y) {
		sparse_x.add(x);
		sparse_y.add(y);		
	}

	public int size() {
		return sparse_x.size();
	}
	
	public void set_minmax_x() {
		min_x = Integer.MAX_VALUE;
		max_x = Integer.MIN_VALUE;
		for (int i=0;i<size();i++) {
			if (sparse_x.get(i)<min_x) {
				min_x = sparse_x.get(i);				
			}
			if (sparse_x.get(i)>max_x) {
				max_x = sparse_x.get(i);				
			}			
		}
	}
	
	public void set_minmax_y() {
		min_y = Integer.MAX_VALUE;
		max_y = Integer.MIN_VALUE;
		for (int i=0;i<size();i++) {
			if (sparse_y.get(i)<min_y) {
				min_y = sparse_y.get(i);				
			}
			if (sparse_y.get(i)>max_y) {
				max_y = sparse_y.get(i);				
			}			
		}
	}

	public void set_minmax_pixel_value(ImageProcessor I) {
		min_pixel_val = Integer.MAX_VALUE;
		max_pixel_val = Integer.MIN_VALUE;
		for (int i=0;i<size();i++) {
			int x = sparse_x.get(i);
			int y = sparse_y.get(i);		
			if (I.getPixel(x, y)<min_pixel_val) {
				min_pixel_val = I.getPixel(x, y);
			}
			if (I.getPixel(x, y)>max_pixel_val) {
				max_pixel_val = I.getPixel(x, y);
			}
		}
	}
	
	public int find_best_threshold(ImageProcessor I, ImageProcessor watershed, int threshold_step, int dilate_radius) {
		set_minmax_pixel_value(I);
		
		int best_response = Integer.MIN_VALUE;
		int best_threshold = -1;
		for (int threshold=max_pixel_val; threshold>min_pixel_val-threshold_step;threshold-=threshold_step) {
			int response = threshold(I,watershed,threshold,dilate_radius);
			if (response > best_response) {
				best_response=response;
				best_threshold=threshold;
			}			
	        //System.out.println(threshold+","+response+";");
		}		
		return best_threshold;
	}
	
	public void draw_segmentation_mask(ImageProcessor I, ImageProcessor watershed, int threshold_step, int dilate_radius, ImageProcessor output) {
		int best_threshold = find_best_threshold(I, watershed, threshold_step, dilate_radius);
		output.setValue(index);
		for (int i=0;i<size();i++){
			int x = sparse_x.get(i);
			int y = sparse_y.get(i);
			if (I.getPixel(x, y)>=best_threshold) {
				output.drawPixel(x, y);
			}		
		}	
	}
	
	public int threshold(ImageProcessor I, ImageProcessor watershed, int threshold, int dilate_radius) {
		
		// create roi to store mask.
		set_minmax_x();
		set_minmax_y();
		int dimx = max_x - min_x + 1;
		int dimy = max_y - min_y + 1;
		byte roi[][]= new byte[ dimx ][ dimy ]; 
		
		// threshold blurred pixels in basin
		// record average pixel intensity in mask
		// also record threshold mask
		int pixelSum = 0;
		int numPixels = 0;
		for (int i=0;i<size();i++){
			int x = sparse_x.get(i);
			int y = sparse_y.get(i);
			if (I.getPixel(x, y)>=threshold) {
				pixelSum += I.getPixel(x, y);				
				numPixels++;
				roi[x-min_x][y-min_y] = 1;
			}		
		}
		int averagePixelValue_innerMask = pixelSum / numPixels;
		/*
        System.out.println("averagePixelValue_innerMask="+averagePixelValue_innerMask);
        System.out.println("pixelSum="+pixelSum);
        System.out.println("numPixels="+numPixels);
		*/

		
        
		// calculate boundary mask
		byte boundary[][]= new byte[ dimx ][ dimy ]; 
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (roi[x][y]>0) { 
					for (int dx=-dilate_radius;dx<=dilate_radius;dx++) {
						for (int dy=-dilate_radius;dy<=dilate_radius;dy++) {
							// bounds checking
							if (x+dx<0 || x+dx>=dimx || y+dy<0 || y+dy>=dimy) {
								continue;
							}
							if (roi[x+dx][y+dy]==0) {
								boundary[x+dx][y+dy]=1;
							}
						}
					}
				}			
			}
		}
		
		/*
	    System.out.print("roi\n");
		for (int i = 0; i < dimx; i++) {
		    for (int j = 0; j < dimy; j++) {
		        System.out.print(boundary[i][j] + " ");
		    }
		    System.out.print("\n");
		}		
	    System.out.print("end boundary\n");		
	    */

		// calculate average pixel intensity in boundary mask
		int pixelSum_outer = 0;
		int numPixels_outer = 0;
		int widthI = I.getWidth();
		int heightI = I.getHeight();
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (boundary[x][y]>0) {
					
					int x_abs = x+min_x;
					int y_abs = y+min_y;

					// bounds checking
					if (x_abs>=widthI || y_abs>=heightI) {
						continue;
					}
					if (watershed.getPixel(x_abs, y_abs)!=index && watershed.getPixel(x_abs, y_abs)!=0) {
						continue;
					}

			        //System.out.println("x="+x_abs+", y="+y_abs+", val="+I.getPixel(x_abs, y_abs));		

					
					pixelSum_outer += I.getPixel(x_abs, y_abs);
					numPixels_outer++;
				}			
			}
		}
		
		
		// edge case
		if (numPixels_outer==0) {
			return Integer.MIN_VALUE;
		}
		
		int averagePixelValue_outerMask = pixelSum_outer / numPixels_outer;	
		/*
        System.out.println("averagePixelValue_outerMask="+averagePixelValue_outerMask);
        System.out.println("pixelSum_outer="+pixelSum_outer);
        System.out.println("numPixels_outer="+numPixels_outer);		
		*/
		
		// return
		return averagePixelValue_innerMask - averagePixelValue_outerMask;
	}
	

	
}
