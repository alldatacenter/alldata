package com.platform.realtime.view.dao.impl;


import com.platform.realtime.view.dao.IAdProvinceTop3DAO;
import com.platform.realtime.view.module.AdProvinceTop3;
import com.platform.realtime.view.jdbc.JDBCHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 各省份top3热门广告DAO实现类
 * @author wulinhao
 *
 */
public class AdProvinceTop3DAOImpl implements IAdProvinceTop3DAO {

	@Override
	public void updateBatch(List<AdProvinceTop3> adProvinceTop3s) {
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		
		// 先做一次去重（date_province）
		List<String> dateProvinces = new ArrayList<String>();
		
		for(AdProvinceTop3 adProvinceTop3 : adProvinceTop3s) {
			String date = adProvinceTop3.getDate();
			String province = adProvinceTop3.getProvince();
			String key = date + "_" + province;
			
			if(!dateProvinces.contains(key)) {
				dateProvinces.add(key);
			}
		}
		
		// 根据去重后的date和province，进行批量删除操作
		String deleteSQL = "DELETE FROM ad_province_top3 WHERE date=? AND province=?";
		
		List<Object[]> deleteParamsList = new ArrayList<Object[]>();
		
		for(String dateProvince : dateProvinces) {
			String[] dateProvinceSplited = dateProvince.split("_");
			String date = dateProvinceSplited[0];
			String province = dateProvinceSplited[1];
			
			Object[] params = new Object[]{date, province};
			deleteParamsList.add(params);
		}
		
		jdbcHelper.executeBatch(deleteSQL, deleteParamsList);
		
		// 批量插入传入进来的所有数据
		String insertSQL = "INSERT INTO ad_province_top3 VALUES(?,?,?,?)";  
		
		List<Object[]> insertParamsList = new ArrayList<Object[]>();
		
		for(AdProvinceTop3 adProvinceTop3 : adProvinceTop3s) {
			Object[] params = new Object[]{adProvinceTop3.getDate(),
					adProvinceTop3.getProvince(),
					adProvinceTop3.getAdid(),
					adProvinceTop3.getClickCount()};
			
			insertParamsList.add(params);
		}
		
		jdbcHelper.executeBatch(insertSQL, insertParamsList);
	}

}
