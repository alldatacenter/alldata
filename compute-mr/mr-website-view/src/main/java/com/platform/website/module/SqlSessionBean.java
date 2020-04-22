package com.platform.website.module;

import org.apache.ibatis.session.SqlSession;

public class SqlSessionBean {
  private SqlSession sqlsession;

  public SqlSession getSqlsession() {
    return sqlsession;
  }

  public void setSqlsession(SqlSession sqlsession) {
    this.sqlsession = sqlsession;
  }
}
