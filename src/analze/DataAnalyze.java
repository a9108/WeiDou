package analze;

import java.util.HashMap;
import java.util.LinkedList;

import basic.Config;
import basic.FileOps;

public class DataAnalyze {
	public static void main(String[] args) {
		Config.load("config.txt");
		LinkedList<String> users = FileOps.LoadFilebyLine(Config
				.getValue("SelectDir") + "users");
		System.out.println("# Users : " + users.size());
		HashMap<Integer, Integer> watched_cnt = new HashMap<Integer, Integer>();
		HashMap<Long, Integer> weibo_cnt = new HashMap<Long, Integer>();
		for (String line : FileOps.LoadFilebyLine(Config.getValue("SelectDir")
				+ "douban_userwatched")) {
			String[] sep = line.split("\t");
			int id = Integer.valueOf(sep[0]);
			if (watched_cnt.containsKey(id))
				watched_cnt.put(id, watched_cnt.get(id) + 1);
			else
				watched_cnt.put(id, 1);
		}
		for (String line : FileOps.LoadFilebyLine(Config.getValue("SelectDir")
				+ "weibo_weibo")) {
			String[] sep = line.split(":");
			long id = Long.valueOf(sep[0]);
			if (weibo_cnt.containsKey(id))
				weibo_cnt.put(id, weibo_cnt.get(id) + 1);
			else
				weibo_cnt.put(id, 1);
		}

		LinkedList<String> outdata = new LinkedList<String>();
		for (String user : users) {
			int cntd = 0, cntw = 0;
			String[] sep = user.split("\t");
			int did = Integer.valueOf(sep[1]);
			long wid = Long.valueOf(sep[2]);
			if (watched_cnt.containsKey(did))
				cntd = watched_cnt.get(did);
			if (weibo_cnt.containsKey(wid))
				cntw = weibo_cnt.get(wid);
			outdata.add(cntd + "\t" + cntw);
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "actions_cnt", outdata);
//		HashMap<Integer, V>
		outdata=new LinkedList<String>();
	}
}
