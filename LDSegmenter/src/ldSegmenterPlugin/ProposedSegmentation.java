package ldSegmenterPlugin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import ij.process.ImageProcessor;

/*
 * This class implements functionality to:
 *   (1) Find the best local threshold for a proposed droplet segmentation
 */
public class ProposedSegmentation implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public int watershed_index; // index of corresponding watershed basin
	public Boolean omit=false;
	
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
	private transient ImageProcessor input_image;     // not serializable
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
	public List<Double> feature_vector = new ArrayList<Double>();
	public String[] feature_names  = {"blurred_image_prc50",    				// 0  , image level feature
									  "blurred_image_prc75",
			                          "blurred_image_prc90",    				// 1  , image level feature
									  "blurred_image_prc95",
			                          "blurred_image_prc99",  					// 2  , image level feature
									  "blurred_image_prc999",
									  "skewness",
									  "kurtosis",

									  "minVal_Inside",
									  "maxVal_Inside",
									  "minVal_Outside",
									  "maxVal_Outside",
									  "minVal_Inside_blur",
									  "maxVal_Inside_blur",
									  "minVal_Outside_blur",
									  "maxVal_Outside_blur",
									  "meanIn_minus_meanOut",
									  "SNR",
									  "CoV_Inside",
									  "CoV_Outside",
									  //"skewness_Inside",// possible NaN
									  //"skewness_Outside",
									  //"kurtosis_Inside", // possible NaN
									  //"kurtosis_Outside",
									  "area",
									  "eccentricity"							
									 };
	private transient int feature_offset = 8;
	
	/*
	 * Constructor:
	 *   index: This is the index of the watershed basin we are going to work on
	 *   watershed: This is a watershed image
	 */
	public ProposedSegmentation(int index, int threshold_step_size, int dilation_radius, ImageProcessor input_image, ImageProcessor blurred_image, ImageProcessor watershed_image) {
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
		
		// set images (these are shared between different self objects)
		this.input_image = input_image;
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
	
	public void omit_if_on_boundary() {
		int dimx = blurred_image.getWidth();
		int dimy = blurred_image.getHeight();
		for (int i=0;i<segmentation_perimeter_x.size();i++) {
			if (segmentation_perimeter_x.get(i)==0 || segmentation_perimeter_x.get(i)==dimx-1 || segmentation_perimeter_y.get(i)==0 || segmentation_perimeter_y.get(i)==dimy-1) {
				omit=true;
				break;
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
	public void set_image_level_features(double [] blurred_image_features) {
		for (int i=0;i<blurred_image_features.length;i++) {
			feature_vector.add(blurred_image_features[i]);
		}
	}

	public void set_segmentation_level_features() {
		// minimum, maximum pixel values in inside/outside segmentation
		/*
		int [] minmax_inner_input = Util.get_min_and_max_pixel_value(segmentation_full_x,segmentation_full_y,input_image);
		int [] minmax_inner_blur  = Util.get_min_and_max_pixel_value(segmentation_full_x,segmentation_full_y,blurred_image);
		int [] minmax_outer_input = Util.get_min_and_max_pixel_value(segmentation_outerBoundary_x,segmentation_outerBoundary_y,input_image);
		int [] minmax_outer_blur  = Util.get_min_and_max_pixel_value(segmentation_outerBoundary_x,segmentation_outerBoundary_y,blurred_image);
		int minVal_Inside = minmax_inner_input[0];
		int maxVal_Inside = minmax_inner_input[1];
		int minVal_Outside = minmax_outer_input[0];
		int maxVal_Outside = minmax_outer_input[1];
		int minVal_Inside_blur = minmax_inner_blur[0];
		int maxVal_Inside_blur = minmax_inner_blur[1];
		int minVal_Outside_blur = minmax_outer_blur[0];
		int maxVal_Outside_blur = minmax_outer_blur[1];
		*/
		
		// median/mean/std value inside/outside segmentation 
		/*
		int medianVal_Inside  = Util.get_median_pixel_intensity(segmentation_full_x, segmentation_full_y, blurred_image);
		int medianVal_Outside = Util.get_median_pixel_intensity(segmentation_outerBoundary_x, segmentation_outerBoundary_y, blurred_image);
		*/
		double meanVal_Inside  = Util.get_mean_pixel_intensity(segmentation_full_x, segmentation_full_y, blurred_image);
		double meanVal_Outside = Util.get_mean_pixel_intensity(segmentation_outerBoundary_x, segmentation_outerBoundary_y, blurred_image);		
		/*
		double variance_Inside   = Util.get_variance_pixel_intensity(segmentation_full_x, segmentation_full_y, blurred_image);
		double variance_Outside  = Util.get_variance_pixel_intensity(segmentation_outerBoundary_x, segmentation_outerBoundary_y, blurred_image);
		*/

		// mean(inside) - mean(outside)
		double meanIn_minus_meanOut = meanVal_Inside - meanVal_Outside;
		
		// mean(inside) / mean(outside)
		double meanIn_divided_meanOut = meanVal_Inside / meanVal_Outside;
		
		// median(inside)/median(outside) , (SNR)
		//int SNR = medianVal_Inside - medianVal_Outside;
		
		// CoV inside/outside
		//double CoV_Inside  = Math.sqrt(variance_Inside)/meanVal_Inside;
		//double CoV_Outside = Math.sqrt(variance_Outside)/meanVal_Outside;

		// skewness/kurtosis of segIn/segOut/fullI
		// Skew = 3(mean - median) / Standard Deviation
		// TODO: skewness, kurtosis to image level statistics
		//double skewness_Inside = 3*(meanVal_Inside-medianVal_Inside)/Math.sqrt(variance_Inside);
		//double skewness_Outside = 3*(meanVal_Outside-medianVal_Outside)/Math.sqrt(variance_Outside);
		//double m4_Inside = Util.get_m4_pixel_intensity(segmentation_full_x, segmentation_full_y, blurred_image);
		//double m2_Inside = variance_Inside;
		//double kurtosis_Inside = m4_Inside/(m2_Inside*m2_Inside) - 3;
		//double m4_Outside = Util.get_m4_pixel_intensity(segmentation_outerBoundary_x, segmentation_outerBoundary_y, blurred_image);
		//double m2_Outside = variance_Outside;
		//double kurtosis_Outside = m4_Outside/(m2_Outside*m2_Outside) - 3;
		
		// shape based features
		int area = basin_sparse_x.size();
		
		// eccentricity, major/minor axis
		/*
		Vector<Integer> asdfx = new Vector<Integer>();
		Vector<Integer> asdfy = new Vector<Integer>();
		asdfx.add(1);
		asdfx.add(1);
		asdfx.add(1);
		asdfx.add(1);
		asdfx.add(1);
		asdfx.add(2);
		asdfx.add(2);
		asdfx.add(2);
		asdfx.add(2);
		asdfx.add(2);
		
		asdfy.add(1);
		asdfy.add(2);
		asdfy.add(3);
		asdfy.add(4);
		asdfy.add(5);
		asdfy.add(1);
		asdfy.add(2);
		asdfy.add(3);
		asdfy.add(4);
		asdfy.add(5);
		
		
		double asdfasdfasd = Util.moment_continuous(asdfx, asdfy, 2, 0);	
		System.out.println("memememmeme");
		*/

		double M00 = Util.moment_continuous(segmentation_full_x, segmentation_full_y, 0, 0); // this is the area
		double M10 = Util.moment_continuous(segmentation_full_x, segmentation_full_y, 1, 0); // M10/M00 is xhat		
		double M01 = Util.moment_continuous(segmentation_full_x, segmentation_full_y, 0, 1); // M01/M00 is yhat
		double M11 = Util.moment_continuous(segmentation_full_x, segmentation_full_y, 1, 1);
		double M20 = Util.moment_continuous(segmentation_full_x, segmentation_full_y, 2, 0);
		double M02 = Util.moment_continuous(segmentation_full_x, segmentation_full_y, 0, 2);

		double xhat = M10/M00;
		double yhat = M01/M00;

		
		double m20 = M20/M00 - xhat*xhat;
		double m02 = M02/M00 - yhat*yhat;
		double m11 = M11/M00 - xhat*yhat;
		
		double lambda1 = (m20+m02)/2 + Math.sqrt(4*m11*m11 + (m20-m02)*(m20-m02))/2;
		double lambda2 = (m20+m02)/2 - Math.sqrt(4*m11*m11 + (m20-m02)*(m20-m02))/2;		
		double eccentricity = Math.sqrt(1-lambda2/lambda1);
		
		
		/*
		System.out.println("xhat="+xhat);
		System.out.println("yhat="+yhat);
		System.out.println("M00="+M00);
		System.out.println("M10="+M10);
		System.out.println("M01="+M01);		
		System.out.println("M20="+M20);
		System.out.println("M02="+M02);		
		System.out.println("lambda1="+lambda1+",lambda2="+lambda2);
		System.out.println("eccentricity="+eccentricity);

		System.out.println("*****");
		*/

		// store in field
		double [] segmentation_level_features = {meanVal_Inside, meanVal_Outside,
												meanIn_minus_meanOut,meanIn_divided_meanOut,
												//skewness_Inside,skewness_Outside,kurtosis_Inside,kurtosis_Outside,
												area,eccentricity
												};
		for (int i=0;i<segmentation_level_features.length;i++) {
			feature_vector.add(segmentation_level_features[i]);
		}
		
		
		
	}
	
	
	
	public static double[] get_image_level_features(ImageProcessor I) {
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
	   //int prc75 = pixel_values[(int) (0.75 * dimx*dimy)];	   
	   int prc90 = pixel_values[(int) (0.9 * dimx*dimy)];
	   //int prc95 = pixel_values[(int) (0.95 * dimx*dimy)];	   	   
	   int prc99 = pixel_values[(int) (0.99 * dimx*dimy)];
	   //int prc999 = pixel_values[(int) (0.999 * dimx*dimy)];

	   // calculate skewness
	   double mean = 0;
	   for (int i=0;i<pixel_values.length;i++) {
		   mean += pixel_values[i];
	   }
	   mean = mean/pixel_values.length;
	   double variance=0;
	   for (int i=0;i<pixel_values.length;i++) {
		   variance += (pixel_values[i]-mean) * (pixel_values[i]-mean);
	   }
	   variance = variance / pixel_values.length;
	   double skewness = 3*(mean-prc50)/Math.sqrt(variance);

	   // calculate kurtosis
	   double m4=0;
	   for (int i=0;i<pixel_values.length;i++) {
		   m4 += (pixel_values[i]-mean) * (pixel_values[i]-mean) * (pixel_values[i]-mean) * (pixel_values[i]-mean);
	   }
	   m4 = m4/pixel_values.length;
	   double kurtosis = m4/(variance*variance) - 3;

	   // return
	   double rn[] = {prc50, prc90, prc99, prc90/prc50, prc99/prc50};
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
