package analze;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import javax.security.auth.kerberos.KerberosKey;

import basic.Config;
import basic.DataOps;
import basic.FileOps;
import basic.format.Pair;

public class UserSim {

	private static HashMap<String, Integer[]> weibo_bow;
	private static HashMap<String, LinkedList<Integer>> movies;

	private static double getWeiboSim(String A, String B) {
		Integer[] ta = weibo_bow.get(A), tb = weibo_bow.get(B);
		int sa = 0, sb = 0, sum = 0;
		for (int i = 0; i < ta.length; i++) {
			sum += Math.min(ta[i], tb[i]);
			sa += ta[i];
			sb += tb[i];
		}
		return sum / (sa + sb + 0.0);
	}

	private static double getDoubanSim(String A, String B) {
		HashSet<Integer> sa = new HashSet<Integer>(movies.get(A));
		HashSet<Integer> sb = new HashSet<Integer>(movies.get(B));
		int sum = sa.size() + sb.size();
		sa.retainAll(sb);
		int inter = sa.size();
		int union = sum - inter;
		return inter / (union + 0.0);
	}

	public static void main(String[] args) {
		Config.load("config.txt");

		HashMap<String, StringBuilder> weibos = new HashMap<String, StringBuilder>();
		movies = new HashMap<String, LinkedList<Integer>>();

		for (String s : FileOps.LoadFilebyLine(Config.getValue("SelectDir")
				+ "weibo_weibo")) {
			int pos = s.indexOf(":");
			if (pos >= s.length())
				continue;
			String wid = s.substring(0, pos);
			String content = s.substring(pos + 1);
			if (!weibos.containsKey(wid))
				weibos.put(wid, new StringBuilder());
			weibos.get(wid).append(content);
		}

		for (String s : FileOps.LoadFilebyLine(Config.getValue("SelectDir")
				+ "douban_userwatched")) {
			String[] sep = s.split("\t");
			if (sep.length != 3)
				continue;
			String did = sep[0];
			int mid = Integer.valueOf(sep[1]);
			if (!movies.containsKey(did))
				movies.put(did, new LinkedList<Integer>());
			movies.get(did).add(mid);
		}

		HashMap<String, Integer> words = new HashMap<String, Integer>();
		for (StringBuilder s : weibos.values())
			for (String word : s.toString().split("\t"))
				if (words.containsKey(word))
					words.put(word, words.get(word) + 1);
				else
					words.put(word, 1);

		LinkedList<Pair<String, Integer>> topwords = DataOps.selectTopN(words,
				new Comparator<Pair<String, Integer>>() {
					@Override
					public int compare(Pair<String, Integer> o1,
							Pair<String, Integer> o2) {
						return -o1.getSecond().compareTo(o2.getSecond());
					}
				}, 2000);

		HashMap<String, Integer> wordid = new HashMap<String, Integer>();
		for (Pair<String, Integer> word : topwords)
			if (word.getSecond() < 50000)
				wordid.put(word.getFirst(), wordid.size());

		weibo_bow = new HashMap<String, Integer[]>();

		for (String s : weibos.keySet()) {
			String content = weibos.get(s).toString();
			Integer[] bow = new Integer[wordid.size()];
			for (int i = 0; i < bow.length; i++)
				bow[i] = 0;
			for (String word : content.split("\t"))
				if (wordid.containsKey(word))
					bow[wordid.get(word)]++;
			weibo_bow.put(s, bow);
		}

		LinkedList<String> users = FileOps.LoadFilebyLine(Config
				.getValue("SelectDir") + "users");

		LinkedList<String> outdata = new LinkedList<String>();
		Random random = new Random();
		for (int q = 0; q < 1000000; q++) {
			int i = random.nextInt(users.size());
			int j = random.nextInt(users.size());
			String[] nameA = users.get(i).split("\t");
			String[] nameB = users.get(j).split("\t");
			double sw = getWeiboSim(nameA[2], nameB[2]);
			double sd = getDoubanSim(nameA[1], nameB[1]);
			outdata.add(sw + "\t" + sd);
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "usersim", outdata);
	}
}
