package com.platform.website.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * 操作member_info表的工具类，主要作用判断member id是否是正常的id以及是一个新的访问会员id
 */
public class MemberUtil {

  private static Map<String, Boolean> cache = new LinkedHashMap<String, Boolean>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
      return this.size() > 10000; //最多保存1w条数据
    }
  };

  public static void deleteMemberInfoByDate(String date, Connection connection)throws SQLException{
    PreparedStatement pstmt=null;
    try{
      pstmt = connection.prepareStatement("DELETE FROM `member_info` WHERE `created` = ?");
      pstmt.setString(1, date);
      pstmt.execute();
    }finally {
      if (pstmt != null){
        try{
          pstmt.close();
        }catch (Exception e){
          //nothing
        }
      }
    }
  }

  public static boolean isValidateMemberId(String memberId) {
    if (StringUtils.isNotBlank(memberId)) {
      return memberId.trim().matches("[0-9A-Za-z]{1,32}");
    }
    return false;
  }

  /**
   * 判断memberId是不是新会员id
   */
  public static boolean isNewMemberId(String memberId, Connection connection) throws SQLException {
    Boolean isNewMemberId = null;
    if (StringUtils.isNotBlank(memberId)) {
      isNewMemberId = cache.get(memberId);
      if (isNewMemberId == null) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
          pstmt = connection.prepareStatement(
              "SELECT `member_id`,  `last_visit_date` FROM `member_info` WHERE `member_id` = ?");
          pstmt.setString(1, memberId);
          rs = pstmt.executeQuery();
          if (rs.next()) {
            //数据库有
            isNewMemberId = Boolean.valueOf(false);
          } else {
            isNewMemberId = Boolean.valueOf(true);
          }

          cache.put(memberId, isNewMemberId);
        } finally {
          if (rs != null) {
            try {
              rs.close();
            } catch (SQLException e) {

            }
          }
        }

      }

    }
    return isNewMemberId == null ? false : isNewMemberId.booleanValue();
  }


}
