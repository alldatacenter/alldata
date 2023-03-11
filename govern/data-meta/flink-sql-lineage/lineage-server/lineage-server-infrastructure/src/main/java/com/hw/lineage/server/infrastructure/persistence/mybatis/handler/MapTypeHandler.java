package com.hw.lineage.server.infrastructure.persistence.mybatis.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @description: MapTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class MapTypeHandler extends BaseTypeHandler<Map<String, String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType) throws SQLException {
        if(MapUtils.isNotEmpty(parameter)) {
            String x = JSON.toJSONString(parameter);
            ps.setString(i, x);
        }else{
            ps.setString(i, null);
        }
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String s = rs.getString(columnName);
        return StringUtils.isBlank(s) ? null : JSON.parseObject(s, new TypeReference<Map<String, String>>(){}); 
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String s = rs.getString(columnIndex);
        return StringUtils.isBlank(s) ? null : JSON.parseObject(s, new TypeReference<Map<String, String>>(){});
    }

    @Override
    public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String s = cs.getString(columnIndex);
        return StringUtils.isBlank(s) ? null : JSON.parseObject(s, new TypeReference<Map<String, String>>(){});
    }
}
