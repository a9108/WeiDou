package task.usermap;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import BasicOps.FileOps;

public class DataSet {
	private LinkedList<Long> weibo_user;
	private LinkedList<String> weibo_username;
	private LinkedList<LinkedList<String>> weibo_weibo;
	private LinkedList<LinkedList<Integer>> weibo_friend;
	private HashMap<Long, Integer> weibo_rid;
	private LinkedList<Long> douban_user;
	private LinkedList<String> douban_username;
	private LinkedList<LinkedList<Integer[]>> douban_usermovie;
	private LinkedList<LinkedList<Integer>> douban_friend;
	private HashMap<Long, Integer> douban_rid;
	private HashMap<Integer,Integer> truth, train;

	public int getSizeDouban(){
		return douban_user.size();
	}
	
	public int getSizeWeibo(){
		return weibo_user.size();
	}
	
	public HashMap<Integer, Integer> getTruth() {
		return truth;
	}

	public HashMap<Integer, Integer> getTrain() {
		return train;
	}

	public String getWeibo_username(int i) {
		return weibo_username.get(i);
	}

	public LinkedList<String> getWeibo_weibo(int i) {
		return weibo_weibo.get(i);
	}

	public String getDouban_username(int i) {
		return douban_username.get(i);
	}

	public LinkedList<Integer[]> getDouban_usermovie(int i) {
		return douban_usermovie.get(i);
	}

	public void load(String dir) {
		// Load Douban
		{
			douban_user = new LinkedList<Long>();
			douban_username = new LinkedList<String>();
			douban_rid = new HashMap<Long, Integer>();
			douban_friend = new LinkedList<LinkedList<Integer>>();
			douban_usermovie = new LinkedList<LinkedList<Integer[]>>();
			HashMap<String, String> namemap = FileOps.LoadDictionSS(dir
					+ "douban_username");
			for (String s : namemap.keySet()) {
				Long uid = Long.valueOf(s);
				douban_rid.put(uid, douban_user.size());
				douban_user.add(uid);
				douban_username.add(namemap.get(s));
				douban_friend.add(new LinkedList<Integer>());
				douban_usermovie.add(new LinkedList<Integer[]>());
			}
			System.out.println("Douban User Loaded");
//			for (String line : FileOps.LoadFilebyLine(dir + "douban_friend")) {
//				String[] sep = line.split("\t");
//				int uida = douban_rid.get(Long.valueOf(sep[0]));
//				int uidb = douban_rid.get(Long.valueOf(sep[1]));
//				douban_friend.get(uida).add(uidb);
//				douban_friend.get(uidb).add(uida);
//			}
//			System.out.println("Douban Friends Loaded");
//			for (String line : FileOps.LoadFilebyLine(dir
//					+ "douban_userwatched")) {
//				String[] sep = line.split("\t");
//				douban_usermovie.get(douban_rid.get(Long.valueOf(sep[0]))).add(
//						new Integer[] { Integer.valueOf(sep[1]),
//								Integer.valueOf(sep[2]) });
//			}
//			System.out.println("Douban Movie Loaded");
		}
		// Load Weibo
		{
			weibo_user = new LinkedList<Long>();
			weibo_username = new LinkedList<String>();
			weibo_rid = new HashMap<Long, Integer>();
			weibo_friend = new LinkedList<LinkedList<Integer>>();
			weibo_weibo = new LinkedList<LinkedList<String>>();
			HashMap<String, String> namemap = FileOps.LoadDictionSS(dir
					+ "weibo_username");
			for (String s : namemap.keySet()) {
				Long uid = Long.valueOf(s);
				weibo_rid.put(uid, weibo_user.size());
				weibo_user.add(uid);
				weibo_username.add(namemap.get(s));
				weibo_friend.add(new LinkedList<Integer>());
				weibo_weibo.add(new LinkedList<String>());
			}
			System.out.println("Weibo User Loaded");
//			for (String line : FileOps.LoadFilebyLine(dir + "weibo_friend")) {
//				String[] sep = line.split("\t");
//				int uida = weibo_rid.get(Long.valueOf(sep[0]));
//				int uidb = weibo_rid.get(Long.valueOf(sep[1]));
//				weibo_friend.get(uida).add(uidb);
//				weibo_friend.get(uidb).add(uida);
//			}
//			System.out.println("Weibo Friends Loaded");
//			for (String line : FileOps.LoadFilebyLine(dir + "weibo_weibo")) {
//				String[] sep = line.split("\t");
//				weibo_weibo.get(weibo_rid.get(Long.valueOf(sep[0])))
//						.add(sep[1]);
//			}
//			System.out.println("Weibo Weibo Loaded");
		}
		// Load Truth
		{
			truth = new HashMap<Integer, Integer>();
			LinkedList<String> lines = FileOps.LoadFilebyLine(dir + "users");
			for (String line : lines) {
				String[] sep = line.split("\t");
				truth.put(douban_rid.get(Long.valueOf(sep[1])), weibo_rid.get(Long.valueOf(sep[2])));
			}
			System.out.println("Ground Truth Loaded");
		}

	}

	public void genTrain(double ratio) {
		train = new HashMap<Integer, Integer>();
		Random random = new Random();
		for (Integer uid : truth.keySet())
			if (random.nextDouble() < ratio)
				train.put(uid, truth.get(uid));
	}
}