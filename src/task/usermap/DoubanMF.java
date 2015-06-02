package task.usermap;

import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import basic.Config;
import basic.DataOps;
import basic.FileOps;
import basic.algorithm.MatrixFactorization;
import basic.format.Pair;

public class DoubanMF extends UserMapTask {
	private int K = 20;
	private Random random = new Random();
	private ArrayList<Pair<Integer, Integer>> testsetT, testsetF;
	private MatrixFactorization mf;

	@Override
	public void run() {
		testsetT = new ArrayList<Pair<Integer, Integer>>();
		testsetF = new ArrayList<Pair<Integer, Integer>>();
		mf = new MatrixFactorization(data.getSizeDouban(), data.getSizeMovie(),
				K, 0, 1e-2, 0,1e-2,MatrixFactorization.SIGMOID);
		for (int i = 0; i < data.getSizeDouban(); i++)
			for (Integer[] mlog : data.getDouban_usermovie(i)) {
				if (random.nextDouble() < 0.8) {
					mf.addTrain(i, mlog[0], 1);
					// mf.addTrain(i, random.nextInt(data.getSizeMovie()), 0);
					for (;;) {
						int q = random.nextInt(data.getSizeMovie());
						if (data.getDouban_usermovie_set(i).contains(q))
							continue;
						mf.addTrain(i, q, 0);
						break;
					}
				} else {
					testsetT.add(new Pair<Integer, Integer>(i, mlog[0]));
					for (;;) {
						int q = random.nextInt(data.getSizeMovie());
						if (data.getDouban_usermovie_set(i).contains(q))
							continue;
						testsetF.add(new Pair<Integer, Integer>(i, q));
						break;
					}
				}
			}
		evaluate();
		for (int i = 0; i < 10; i++) {
			mf.train(10, false);
			evaluate();
			save();
		}
	}

	private void save() {
		LinkedList<String> outdata = new LinkedList<String>();
		for (int i = 0; i < data.getSizeDouban(); i++) {
			double[] u = mf.getEmbedding_User(i);
			StringBuilder sb = new StringBuilder();
			for (double v : u)
				sb.append(v + "\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "douban.mf.user", outdata);
		LinkedList<String> itemdata = new LinkedList<String>();
		for (int i = 0; i < data.getSizeMovie(); i++) {
			double[] u = mf.getEmbedding_Item(i);
			StringBuilder sb = new StringBuilder();
			for (double v : u)
				sb.append(v + "\t");
			itemdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "douban.mf.movie",
				itemdata);
	}

	@Override
	public void evaluate() {
		int hit = 0;
		for (int i = 0; i < testsetF.size(); i++) {
			double vt = mf.predict(testsetT.get(i).getFirst(), testsetT.get(i)
					.getSecond());
			double vf = mf.predict(testsetF.get(i).getFirst(), testsetF.get(i)
					.getSecond());
			if (vt > vf)
				hit++;
		}
		System.out.println("Pairwise Precision = " + hit
				/ (0.0 + testsetF.size()));
	}
}
