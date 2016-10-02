/**
 * 版权所有：恒生电子股份有限公司
 * 项目名称:shortestway
 * 创建者: yexh
 * 创建日期: 2016年10月1日
 * 文件说明: 
 */
package com.shortestway.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author yexh
 *
 */
public class DBUtils {

	//数据库连接地址
	public static String URL = "jdbc:mysql://127.0.0.1:3306/test?serverTimezone=UTC";
	//用户名
	public static String USERNAME = "root";
	//密码
	public static String PASSWORD = "admin";
	//mysql的驱动类
	public static String DRIVER = "com.mysql.cj.jdbc.Driver";

	private static Logger logger = LogManager.getLogger("DBUtils");
	
	private DBUtils(){}

	//使用静态块加载驱动程序
	static{
		try {
			Class.forName(DRIVER);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	//定义一个获取数据库连接的方法
	public static Connection getConnection(){
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
		} catch (SQLException e) {
			logger.error("getConnection fail", e);
		}
		return conn;
	}

	/**
	 * 关闭数据库连接
	 * @param rs
	 * @param stat
	 * @param conn
	 */
	public static void close(ResultSet rs,PreparedStatement ps,Connection conn){
		try {
			if(rs!=null)rs.close();
			if(ps!=null)ps.close();
			if(conn!=null)conn.close();
		} catch (SQLException e) {
			logger.error("closeConnection fail", e);
		}
	}
}
