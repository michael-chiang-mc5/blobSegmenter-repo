package ldSegmenterPlugin;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import ij.ImagePlus;
import libsvm.svm;
import libsvm.svm_parameter;

public class HelloWorld {
	
    public static void main(String[] args) {

    	
        PreprocessingBackend preprocessing_backend = new PreprocessingBackend();
        preprocessing_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking");        
        preprocessing_backend.create_working_directory_substructure();
        double blur_sigma=1.0;
        int threshold_step_size=10;
        int dilate_radius=1;
        preprocessing_backend.preprocess_batch(blur_sigma,threshold_step_size,dilate_radius);

        // get positive annotations by reading tab delimited text file
        Scanner sc;
    	Vector<Integer> x_positive = new Vector<Integer>(0);
    	Vector<Integer> y_positive = new Vector<Integer>(0);    	
		try {
			sc = new Scanner(new FileReader("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking_annotations/positive_annotations.txt"));
	        while (sc.hasNextLine()){
	        	String line = sc.next();
	        	String [] vals = line.split(",");
	        	x_positive.addElement(Integer.parseInt(vals[0]));
	        	y_positive.addElement(Integer.parseInt(vals[1]));
	        }        			
		} catch (FileNotFoundException e) {
		} catch (NoSuchElementException e) {
		}

        // get positive annotations by reading tab delimited text file
        Scanner sc2;
    	Vector<Integer> x_negative= new Vector<Integer>(0);
    	Vector<Integer> y_negative = new Vector<Integer>(0);    	
		try {
			sc2 = new Scanner(new FileReader("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking_annotations/negative_annotations.txt"));
	        while (sc2.hasNextLine()){
	        	String line = sc2.next();
	        	String [] vals = line.split(",");
	        	x_negative.addElement(Integer.parseInt(vals[0]));
	        	y_negative.addElement(Integer.parseInt(vals[1]));
	        }        			
		} catch (FileNotFoundException e) {
		} catch (NoSuchElementException e) {
		}
		
		//System.out.println("num_positive="+x_positive.size()+", num_negative="+x_negative.size());
		
		// create ground truth vector for comparison
        AnnotationBackend annotation_backend = new AnnotationBackend();
        annotation_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking");
        annotation_backend.open("94.tif");
	    annotation_backend.clearTrainingData();
        double [] ground_truth = new double[annotation_backend.proposed_segmentations.length];
        for (int i=0;i<x_positive.size();i++) {
    		int watershed_index = annotation_backend.watershed_image.getPixel(x_positive.get(i), y_positive.get(i));
    		ground_truth[watershed_index]=1.0;
        }


               
		
		int num_runs = 20;
		int max_num_labels=30;
		int num_negative_samples_multiplier=4; // 4 or 5 seems to be the best number to balancce precision, recall
		
		double [] mean_precision_storage = new double[max_num_labels];
		double [] mean_recall_storage = new double[max_num_labels];
		double [] sd_precision_storage = new double[max_num_labels];
		double [] sd_recall_storage = new double[max_num_labels];
		
		int [] num_training_samples_iter = new int[max_num_labels];
		for (int i=0;i<num_training_samples_iter.length;i++) {
			num_training_samples_iter[i] = (i)*1+1;
		}
		
		for (int k=0;k<num_training_samples_iter.length;k++) { // loop over number of training samples
			int num_training_samples = num_training_samples_iter[k];
	        //System.out.println("num_training_samples="+num_training_samples);

			
			double [] precision_storage = new double[num_runs];
			double [] recall_storage = new double[num_runs];
			
			for (int i=0;i<num_runs;i++) { // loop over runs for confidence intervals

				// randomly permute negative/positive annotations
			    final ArrayList<Integer> positive_idx = new ArrayList<Integer>(x_positive.size()); 
			    for (int j = 0; j < x_positive.size(); j++) {
			    	positive_idx.add(j);
			    }
			    Collections.shuffle(positive_idx);
			    final ArrayList<Integer> negative_idx = new ArrayList<Integer>(x_negative.size()); 
			    for (int j = 0; j < x_negative.size(); j++) {
			    	negative_idx.add(j);
			    }
			    Collections.shuffle(negative_idx);

			    // write out annotations
		        for (int j=0;j<num_training_samples;j++) {
			    	int idx_p = positive_idx.get(j);
			    	annotation_backend.addTrainingData(x_positive.get(idx_p), y_positive.get(idx_p), 1);
			    	int jp1 = j+1;
				    //System.out.println("j="+jp1+", "+"training data sz = "+annotation_backend.training_data.size());

			    	if (jp1!=annotation_backend.training_data.size()) {
			    		//System.out.println(x_positive.get(idx_p)+", "+y_positive.get(idx_p));
			    	}
		        }
		        for (int j=0;j<num_training_samples*num_negative_samples_multiplier;j++) {
			    	int idx_n = negative_idx.get(j);
					annotation_backend.addTrainingData(x_negative.get(idx_n), y_negative.get(idx_n), 0);
			    }
			    annotation_backend.serializeTrainingData();
			    //System.out.println("training data sz = "+annotation_backend.training_data.size());
			    annotation_backend.clearTrainingData();
			    //System.out.println("training data sz = "+annotation_backend.training_data.size());
			    //System.out.println("positive = "+num_training_samples);
			    //System.out.println("negative = "+num_training_samples*num_negative_samples_multiplier);

			    // do svm classification
			    svm.svm_set_print_string_function(new libsvm.svm_print_interface(){
			        @Override public void print(String s) {} // Disables svm output
			    });
		        SvmBackend svm_backend = new SvmBackend();
		        svm_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking");
		        svm_backend.set_svm_parameters(1, 0.5, 0.5, 100, .001);
		        svm_backend.read_training_data(); // always read and train on all images
		        svm_backend.svmTrain();
		        svm_backend.svmPredict_batch();
		        
		        double [] klass = null;
		        double[] labels = Util.deserialize("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking/svm_labels/94.tif", klass);
		        
	        
	    		/*
		        ProposedSegmentation[] klass = null;
	    		ProposedSegmentation[] proposed_segmentations = Util.deserialize("/Users/michaelchiang/Desktop/projects/LDSegmenter-repo/benchmarking/proposed_segmentations/94.tif", klass);
	    		double[] labels = svm_backend.svmPredict(proposed_segmentations);			    
		        */
		        
		        //double [] labels = new double[annotation_backend.proposed_segmentations.length];
		        labels[0] = 0;
		        for (int j=1;j<labels.length;j++) {
		        	if (annotation_backend.proposed_segmentations[j].omit) {
		        		labels[j]=0;
		        	}
		        }
		        
		        int count_positive=0;
		        for (int j=1;j<labels.length;j++) {
		        	if (labels[j]==1) {
		        		count_positive++;
		        	}
		        }
		        //System.out.println("count_positive="+count_positive+"/"+labels.length);
		        
		        // overwrite classification with manual annotations
		        
		        
		        
		        // calculate precision, recall
		        double tp=0;
		        double tn=0;
		        double fp=0;
		        double fn=0;
		        for (int j=1;j<labels.length;j++) {
		        	if (labels[j]==1 && ground_truth[j]==1) {
		        		tp++;
		        	} else if (labels[j]==1 && ground_truth[j]==0) {
						fp++;
					} else if (labels[j]==0 && ground_truth[j]==0) {
						tn++;
					} else if (labels[j]==0 && ground_truth[j]==1) {
						fn++;
					} else {
						System.out.println("error");
					}
		        }		        
		        double precision = tp/(tp+fp);
		        double recall = tp/(tp+fn);
		        precision_storage[i] = precision;
		        recall_storage[i] = recall;
		        
		        //System.out.println("tp="+tp);
		        //System.out.println("fp="+fp);
		        //System.out.println("fn="+fn);
		        //System.out.println("tn="+tn);
			}
			
			
			// calculate mean precision, recall
			double mean_precision = 0;
			double mean_recall = 0;
			for (int i = 0; i < precision_storage.length; i++) {
				mean_precision += precision_storage[i];
				mean_recall += recall_storage[i];
			}
			mean_precision = mean_precision/precision_storage.length;
			mean_recall = mean_recall/recall_storage.length;
			mean_precision_storage[k] = mean_precision;
			mean_recall_storage[k] = mean_recall;
			
			// calculate std precision, recall
			double sd_precision = 0;
			double sd_recall = 0;
			for (int i = 0; i < precision_storage.length; i++) {
				sd_precision += (precision_storage[i] - mean_precision)*(precision_storage[i] - mean_precision) / precision_storage.length;
				sd_recall += (recall_storage[i] - mean_recall)*(recall_storage[i] - mean_recall) / recall_storage.length;
			}
			sd_precision = Math.sqrt(sd_precision);
			sd_recall = Math.sqrt(sd_recall);
			sd_precision_storage[k] = sd_precision;
			sd_recall_storage[k] = sd_precision;
			
			
			// print
			int kp1 = k+1;
			System.out.print("precision(:,"+kp1+")=[");
			for (int i = 0; i < precision_storage.length; i++) {
				System.out.print(precision_storage[i] + " ");
			}
			System.out.print("];\n");
			System.out.print("recall(:,"+kp1+")=[");
			for (int i = 0; i < recall_storage.length; i++) {
				System.out.print(recall_storage[i] + " ");
			}
			System.out.print("];\n");
			
		}
	
 
		/*
        System.out.println("done");
        for (int i=0;i<mean_precision_storage.length;i++) {
        	System.out.println("precision = "+mean_precision_storage[i]+" +- "+sd_precision_storage[i]);
        	System.out.println("recall = "+mean_recall_storage[i]+" +- "+sd_recall_storage[i]);
        }
        System.out.println("*** mean precision ***");
        for (int i=0;i<mean_precision_storage.length;i++) {
        	System.out.println(mean_precision_storage[i]);
        }
        System.out.println("*** sd precision ***");
        for (int i=0;i<sd_precision_storage.length;i++) {
        	System.out.println(sd_precision_storage[i]);
        }
        System.out.println("*** mean recall ***");
        for (int i=0;i<mean_recall_storage.length;i++) {
        	System.out.println(mean_recall_storage[i]);
        }
        System.out.println("*** sd recall ***");
        for (int i=0;i<sd_recall_storage.length;i++) {
        	System.out.println(sd_recall_storage[i]);
        }
        */
        
        /*      
        AnnotationBackend annotation_backend = new AnnotationBackend();
        annotation_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        annotation_backend.open("94.tif");
        annotation_backend.draw("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory/94.tif");

        SvmBackend svm_backend = new SvmBackend();
        svm_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");        
        svm_backend.read_training_data(); // always read and train on all images
		*/
    	
        /*
        SvmBackend svm_backend = new SvmBackend();
        svm_backend.set_working_directory("/Users/michaelchiang/Desktop/projects/blobSegmenter-repo/example_working_directory");
        svm_backend.set_svm_parameters(1, 0.5, 0.5, 100, 0.001);
        svm_backend.read_training_data(); // always read and train on all images
        svm_backend.svmTrain();
        svm_backend.svmPredict_batch();		    	
		*/
        
        
        
    }
}
