package com.platform.spark.dao.factory;

import com.platform.spark.dao.IAdBlacklistDAO;
import com.platform.spark.dao.IAdClickTrendDAO;
import com.platform.spark.dao.IAdStatDAO;
import com.platform.spark.dao.ITaskDAO;
import com.platform.spark.dao.IAdProvinceTop3DAO;
import com.platform.spark.dao.IAdUserClickCountDAO;
import com.platform.spark.dao.IAreaTop3ProductDAO;
import com.platform.spark.dao.IPageSplitConvertRateDAO;
import com.platform.spark.dao.ISessionAggrStatDAO;
import com.platform.spark.dao.ISessionDetailDAO;
import com.platform.spark.dao.ISessionRandomExtractDAO;
import com.platform.spark.dao.ITop10CategoryDAO;
import com.platform.spark.dao.ITop10SessionDAO;
import com.platform.spark.dao.impl.AdBlacklistDAOImpl;
import com.platform.spark.dao.impl.AdClickTrendDAOImpl;
import com.platform.spark.dao.impl.AdProvinceTop3DAOImpl;
import com.platform.spark.dao.impl.AdStatDAOImpl;
import com.platform.spark.dao.impl.AdUserClickCountDAOImpl;
import com.platform.spark.dao.impl.AreaTop3ProductDAOImpl;
import com.platform.spark.dao.impl.PageSplitConvertRateDAOImpl;
import com.platform.spark.dao.impl.SessionAggrStatDAOImpl;
import com.platform.spark.dao.impl.SessionDetailDAOImpl;
import com.platform.spark.dao.impl.SessionRandomExtractDAOImpl;
import com.platform.spark.dao.impl.TaskDAOImpl;
import com.platform.spark.dao.impl.Top10CategoryDAOImpl;
import com.platform.spark.dao.impl.Top10SessionDAOImpl;

/**
 * DAO工厂类
 * @author wulinhao
 *
 */
public class DAOFactory {


	public static ITaskDAO getTaskDAO() {
		return new TaskDAOImpl();
	}

	public static ISessionAggrStatDAO getSessionAggrStatDAO() {
		return new SessionAggrStatDAOImpl();
	}
	
	public static ISessionRandomExtractDAO getSessionRandomExtractDAO() {
		return new SessionRandomExtractDAOImpl();
	}
	
	public static ISessionDetailDAO getSessionDetailDAO() {
		return new SessionDetailDAOImpl();
	}
	
	public static ITop10CategoryDAO getTop10CategoryDAO() {
		return new Top10CategoryDAOImpl();
	}
	
	public static ITop10SessionDAO getTop10SessionDAO() {
		return new Top10SessionDAOImpl();
	}
	
	public static IPageSplitConvertRateDAO getPageSplitConvertRateDAO() {
		return new PageSplitConvertRateDAOImpl();
	}
	
	public static IAreaTop3ProductDAO getAreaTop3ProductDAO() {
		return new AreaTop3ProductDAOImpl();
	}
	
	public static IAdUserClickCountDAO getAdUserClickCountDAO() {
		return new AdUserClickCountDAOImpl();
	}
	
	public static IAdBlacklistDAO getAdBlacklistDAO() {
		return new AdBlacklistDAOImpl();
	}
	
	public static IAdStatDAO getAdStatDAO() {
		return new AdStatDAOImpl();
	}
	
	public static IAdProvinceTop3DAO getAdProvinceTop3DAO() {
		return new AdProvinceTop3DAOImpl();
	}
	
	public static IAdClickTrendDAO getAdClickTrendDAO() {
		return new AdClickTrendDAOImpl();
	}
	
}
