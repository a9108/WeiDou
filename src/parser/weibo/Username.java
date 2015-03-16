package parser.weibo;

import java.io.File;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basic.Config;
import basic.FileOps;

public class Username {
	public static void main(String[] args) {
		Config.load("config.txt");
		LinkedList<String> uid=FileOps.LoadFilebyLine(Config.getValue("DataDir")+"weibo_user");
		LinkedList<String> outdata=new LinkedList<String>();
		for (String u:uid){
			String content=UserWeibo.loadWeiboPage(Config.getValue("RawDataDir")+"weibo\\user\\"+u+"\\1");
			Matcher matcher=Pattern.compile("<title>(.*?)的微博</title>").matcher(content);
			if (!matcher.find()){
				System.out.println(content);
			}else {
				outdata.add(u+"\t"+matcher.group(1));
				System.out.println(outdata.size()+"\t"+u+"\t"+matcher.group(1));
			}
		}
		FileOps.SaveFile(Config.getValue("DataDir")+"weibo_username", outdata);
	}
}
