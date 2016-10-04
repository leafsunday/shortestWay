/**
 * 版权所有：恒生电子股份有限公司
 * 项目名称:shortestway
 * 创建者: yexh
 * 创建日期: 2016年10月4日
 * 文件说明: 
 */
package com.shortestway;


/**
 * @author yexh
 *
 */
public class SitePath {
	private int from;
	private int to;
	private double weight;
	private StringBuffer path;
	
	public SitePath(){}
	
	/**
	 * @param from
	 * @param to
	 * @param weight
	 * @param path
	 * @param subSiteError
	 * @param avoidPathError
	 */
	public SitePath(int from, int to, double weight, StringBuffer path) {
		this.from = from;
		this.to = to;
		this.weight = weight;
		this.path = path;
	}

	public int getFrom() {
		return from;
	}
	
	public void setFrom(int from) {
		this.from = from;
	}
	
	public int getTo() {
		return to;
	}
	
	public void setTo(int to) {
		this.to = to;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public StringBuffer getPath() {
		return path;
	}
	
	public void setPath(StringBuffer path) {
		this.path = path;
	}
}
