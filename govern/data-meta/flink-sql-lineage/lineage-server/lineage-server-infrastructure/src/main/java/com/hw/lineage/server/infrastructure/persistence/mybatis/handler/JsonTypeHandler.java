package com.hw.lineage.server.infrastructure.persistence.mybatis.handler;

import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @description: JsonTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class JsonTypeHandler<E extends Object> extends BaseTypeHandler<E> {

    private Class<E> clazz;

    public JsonTypeHandler() {
    }

    protected JsonTypeHandler(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return JSON.parseObject(rs.getString(columnName), clazz);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return JSON.parseObject(rs.getString(columnIndex), clazz);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return JSON.parseObject(cs.getString(columnIndex), clazz);
    }
}
