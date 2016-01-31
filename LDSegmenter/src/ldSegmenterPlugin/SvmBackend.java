package ldSegmenterPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import libsvm.*;

public class SvmBackend {
	private String working_directory;
	
	List<Integer> labels;
	List< List<Double> > feature_vectors;

	svm_model model;
	svm_parameter parameters;
	
	public SvmBackend() {
		parameters = new svm_parameter();
		parameters.probability = 1;
		parameters.gamma = 0.5;
		parameters.nu = 0.5;
		parameters.C = 100;
		parameters.svm_type = svm_parameter.C_SVC;
		//parameters.kernel_type = svm_parameter.RBF;       
		parameters.kernel_type = svm_parameter.LINEAR;
		parameters.cache_size = 20000;
		parameters.eps = 0.001;   
	}
	
	public void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
	}
	

	public void read_training_data() {
		labels = new ArrayList<Integer>(); // 0 for negative annotation, 1 for positive annotation TODO: change to -1, 1
		feature_vectors = new ArrayList< List<Double> >();
		File folder = new File(working_directory + "/training_data/");		
		for(File child : folder.listFiles()) {
		    if (!child.getAbsolutePath().endsWith(".tif")) {
		        continue;
		    }						
			TrainingData klass = null;
			TrainingData training_data = Util.deserialize(child.getAbsolutePath(), klass);
			for (int i=0;i<training_data.size();i++) {
				labels.add(training_data.getLabel(i));
				feature_vectors.add(training_data.getFeatureVector(i));
			}			
		}
		
		// Sanity check
		if (feature_vectors.size()==0) {
			System.out.println("Error: training data is empty");
		}		
	}
	

	// http://stackoverflow.com/questions/10792576/libsvm-java-implementation
	public void svmTrain() {
		svm_problem prob = new svm_problem();
		int recordCount = labels.size();
		int featureCount = feature_vectors.get(0).size();
		
		prob.y = new double[recordCount]; // label
		prob.l = recordCount;			  // size of training set
		prob.x = new svm_node[recordCount][featureCount];
		for (int i=0;i<recordCount;i++) {
			List<Double>features = feature_vectors.get(i);
			prob.x[i] = new svm_node[features.size()];
			for (int j=0; j<features.size(); j++) {
				svm_node node = new svm_node();
				node.index = j;
				node.value = features.get(j);
				prob.x[i][j] = node;
			}
			prob.y[i] = labels.get(i);
		}
        model = svm.svm_train(prob, parameters);
	}	
	
	void drawSegmentations_perimeter(int value, ImageProcessor I, ProposedSegmentation proposed_segmentation) {
		I.setValue(value);
		for (int i=0;i<proposed_segmentation.segmentation_perimeter_x.size();i++) {
			int x_coordinate=proposed_segmentation.segmentation_perimeter_x.get(i);
			int y_coordinate=proposed_segmentation.segmentation_perimeter_y.get(i);
			I.drawPixel(x_coordinate, y_coordinate);				
		}	
	}
	void drawSegmentations_full(int value, ImageProcessor I, ProposedSegmentation proposed_segmentation) {
		I.setValue(value);
		for (int i=0;i<proposed_segmentation.segmentation_full_x.size();i++) {
			int x_coordinate=proposed_segmentation.segmentation_full_x.get(i);
			int y_coordinate=proposed_segmentation.segmentation_full_y.get(i);
			I.drawPixel(x_coordinate, y_coordinate);				
		}		
	}		
	
	public void svmPredict_batch() {
		// iterate over all images
		File folder = new File(working_directory + "/proposed_segmentations/");	
		String[] input_images = folder.list();
        for (int i=0;i<input_images.length;i++) {
        	// check file name
        	if (!input_images[i].endsWith(".tif")) {
        		continue;
        	}
        	
    		// deserialize data for proposed segmentations
    		ProposedSegmentation[] klass = null;
    		ProposedSegmentation[] proposed_segmentations = Util.deserialize(working_directory + "/proposed_segmentations/"+input_images[i], klass);

        	// run svm prediction
    		double[] labels = svmPredict(proposed_segmentations);
    		
    		// create empty images to store full segmentation, perimeter segmentation
    		int[] dim = Util.get_image_dimensions(working_directory + "/input_images/" + input_images[i]);
    		int dimx = dim[0];
    		int dimy = dim[1];
    		ImagePlus full_segmentation = IJ.createImage("full_segmention", dimx, dimy, 1, 16);
    		ImagePlus perimeter_segmentation = IJ.createImage("perimeter_segmention", dimx, dimy, 1, 16);
    		
	        // draw classifications
			for (int j=1;j<labels.length;j++) {			
				int watershed_index = proposed_segmentations[j].watershed_index;
				double label = labels[j];
				if (label==1 && !(proposed_segmentations[j].omit)) {
					drawSegmentations_perimeter(1, perimeter_segmentation.getProcessor(), proposed_segmentations[j]);
					drawSegmentations_full(watershed_index, full_segmentation.getProcessor(), proposed_segmentations[j]);
				}
			}
			
			// load previous segmentations if they exist
			String file_path = working_directory + "/training_data/" + input_images[i];
			TrainingData training_data;
			File file = new File(file_path);
			if (file.exists()) {
				TrainingData klass2 = null;
				training_data = Util.deserialize(file_path, klass2);
				if (training_data == null) {
					training_data = new TrainingData(); // user might save empty data
				}
			} else {
				training_data = new TrainingData(); // empty training data if none exists
			}
						
			// draw previous segmentations (overwriting classifications)
			// TODO: add this back in, removed for debugging benchmarking
			/*
			for (int j=0;j<training_data.size();j++) {
				int watershed_index = training_data.getWatershedIndex(j);
				int label = training_data.getLabel(j);
				if (label==0) { // TODO: change this to -1
					drawSegmentations_perimeter(0, perimeter_segmentation.getProcessor(), proposed_segmentations[watershed_index]);
					drawSegmentations_full(0, full_segmentation.getProcessor(), proposed_segmentations[watershed_index]);
				} else if (label==1) {
					drawSegmentations_perimeter(1, perimeter_segmentation.getProcessor(), proposed_segmentations[watershed_index]);
					drawSegmentations_full(watershed_index, full_segmentation.getProcessor(), proposed_segmentations[watershed_index]);
				}
			}
			*/
			
			// save images
	        IJ.save(perimeter_segmentation,working_directory+"/visualize_final_segmentations/"+input_images[i]);
	        IJ.save(full_segmentation,working_directory+"/final_segmentations/"+input_images[i]);
	        
	        // save labels
	        Util.serialize_object(labels, working_directory+"/svm_labels/"+input_images[i]);
	        
        }
	}
	

	public double[] svmPredict(ProposedSegmentation [] proposed_segmentations) {
		

		// Sanity check
		if (proposed_segmentations.length==0) {
			System.out.println("Error: test data is empty");
		}
		
		// classify
        double[] yPred = new double[proposed_segmentations.length]; // TODO: can this be turned into int?
	    for(int k = 1; k < proposed_segmentations.length; k++){
	    	List<Double> fVector = proposed_segmentations[k].feature_vector;
	        svm_node[] nodes = new svm_node[fVector.size()];
	        for (int i = 0; i < fVector.size(); i++) {
	            svm_node node = new svm_node();
	            node.index = i;
	            node.value = fVector.get(i);
	            nodes[i] = node;
	        }
	        int totalClasses = 2;       
	        int[] labels = new int[totalClasses];
	        svm.svm_get_labels(model,labels);

	        double[] prob_estimates = new double[totalClasses];
	        yPred[k] = svm.svm_predict_probability(model, nodes, prob_estimates);
	    }
	    
	    return yPred;
    
     } 	
	
	
	void set_svm_parameters(int probability, double gamma, double nu, double C, double eps) {
		parameters.probability = probability;
		parameters.gamma = gamma;
		parameters.nu = nu;
		parameters.C = C;
		parameters.eps = eps;
		
		double [] weight = new double[2];
		weight[0] = 1;
		weight[1] = 1;
		int [] weight_label = new int[2];
		weight_label[0] = 0;
		weight_label[1] = 1;

		parameters.nr_weight=2;
		parameters.weight = weight;
		parameters.weight_label=weight_label;
	}
	
	
	
}
