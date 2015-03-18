package task.usermap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import javax.security.auth.kerberos.KerberosKey;

import basic.Config;
import basic.FileOps;
import basic.StringOps;
import basic.algorithm.Classification;
import basic.algorithm.RandomForest;
import basic.algorithm.WinSVM;
import basic.format.Feature;

public class SVMWrapper extends UserMapTask {
	private int NFeature = 3;
	private LinkedList<Feature> train;
	private double[][] jum;
	Random random;

	private void prepareModels() {
		jum = new double[data.getSizeDouban()][data.getSizeWeibo()];
		LinkedList<String> lines = FileOps.LoadFilebyLine(Config
				.getValue("WorkDir") + "jum.model");
		int i = 0;
		for (String line : lines) {
			String[] t = line.split("\t");
			for (int j = 0; j < t.length; j++)
				jum[i][j] = Double.valueOf(t[j]);
			i++;
		}
	}

	private Feature genFeature(int i, int j, double v) {
		Feature feature = new Feature();
		feature.setSize(NFeature);
		feature.setResult(v);
		int ed = basic.algorithm.StringAlg.EditDistance(

		data.getDouban_username(i), data.getWeibo_username(j));
		feature.setValue(0, ed);
		feature.setValue(
				1,
				(ed + 0.0)
						/ Math.max(data.getWeibo_username(i).length(), data
								.getDouban_username(j).length()));
		feature.setValue(2, jum[i][j]);
		return feature;
	}

	private Classification model;

	private void Train() {
		model = new WinSVM("D:\\cxz\\tools\\svm\\", "-b 1 -t 3","-b 1");
		model.setNFeature(NFeature);
		for (int id : data.getTruth().keySet()) {
			model.addTrain(genFeature(id, data.getTruth().get(id), 1));
			model.addTrain(genFeature(id, random.nextInt(data.getSizeWeibo()),
					0));
		}
		model.train();
	}

	private double getScore(int i, int j) {
		return model.predict(genFeature(i, j, 0));
	}

	private void Predict() {
		final LinkedList<Integer> Q = new LinkedList<Integer>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			if (!data.getTrain().containsKey(i))
				Q.add(i);
		result = new HashMap<Integer, Integer>();
		Thread[] worker = new Thread[Integer
				.valueOf(Config.getValue("#Thread"))];
		for (int i = 0; i < worker.length; i++) {
			worker[i] = new Thread() {
				@Override
				public void run() {
					for (;;) {
						int i;
						synchronized (Q) {
							if (Q.isEmpty())
								return;
							i = Q.removeFirst();
						}
						int res = -1;
						double best = -1;
						LinkedList<Feature> features=new LinkedList<Feature>();
						for (int j=0;j<data.getSizeWeibo();j++)
							features.add(genFeature(i, j, 0));
						ArrayList<Double> score=model.predict(features);
						System.out.println(i);
						for (int j = 0; j < data.getSizeWeibo(); j++)
						{
							double curs = score.get(j);
							if (curs > best) {
								best = curs;
								res = j;
							}
						}
						System.out.println(i + "\t" + res + "\t" + best + "\t"
								+ data.getTruth().get(i) + "\t"
								+ genFeature(i, res, 0));
						synchronized (Q) {
							result.put(i, res);
						}
					}

				}
			};
			worker[i].start();
		}
		for (Thread w : worker)
			try {
				w.join();
			} catch (Exception e) {
			}

	}

	@Override
	public void run() {
		random = new Random();
		prepareModels();
		Train();
//		Predict();
	}
}
