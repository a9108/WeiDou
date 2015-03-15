package data.selector;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import data.Douban;
import data.Weibo;
import BasicOps.Config;
import BasicOps.FileOps;


public class UserSelector {
	public static void main(String[] args) {
		Config.load("config.txt");
		Douban douban=new Douban();
		douban.loadUser();
		System.out.println("Douban Loaded");
		Weibo weibo=new Weibo();
		weibo.loadUser();
		System.out.println("Weibo Loaded");
		HashMap<String, String> userlink=FileOps.LoadDictionSS(Config.getValue("DataDir")+"douban_weibo");
		
		LinkedList<String[]> user=new LinkedList<String[]>();
		for (String s:userlink.keySet())
			user.add(new String[]{s,userlink.get(s)});
		
		System.out.println("Current User Count : "+user.size());
		
		{
			System.out.println("Removing [已注销]");
			LinkedList<String[]> old=new LinkedList<String[]>(user);
			user.clear();
			for (String[] s:old)
				if (douban.getUsername(s[0]).equals("[已注销]")) continue;
				else user.add(s);
			System.out.println("Current User Count : "+user.size());
		}
		{
			System.out.println("Removing No Movie Log");
			LinkedList<String[]> old=new LinkedList<String[]>(user);
			user.clear();
			for (String[] s:old){
				String uid=s[0];
				LinkedList<String> movies=FileOps.LoadFilebyLine(Config.getValue("DataDir")+"douban_usermovie\\"+uid);
				if (movies.size()<10)
					System.out.println("Removing User : "+uid+"\t # Movies : "+movies.size());
				else user.add(s);
			}
			System.out.println("Current User Count : "+user.size());
		}
		{
			System.out.println("Removing No Weibo");
			weibo.loadWeibo();
			
			LinkedList<String[]> old=new LinkedList<String[]>(user);
			user.clear();
			for (String[] s:old){
				int cnt=weibo.getWeibo(s[1]).size();
				if (cnt<100)
					System.out.println("Removing user : "+s[1]+"\t # Weibos : "+cnt);
				else user.add(s);
			}
			System.out.println("Current User Count : "+user.size());
		}
		{
		}
		
		LinkedList<String> outdata=new LinkedList<String>();
		for (int i=0;i<user.size();i++)
			outdata.add(i+"\t"+user.get(i)[0]+"\t"+user.get(i)[1]
//					+"\t"+douban.getUsername(user.get(i)[0])+"\t"+weibo.getUsername(user.get(i)[1])
					);
		FileOps.SaveFile(Config.getValue("SelectDir")+"users", outdata);
	}
}
