package blobSegmenter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Vector;

import ij.process.ImageProcessor;

/*
 * This class implements functionality to:
 *   (1) Find the best local threshold for a proposed droplet segmentation
 */
public class ProposedSegmentation implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public int watershed_index; // index of corresponding watershed basin
	
	// statistics for watershed catchment basin
	private transient Vector<Integer> basin_sparse_x; // sparse x-coordinates of basin encompassing segmentation
	private transient Vector<Integer> basin_sparse_y; // sparse y-coordinates of basin encompassing segmentation
	private transient int basin_min_x;				// minimum x-coordinate of basin
	private transient int basin_min_y;				// minimum y-coordinate of basin
	private transient int basin_max_x;				// maximum x-coordinate of basin
	private transient int basin_max_y;				// maximum y-coordinate of basin	
	private transient int basin_min_blurredPixelVal;  // minimum pixel value of blurred image within basin
	private transient int basin_max_blurredPixelVal;  // maximum pixel value of blurred image within basin
	
	// shared images
	private transient ImageProcessor blurred_image;   // not serializable
	private transient ImageProcessor watershed_image; // not serializable
	
	// segmentation parameters
	private transient int threshold_step_size;
	private transient int dilation_radius;
	private transient int best_threshold;
	private transient Vector<Integer> threshold_curve; // plot responses_curve vs. threshold_curve
	private transient Vector<Integer> response_curve;
	
	// segmentation coordinates
	public Vector<Integer> segmentation_full_x;
	public Vector<Integer> segmentation_full_y;
	public Vector<Integer> segmentation_perimeter_x;
	public Vector<Integer> segmentation_perimeter_y;
	private transient Vector<Integer> segmentation_outerBoundary_x;
	private transient Vector<Integer> segmentation_outerBoundary_y;
	
	// features used to determine whether proposed segmentation is an actual lipid droplet
	public double[] feature_vector;
	public String[] feature_names  = {"blurred_image_prc50",    				// 0  , image level feature
			                          "blurred_image_prc90",    				// 1  , image level feature
			                          "blurred_image_prc_99",  					// 2  , image level feature
			                          "segmentation_size",						// 3  , offset here
			                          "segmentation_meanPixelValue",			// 4
			                          "segmentation_boundaryMeanPixelValue",	// 5
									 };
	private transient int feature_offset = 3;
	
	/*
	 * Constructor:
	 *   index: This is the index of the watershed basin we are going to work on
	 *   watershed: This is a watershed image
	 */
	public ProposedSegmentation(int index, int threshold_step_size, int dilation_radius, ImageProcessor blurred_image, ImageProcessor watershed_image) {
		// set index
		this.watershed_index = index;
		
		// set iterative thresholding segmentation parameters
		this.threshold_step_size = threshold_step_size;
		this.dilation_radius = dilation_radius;
		
		// initialize vectors
		basin_sparse_x = new Vector<Integer>(0);
		basin_sparse_y = new Vector<Integer>(0);
		threshold_curve = new Vector<Integer>(0);
		response_curve = new Vector<Integer>(0);
		segmentation_full_x = new Vector<Integer>(0);
		segmentation_full_y = new Vector<Integer>(0);
		segmentation_perimeter_x = new Vector<Integer>(0);
		segmentation_perimeter_y = new Vector<Integer>(0);
		segmentation_outerBoundary_x = new Vector<Integer>(0);
		segmentation_outerBoundary_y = new Vector<Integer>(0);
		feature_vector = new double[feature_names.length];
		
		// set images (these are shared between different self objects)
		this.blurred_image = blurred_image;
		this.watershed_image = watershed_image;
	}
	
	/*
	 * Add a pixel coordinate to basin coordinate storage.
	 */
	public void addPixelCoodinate(int x, int y) {
		basin_sparse_x.add(x);
		basin_sparse_y.add(y);		
	}
	
	/*
	 * 
	 */
	public void setData() {
		// calculate all necessary basin statistics
		setBasinStatistics();
		
		// find the threshold that best segments proposed lipid droplet
		// This function also records threshold_curve, response_curve
		best_threshold = find_best_threshold();
		
		// record full segmentation coordinates. At the same time, create mask image
		int dimx = basin_max_x - basin_min_x + 1 +2; //add two so that we can include watershed boundary in segmentation boundary
		int dimy = basin_max_y - basin_min_y + 1 +2;
		byte mask[][]= new byte[ dimx ][ dimy ];		
		for (int i=0;i<basin_sparse_x.size();i++){
			int x = basin_sparse_x.get(i);
			int y = basin_sparse_y.get(i);
			if (blurred_image.getPixel(x, y)>=best_threshold) {
				segmentation_full_x.addElement(x);
				segmentation_full_y.addElement(y);
				mask[x-basin_min_x+1][y-basin_min_y+1] = 1; // add one because of padding
			}
		}
		
		// record perimeter segmentation coordinates
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (mask[x][y]>0) { 
					Boolean is_perimeter_pixel = false;
					if (x-1<0 || x+1>=dimx || y-1<0 || y+1>=dimy) {
						is_perimeter_pixel = true;
					} else if (mask[x+1][y+0]==0 || mask[x+0][y+1]==0 || mask[x-1][y+0]==0 || mask[x+0][y-1]==0) {
						is_perimeter_pixel = true;
					}					
					if (is_perimeter_pixel) {
						segmentation_perimeter_x.addElement(basin_min_x+x-1); // subtract one because of padding
						segmentation_perimeter_y.addElement(basin_min_y+y-1);					
					}						
					
				}				
			}
		}
		
		// record outer boundary mask segmentation coordinates
		byte boundary[][]= new byte[ dimx ][ dimy ]; 
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (mask[x][y]>0) { 
					for (int dx=-dilation_radius;dx<=dilation_radius;dx++) {
						for (int dy=-dilation_radius;dy<=dilation_radius;dy++) {
							// bounds checking
							if (x+dx<0 || x+dx>=dimx || y+dy<0 || y+dy>=dimy) {
								continue;
							}
							if (mask[x+dx][y+dy]==0) {
								boundary[x+dx][y+dy]=1;
							}
						}
					}
				}			
			}
		}
		int widthI = blurred_image.getWidth();
		int heightI = blurred_image.getWidth();
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (boundary[x][y]>0) {				
					int x_abs = x+basin_min_x-1; // subtract one because of padding
					int y_abs = y+basin_min_y-1;
					// bounds checking
					if (x_abs>=widthI || y_abs>=heightI) {
						continue;
					}
					if (watershed_image.getPixel(x_abs, y_abs)!=watershed_index && watershed_image.getPixel(x_abs, y_abs)!=0) {
						continue;
					}
					segmentation_outerBoundary_x.addElement(x_abs);
					segmentation_outerBoundary_y.addElement(y_abs);					
				}			
			}
		}		
		

	}
	
	public void draw_segmentation_mask(ImageProcessor output, int value) {
		output.setValue(value);
		for (int i=0;i<segmentation_perimeter_x.size();i++) {
			int x = segmentation_perimeter_x.get(i);
			int y = segmentation_perimeter_y.get(i);
			output.drawPixel(x, y);
		}
	}
	
	// TODO: make add_sparse_x into static method
	// TODO: shape descriptors https://code.google.com/p/jfeaturelib/wiki/FeaturesOverview
	// TODO: sift, surf features
	public void set_image_level_features(int [] blurred_image_features) {
		for (int i=0;i<blurred_image_features.length;i++) {
			feature_vector[i] = blurred_image_features[i];
		}
	}

	public void set_segmentation_level_features() {
		int segmentation_size = basin_sparse_x.size();
		int segmentation_meanValue = mean_pixel_intensity(segmentation_full_x, segmentation_full_y, blurred_image);
		int segmentationBoundary_meanValue = mean_pixel_intensity(segmentation_outerBoundary_x, segmentation_outerBoundary_y, blurred_image);		

		double [] segmentation_level_features = {segmentation_size,segmentation_meanValue,segmentationBoundary_meanValue};
		for (int i=0;i<segmentation_level_features.length;i++) {
			feature_vector[feature_offset+i] = segmentation_level_features[i];
		}
		
	}
	
	private int mean_pixel_intensity(Vector<Integer> x_vector, Vector<Integer> y_vector, ImageProcessor I) {		
		int sum=0;
		for (int i=0;i<x_vector.size();i++) {
			int x = x_vector.get(i);
			int y = y_vector.get(i);
			sum+=I.getPixel(x, y);
		}		
		return sum/x_vector.size();
	}
	
	
	public static int[] get_image_level_features(ImageProcessor I) {
		// store pixel values in int[] array
		int dimx = I.getWidth();
		int dimy = I.getHeight();
		int pixel_values[] = new int[dimx*dimy];	
		int count=0;
		for (int x=0;x<dimx;x++) {
			for (int y=0;y<dimy;y++) {
				pixel_values[count]=I.getPixel(x,y);
				count++;
			}
		}

		// sort int[] array
	   Arrays.sort(pixel_values);

	   // get 50%, 90%, 99% percentile pixel intensities
	   int prc50 = pixel_values[(int) (0.5 * dimx*dimy)];
	   int prc90 = pixel_values[(int) (0.9 * dimx*dimy)];
	   int prc99 = pixel_values[(int) (0.99 * dimx*dimy)];
	   
	   // return
	   int rn[] = {prc50, prc90, prc99};
	   return rn;
	}
	
	
	
	
	/*
	 * Calculate statistics for watershed catchment basin
	 */
	private void setBasinStatistics() {
		// calculate basin_min_x, basin_max_x
		basin_min_x = Integer.MAX_VALUE;
		basin_max_x = Integer.MIN_VALUE;
		basin_min_y = Integer.MAX_VALUE;
		basin_max_y = Integer.MIN_VALUE;
		basin_min_blurredPixelVal = Integer.MAX_VALUE;
		basin_max_blurredPixelVal = Integer.MIN_VALUE;		
		for (int i=0;i<basin_sparse_x.size();i++) {
			int x = basin_sparse_x.get(i);
			int y = basin_sparse_y.get(i);
			if (x<basin_min_x) {
				basin_min_x = basin_sparse_x.get(i);				
			}
			if (x>basin_max_x) {
				basin_max_x = basin_sparse_x.get(i);				
			}
			if (y<basin_min_y) {
				basin_min_y = basin_sparse_y.get(i);				
			}
			if (y>basin_max_y) {
				basin_max_y = basin_sparse_y.get(i);				
			}
			if (blurred_image.getPixel(x,y)<basin_min_blurredPixelVal) {
				basin_min_blurredPixelVal = blurred_image.getPixel(x, y);
			}
			if (blurred_image.getPixel(x,y)>basin_max_blurredPixelVal) {
				basin_max_blurredPixelVal = blurred_image.getPixel(x, y);
			}			
		}		
	}
	
	private int find_best_threshold() {
		int best_response = Integer.MIN_VALUE;
		int best_threshold = -1;
		for (int threshold=basin_max_blurredPixelVal; threshold>basin_min_blurredPixelVal-threshold_step_size;threshold-=threshold_step_size) {
			int response = calculate_response(threshold);
			if (response > best_response) {
				best_response=response;
				best_threshold=threshold;
			}
			threshold_curve.add(threshold);
			response_curve.add(response);
		}		
		return best_threshold;
	}	
	private int calculate_response(int threshold) {
		// create matrix store mask.
		int dimx = basin_max_x - basin_min_x + 1 + 2; // add 2 so that we are guarenteed to look at 0 boundary 
		int dimy = basin_max_y - basin_min_y + 1 + 2;
		byte mask[][]= new byte[ dimx ][ dimy ];
		
		// threshold blurred pixels in basin, record average pixel intensity in mask, and record threshold mask
		int pixelSum = 0;
		int numPixels = 0;
		for (int i=0;i<basin_sparse_x.size();i++){
			int x = basin_sparse_x.get(i);
			int y = basin_sparse_y.get(i);
			if (blurred_image.getPixel(x, y)>=threshold) {
				pixelSum += blurred_image.getPixel(x, y);				
				numPixels++;
				mask[x-basin_min_x+1][y-basin_min_y+1] = 1;
			}		
		}
		int averagePixelValue_innerMask = pixelSum / numPixels;
        
		// calculate boundary mask
		byte boundary[][]= new byte[ dimx ][ dimy ]; 
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (mask[x][y]>0) { 
					for (int dx=-dilation_radius;dx<=dilation_radius;dx++) {
						for (int dy=-dilation_radius;dy<=dilation_radius;dy++) {
							// bounds checking
							if (x+dx<0 || x+dx>=dimx || y+dy<0 || y+dy>=dimy) {
								continue;
							}
							if (mask[x+dx][y+dy]==0) {
								boundary[x+dx][y+dy]=1;
							}
						}
					}
				}			
			}
		}
		
		// calculate average pixel intensity of blurred image in boundary mask
		int pixelSum_outer = 0;
		int numPixels_outer = 0;
		int widthI = blurred_image.getWidth();
		int heightI = blurred_image.getHeight();
		for (int x=0; x<dimx; x++) {
			for (int y=0; y<dimy; y++) {
				if (boundary[x][y]>0) {
					
					int x_abs = x+basin_min_x-1; // minus 1 because we padded by two so we can look at 0 pixels
					int y_abs = y+basin_min_y-1;

					// bounds checking
					if (x_abs>=widthI || y_abs>=heightI) {
						continue;
					}
					if (watershed_image.getPixel(x_abs, y_abs)!=watershed_index && watershed_image.getPixel(x_abs, y_abs)!=0) {
						continue;
					}

					pixelSum_outer += blurred_image.getPixel(x_abs, y_abs);
					numPixels_outer++;
				}			
			}
		}
		
		
		int averagePixelValue_outerMask = pixelSum_outer / numPixels_outer;	
		
		// return
		return averagePixelValue_innerMask - averagePixelValue_outerMask;
	}


	
}
