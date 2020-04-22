package com.platform.website.service.impl;

import com.platform.website.module.SqlSessionBean;
import com.platform.website.utils.ApplicationContextUtil;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;

@Component
public class BaseDao {

  private static SqlSession sqlsession = null;

  private static SqlSessionBean sqlSessionBean = null;

  static {
    sqlSessionBean = (SqlSessionBean) ApplicationContextUtil.getApplicationContext()
        .getBean("sqlSessionBean");
    sqlsession = sqlSessionBean.getSqlsession();
  }

  protected SqlSession getSqlSession() {
    return sqlsession;
  }
}
