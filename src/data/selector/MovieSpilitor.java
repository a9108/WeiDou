package data.selector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;

import data.MovieLog;
import BasicOps.Config;
import BasicOps.FileOps;

public class MovieSpilitor {
	public static void main(String[] args) {
		Config.load("config.txt");
		HashMap<String, LinkedList<String> >outdata=new HashMap<String, LinkedList<String>>();
		try {
			int cnt = 0;
			BufferedReader fin = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(Config.getValue("DataDir") + "douban_usermovie")),"GB2312"));
			for (;;) {
				cnt++;
				if (cnt % 10000 == 0)
					System.out.println("Loading Movie # : " + cnt);
				String s = fin.readLine();
				if (s == null)
					break;
				MovieLog cur = new MovieLog(s);
				String uid=cur.uid;
				if (!outdata.containsKey(uid))
					outdata.put(uid, new LinkedList<String>());
				outdata.get(uid).add(s);
			}
			fin.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		for (String uid:outdata.keySet())
			FileOps.SaveFile(Config.getValue("DataDir")+"usermovie\\"+uid, outdata.get(uid));
	}
}
