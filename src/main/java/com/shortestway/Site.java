/**
 * 版权所有：恒生电子股份有限公司
 * 项目名称:shortestway
 * 创建者: yexh
 * 创建日期: 2016年9月30日
 * 文件说明: 站点domain
 */
package com.shortestway;


/**
 * @author yexh
 *
 */
public class Site {
	
	private int id;//id 连续的 from 1 to Max
	
	private int parentId;//主站
	
	private String siteNo;//站点编号
	
	private String siteName;//站点名称

	
	public int getId() {
		return id;
	}

	
	public void setId(int id) {
		this.id = id;
	}

	
	public int getParentId() {
		return parentId;
	}

	
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}


	public String getSiteNo() {
		return siteNo;
	}

	
	public void setSiteNo(String siteNo) {
		this.siteNo = siteNo;
	}

	
	public String getSiteName() {
		return siteName;
	}

	
	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}
}
