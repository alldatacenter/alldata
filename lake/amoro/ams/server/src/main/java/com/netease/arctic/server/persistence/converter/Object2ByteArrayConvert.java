package com.netease.arctic.server.persistence.converter;

import com.netease.arctic.server.utils.CompressUtil;
import com.netease.arctic.utils.SerializationUtil;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class Object2ByteArrayConvert<T> implements TypeHandler<T> {

  @Override
  public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
    if (parameter == null) {
      ps.setNull(i, Types.BLOB);
      return;
    }

    ps.setBinaryStream(
        i,
        new ByteArrayInputStream(CompressUtil.gzip(SerializationUtil.simpleSerialize(parameter).array())));
  }

  @Override
  public T getResult(ResultSet rs, String columnName) throws SQLException {
    byte[] bytes = rs.getBytes(columnName);
    if (bytes == null) {
      return null;
    }
    return SerializationUtil.simpleDeserialize(CompressUtil.unGzip(bytes));
  }

  @Override
  public T getResult(ResultSet rs, int columnIndex) throws SQLException {
    byte[] bytes = rs.getBytes(columnIndex);
    if (bytes == null) {
      return null;
    }
    return SerializationUtil.simpleDeserialize(CompressUtil.unGzip(bytes));
  }

  @Override
  public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
    byte[] bytes = cs.getBytes(columnIndex);
    if (bytes == null) {
      return null;
    }
    return SerializationUtil.simpleDeserialize(CompressUtil.unGzip(bytes));
  }
}