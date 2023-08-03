package com.netease.arctic.server.persistence;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.io.Closeable;
import java.util.function.Supplier;

public final class NestedSqlSession implements Closeable {
  @VisibleForTesting
  protected static final int MAX_NEST_BEGIN_COUNT = 5;
  @VisibleForTesting
  protected static final ThreadLocal<NestedSqlSession> sessions = new ThreadLocal<>();

  private int nestCount = 0;
  private boolean isRollingback = false;
  private SqlSession sqlSession;


  public static NestedSqlSession openSession(Supplier<SqlSession> sessionSupplier) {
    NestedSqlSession session = sessions.get();
    if (session == null) {
      sessions.set(new NestedSqlSession(sessionSupplier.get()));
      return sessions.get();
    } else {
      return session.openNestedSession();
    }
  }

  protected SqlSession getSqlSession() {
    return sqlSession;
  }

  @VisibleForTesting
  protected NestedSqlSession(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }

  NestedSqlSession openNestedSession() {
    checkState(true);
    Preconditions.checkState(nestCount < MAX_NEST_BEGIN_COUNT && nestCount >= 0,
        "openNestedSession() has not been properly called for nest count is " + nestCount);
    nestCount++;
    return this;
  }

  public void commit() {
    checkState(true);
    if (nestCount == 0) {
      sqlSession.commit(true);
    }
  }

  public void rollback() {
    checkState(false);
    isRollingback = true;
    if (nestCount == 0) {
      sqlSession.rollback(true);
      isRollingback = false;
    }
  }

  private void checkState(boolean checkRollingback) {
    Preconditions.checkState(sqlSession != null, "session already closed");
    if (checkRollingback) {
      Preconditions.checkState(!isRollingback,
          "session is rolling back, can not execute operation");
    }
  }

  public void close() {
    if (nestCount > 0) {
      nestCount--;
    } else if (nestCount == 0 && sqlSession != null) {
      sqlSession.close();
      sqlSession = null;
      sessions.set(null);
      nestCount = -1;
    }
  }
}
