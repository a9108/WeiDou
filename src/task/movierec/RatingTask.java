package task.movierec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import basic.Config;
import basic.DataOps;

public abstract class RatingTask {
	public static DataSet data;

	public abstract void train();

	public abstract double predict(int uid, int mid);

	private LinkedList<Integer[]> train, test;

	public void genTrain(double ratio) {
		Random random = new Random(0);
		train = new LinkedList<Integer[]>();
		test = new LinkedList<Integer[]>();
		for (int i = 0; i < data.getSizeDouban(); i++) {
			double curR = 0.9;// random.nextDouble()<0.3?.8:ratio;
			for (Integer[] item : data.getDouban_usermovie(i)) {
				Integer[] cur = new Integer[] { i, item[0], item[1] };
				if (random.nextDouble() < curR)
					test.add(cur);
				else
					train.add(cur);
			}
		}
	}

	public LinkedList<Integer[]> getTrain() {
		return train;
	}

	private LinkedList<Integer[]> getTest() {
		return test;
	}

	public void evaluate() {
		ArrayList<Integer> traincnt = new ArrayList<Integer>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			traincnt.add(0);
		LinkedList<Double> trainErr = new LinkedList<Double>();
		for (Integer[] item : train) {
			double curE = predict(item[0], item[1]) - item[2];
			traincnt.set(item[0], traincnt.get(item[0]) + 1);
			trainErr.add(curE);
		}
		System.out.println("RMSE @ Train: " + DataOps.calcRMSE(trainErr));
		LinkedList<Double> testErr = new LinkedList<Double>();
		int[] coldThres = new int[] { 2, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90,
				100, 10000 };
		ArrayList<LinkedList<Double>> coldErr = new ArrayList<LinkedList<Double>>();
		for (int i = 0; i < coldThres.length; i++)
			coldErr.add(new LinkedList<Double>());
		for (Integer[] item : test) {
			double curE = predict(item[0], item[1]) - item[2];
			testErr.add(curE);
			int cursize = traincnt.get(item[0]);
			for (int q = 0; q < coldThres.length; q++)
				if (cursize < coldThres[q]) {
					coldErr.get(q).add(curE);
					break;
				}
		}
		System.out.println("RMSE @ Test: " + DataOps.calcRMSE(testErr));
		for (int i = 0; i < coldThres.length; i++) {
			System.out.println("RMSE @ Cold<" + coldThres[i] + " : "
					+ DataOps.calcRMSE(coldErr.get(i)) + "\t"
					+ coldErr.get(i).size());
		}
	}
}
