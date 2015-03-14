package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tasks.WeiboUser;
import BasicOps.Config;
import BasicOps.FileOps;

public class Weibo {
	public LinkedList<String> users;
	public HashMap<String, String> umap;
	private HashMap<String, String> username;
	private HashMap<String, Integer> rid;
	private LinkedList<LinkedList<String> > weibos;
	
	public void loadUser(){
		username=new HashMap<String, String>();
		username=FileOps.LoadDictionSS(Config.getValue("DataDir")+"weibo_username");
		users=new LinkedList<String>();
		rid=new HashMap<String, Integer>();
		weibos=new LinkedList<LinkedList<String>>();
		for (String s:username.keySet()){
			rid.put(s, users.size());
			users.add(s);
			weibos.add(new LinkedList<String>());
		}
	}
	
	public LinkedList<String> getWeibo(String uid){
		return weibos.get(rid.get(uid));
	}
	
	public void loadWeibo(){
		try {
			int cnt = 0;
			BufferedReader fin = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(Config.getValue("DataDir") + "weibo_weibo")),"GB2312"));
			for (;;) {
				cnt++;
				if (cnt % 10000 == 0)
					System.out.println("Loading Weibo # : " + cnt);
				String s = fin.readLine();
				if (s == null)
					break;
				String[] sep=s.split("\t");
				if (sep.length!=2)continue;
				weibos.get(rid.get(sep[0])).add(sep[1]);
			}
			fin.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public String getUsername(String uid){
		if (username.containsKey(uid))
			return username.get(uid);
		else return "NOT-EXIST";
	}
	public void loadUmap(){
		System.out.println("###");
		umap=new HashMap<String, String>();
		int cnt=0;
		for (String uid:users){
			cnt++;
			umap.put(uid, uid);
			String content=FileOps.LoadFile(Config.getValue("RawDataDir")+"weibo\\user\\"+uid+"\\1");
			Matcher matcher=Pattern.compile("<a href=\"/(.*?)\\?(.*?)class=\"nl\">").matcher(content);
			if (matcher.find()){
				System.out.println(cnt+"\t"+matcher.group(1)+"\t-\t"+uid);
				umap.put(matcher.group(1), uid);
			}
			else 
				System.out.println(content);
//			return;
		}
	}
	public String queryWeiboId(String name){
		if (umap.containsKey(name))
			return umap.get(name);
		else return "";
	}
}
