package blobSegmenter;

import java.io.Serializable;
import java.util.LinkedList;

public class TrainingData implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private LinkedList<Integer> labels; // 0 for negative annotation, 1 for positive annotation
	private LinkedList<double[]> feature_vectors; //
	private LinkedList<Integer> watershed_index; // 
	
	public TrainingData() {
		labels = new LinkedList<Integer>();
		feature_vectors = new LinkedList<double[]>();
		watershed_index = new LinkedList<Integer>();
	}
	
	public void add(ProposedSegmentation o, int label) {
		// Don't do anything if already added
		Boolean isPreexisting = false;
		for (int i=0;i<this.watershed_index.size();i++) {
			if (this.watershed_index.get(i)==o.watershed_index) {
				isPreexisting = true;
				break;
			}
		}
		if (isPreexisting) {
			return;
		}
		
		// add data		
		this.labels.add(label);
		this.feature_vectors.add(o.feature_vector);
		this.watershed_index.add(o.watershed_index);		
	}
	
	public void remove(ProposedSegmentation o) {
		for (int i=0;i<this.watershed_index.size();i++) {
			if (this.watershed_index.get(i)==o.watershed_index) {
				labels.remove(i);
				feature_vectors.remove(i);
				watershed_index.remove(i);
				break;
			}
		}
	}
	
	public int size() {
		return watershed_index.size();
	}
	
	public int getWatershedIndex(int i) {
		return watershed_index.get(i);
	}
	
	public int getLabel(int i) {
		return labels.get(i);
	}
	
	public double[] getFeatureVector(int i) {
		return feature_vectors.get(i);
	}
	
	
	
}
