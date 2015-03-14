package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;

import BasicOps.Config;
import BasicOps.FileOps;

public class Douban {

	public LinkedList<DoubanUser> users;
	private HashMap<String, Integer> rid;

	private HashMap<String, String> username;

	public void loadUser() {
		if (users != null)
			return;
		LinkedList<String> content = FileOps.LoadFilebyLine(Config
				.getValue("DataDir") + "douban_user");
		users = new LinkedList<DoubanUser>();
		rid = new HashMap<String, Integer>();
		for (String cur : content) {
			users.add(new DoubanUser(cur));
			rid.put(users.getLast().uid, users.size() - 1);
		}
		username = new HashMap<String, String>();
		for (DoubanUser user : users)
			username.put(user.uid, user.display);
	}

	public DoubanUser getUser(String uid){
		return users.get(rid.get(uid));
	}
	public void loadMovieLog() {
		try {
			int cnt = 0;
			BufferedReader fin = new BufferedReader(new FileReader(
					Config.getValue("DataDir") + "douban_usermovie"));
			for (;;) {
				cnt++;
				if (cnt % 1000 == 0)
					System.out.println("Loading Movie # : " + cnt);
				String s = fin.readLine();
				if (s == null)
					break;
				MovieLog cur = new MovieLog(s);
				users.get(rid.get(cur.uid)).addMovie(cur);
			}
			fin.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String getUsername(String uid) {
		if (username.containsKey(uid))
			return username.get(uid);
		return "NOT-EXIST";
	}
}
