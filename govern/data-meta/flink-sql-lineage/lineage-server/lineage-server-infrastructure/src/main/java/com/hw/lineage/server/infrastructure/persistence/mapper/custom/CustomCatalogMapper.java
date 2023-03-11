package com.hw.lineage.server.infrastructure.persistence.mapper.custom;

import com.hw.lineage.server.domain.query.catalog.CatalogEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import java.util.Optional;

/**
 * @description: CustomCatalogMapper
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Mapper
public interface CustomCatalogMapper {
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @Results(id = "FunctionEntryResult", value = {
            @Result(column = "plugin_code", property = "pluginCode", jdbcType = JdbcType.VARCHAR),
            @Result(column="catalog_id", property="catalogId", jdbcType=JdbcType.BIGINT),
            @Result(column = "catalog_name", property = "catalogName", jdbcType = JdbcType.VARCHAR)
    })
    Optional<CatalogEntry> selectOne(SelectStatementProvider selectStatement);
}