package blobSegmenter;

import java.io.File;
import java.util.LinkedList;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import libsvm.*;

public class SvmBackend {
	private String working_directory;
	
	LinkedList<Integer> labels;
	LinkedList<double[]> feature_vectors;

	svm_model model;
	svm_parameter parameters;
	
	public SvmBackend() {
		parameters = new svm_parameter();
		parameters.probability = 1;
		parameters.gamma = 0.5;
		parameters.nu = 0.5;
		parameters.C = 100;
		parameters.svm_type = svm_parameter.C_SVC;
		parameters.kernel_type = svm_parameter.LINEAR;       
		parameters.cache_size = 20000;
		parameters.eps = 0.001;   
	}
	
	public void set_working_directory(String working_directory) {
		this.working_directory = working_directory;
	}
	

	// TODO: to filter by extension:
	// http://stackoverflow.com/questions/19190584/how-to-loop-through-all-the-files-in-a-folder-if-the-names-of-the-files-are-unk
	public void read_training_data() {
		labels = new LinkedList<Integer>(); // 0 for negative annotation, 1 for positive annotation TODO: change to -1, 1
		feature_vectors = new LinkedList<double[]>();
		File folder = new File(working_directory + "/training_data/");		
		for(File child : folder.listFiles()) {
			System.out.println(child.getAbsolutePath());
			TrainingData klass = null;
			TrainingData training_data = Util.deserialize(child.getAbsolutePath(), klass);
			for (int i=0;i<training_data.size();i++) {
				labels.add(training_data.getLabel(i));
				feature_vectors.add(training_data.getFeatureVector(i));
				if (i==0)
					System.out.println("1="+training_data.getFeatureVector(i)[5]);
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
		int featureCount = feature_vectors.getFirst().length;
		
		prob.y = new double[recordCount]; // label
		prob.l = recordCount;			  // size of training set
		prob.x = new svm_node[recordCount][featureCount];
		for (int i=0;i<recordCount;i++) {
			double[] features = feature_vectors.get(i);
			prob.x[i] = new svm_node[features.length];
			for (int j=0; j<features.length; j++) {
				svm_node node = new svm_node();
				node.index = j;
				node.value = features[j];
				prob.x[i][j] = node;
			}
			prob.y[i] = labels.get(i);
		}
        model = svm.svm_train(prob, parameters);
	}	
	
	
	
	

	public double[] svmPredict(ProposedSegmentation [] proposed_segmentations) {
		
		// read test data
		/*
	    ProposedSegmentation[] klass = null;
	    ProposedSegmentation[] proposed_segmentations = Util.deserialize(working_directory + "/proposed_segmentations/"+file_name, klass);
		*/
		
		// Sanity check
		if (proposed_segmentations.length==0) {
			System.out.println("Error: test data is empty");
		}
		
		// classify
        double[] yPred = new double[proposed_segmentations.length]; // TODO: can this be turned into int?
	    for(int k = 1; k < proposed_segmentations.length; k++){
	    	double[] fVector = proposed_segmentations[k].feature_vector;
	        svm_node[] nodes = new svm_node[fVector.length];
	        for (int i = 0; i < fVector.length; i++) {
	            svm_node node = new svm_node();
	            node.index = i;
	            node.value = fVector[i];
	            nodes[i] = node;
	        }
	        int totalClasses = 2;       
	        int[] labels = new int[totalClasses];
	        svm.svm_get_labels(model,labels);

	        double[] prob_estimates = new double[totalClasses];
	        yPred[k] = svm.svm_predict_probability(model, nodes, prob_estimates);
	    }
	    
	    // display segmentation classifications
	    /*
        for (int i=1;i<yPred.length;i++) {
        	if (yPred[i]==1) {
        		proposed_segmentations[i].draw_segmentation_mask(masks,1);		
        	}
        }

        // overwrite with annotations // TODO: make this optional for benchmarking
		TrainingData klass2 = null;
		TrainingData training_data = Util.deserialize(working_directory+"/training_data/"+file_name, klass2);
		for (int i=0;i<training_data.size();i++) {
			int label = training_data.getLabel(i);
			int watershed_index = training_data.getWatershedIndex(i);
			if (label==1) {
        		proposed_segmentations[watershed_index].draw_segmentation_mask(masks,1);
			} else if (label==0) { // TODO: change this to -1
        		proposed_segmentations[watershed_index].draw_segmentation_mask(masks,0);				
			} else {
				System.out.println("Error: label is " + label);
			}
		}
		*/
	    
	    return yPred;
    
     } 	
	
	
	void set_svm_parameters(int probability, double gamma, double nu, double C, double eps) {
		parameters.probability = probability;
		parameters.gamma = gamma;
		parameters.nu = nu;
		parameters.C = C;
		parameters.eps = eps;   
	}
	
	
	
}
