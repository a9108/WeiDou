package data;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import BasicOps.StringOps;

public class MovieLog {
	public String uid;
	public int mid;
	public String name;
	public String time;
	public int rate;
	public String tag;
	public String review;
	public MovieLog() {
	}
	public MovieLog(String s){
		LinkedList<String> sep=StringOps.splitForce(s, "\t");
		uid=sep.get(0);
		mid=Integer.valueOf(sep.get(1));
		name=sep.get(2);
		rate=Integer.valueOf(sep.get(3));
		tag=sep.get(4);
		review=sep.get(5);
	}
	public boolean parse(String uid,String content){
		mid=rate=0;name=time=tag=review="";
		this.uid=uid;
		Matcher matcher=Pattern.compile("subject/(\\d*)/\">(.*?)</a>").matcher(content);
		if (!matcher.find())
			return false;
		mid=Integer.valueOf(matcher.group(1));
		if (mid==0) System.out.println(content);
		name=matcher.group(2);
		matcher=Pattern.compile("<span>\\((\\d)星\\)</span>").matcher(content);
		if (matcher.find())
			rate=Integer.valueOf(matcher.group(1));
		matcher=Pattern.compile("<br>(\\d*-\\d*-\\d*)").matcher(content);
		if (matcher.find()) time=matcher.group(1);
		else System.err.println(content);
		matcher=Pattern.compile("短评: (.*)").matcher(content);
		if (matcher.find())
			review=matcher.group(1).trim();
		matcher=Pattern.compile("短评: (.*?)<br>").matcher(content);
		if (matcher.find())
			review=matcher.group(1).trim();
		matcher=Pattern.compile("标签: (.*)").matcher(content);
		if (matcher.find())
			tag=matcher.group(1).trim();
		matcher=Pattern.compile("标签: (.*?)<br>").matcher(content);
		if (matcher.find())
			tag=matcher.group(1).trim();
		return true;
	}
	@Override
	public String toString() {
		return uid+"\t"+mid+"\t"+name+"\t"+rate+"\t"+time+"\t"+simplify(tag)+"\t"+simplify(review);
	}
	private String simplify(String s){
		return s.trim().replace("\t", " ").replace("\n", " ").replace("\r", " ");
	}
}
