package com.platform.website.transformer.service.impl;

import com.platform.website.common.GlobalConstants;
import com.platform.website.util.JdbcManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;

public class InboundDimensionService {

  //全部的inbound id
  public static final int ALL_OF_INBOUND_ID  = 1;

  //其他外链的inbound id
  public static final int OTHER_OF_INBOUND_ID = 2;



  /**
   * 获取数据库中dimension_inbound表url和id的映射关系
   * @param conf
   * @param type
   * @return
   */
  public static  Map<String, Integer> getInboundByType(Configuration conf, int type) throws IOException {
    Connection conn  = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try{
      conn  = JdbcManager.getConnection(conf, GlobalConstants.WAREHOUSE_OF_WEBSITE);
      pstmt = conn.prepareStatement("SELECT `id`, `url` FROM `dimension_inbound` WHERE `type`=?");
      pstmt.setInt(1, type);
      rs = pstmt.executeQuery();
      Map<String, Integer> result = new HashMap<String, Integer>();
      //处理返回结果
      while(rs.next()){
        int id = rs.getInt("id");
        String url = rs.getString("url");
        result.put(url, id);
      }
      return result;
    } catch (SQLException e) {
      e.printStackTrace();
    }finally {
      JdbcManager.close(conn, null, null);
    }
    throw new IOException("获取dimension_inbound的url与id映射关系出异常");
  }

}
