package task.movierec;

import java.util.Random;

import basic.algorithm.MatrixFactorization;

public class MFWrapper extends RankingTask {
	private MatrixFactorization model;
	private Random random;
	@Override
	public void train() {
		random=new Random(0);
		model = new MatrixFactorization(data.getSizeDouban(),
				data.getSizeMovie(), 10, 0, 1e-1, 1e-4,MatrixFactorization.SIGMOID);
		
		for (int i = 0; i < data.getSizeDouban(); i++)
			for (int item : getTrain().get(i)) {
				model.addTrain(i, item, 1);
				model.addTrain(i, random.nextInt(data.getSizeMovie()), 0);
			}
		for (;;) {
			model.train(10, true);
			evaluate();
		}
	}

	@Override
	public double predict(int uid, int mid) {
		return model.predict(uid, mid);
	}

}
