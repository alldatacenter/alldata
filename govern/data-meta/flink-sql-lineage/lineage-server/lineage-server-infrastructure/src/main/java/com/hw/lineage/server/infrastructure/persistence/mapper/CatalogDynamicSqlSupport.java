package com.hw.lineage.server.infrastructure.persistence.mapper;

import com.alibaba.fastjson2.JSONObject;
import com.hw.lineage.common.enums.CatalogType;
import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class CatalogDynamicSqlSupport {
    public static final Catalog catalog = new Catalog();

    public static final SqlColumn<Long> catalogId = catalog.catalogId;

    public static final SqlColumn<Long> pluginId = catalog.pluginId;

    public static final SqlColumn<String> catalogName = catalog.catalogName;

    public static final SqlColumn<CatalogType> catalogType = catalog.catalogType;

    public static final SqlColumn<String> defaultDatabase = catalog.defaultDatabase;

    public static final SqlColumn<String> descr = catalog.descr;

    public static final SqlColumn<Boolean> defaultCatalog = catalog.defaultCatalog;

    public static final SqlColumn<Long> createUserId = catalog.createUserId;

    public static final SqlColumn<Long> modifyUserId = catalog.modifyUserId;

    public static final SqlColumn<Long> createTime = catalog.createTime;

    public static final SqlColumn<Long> modifyTime = catalog.modifyTime;

    public static final SqlColumn<Boolean> invalid = catalog.invalid;

    public static final SqlColumn<JSONObject> catalogProperties = catalog.catalogProperties;

    public static final class Catalog extends AliasableSqlTable<Catalog> {
        public final SqlColumn<Long> catalogId = column("`catalog_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> pluginId = column("`plugin_id`", JDBCType.BIGINT);

        public final SqlColumn<String> catalogName = column("`catalog_name`", JDBCType.VARCHAR);

        public final SqlColumn<CatalogType> catalogType = column("`catalog_type`", JDBCType.VARCHAR, "com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.CatalogTypeHandler");

        public final SqlColumn<String> defaultDatabase = column("`default_database`", JDBCType.VARCHAR);

        public final SqlColumn<String> descr = column("`descr`", JDBCType.VARCHAR);

        public final SqlColumn<Boolean> defaultCatalog = column("`default_catalog`", JDBCType.BIT);

        public final SqlColumn<Long> createUserId = column("`create_user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyUserId = column("`modify_user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> createTime = column("`create_time`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyTime = column("`modify_time`", JDBCType.BIGINT);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public final SqlColumn<JSONObject> catalogProperties = column("`catalog_properties`", JDBCType.LONGVARCHAR, "com.hw.lineage.server.infrastructure.persistence.mybatis.handler.JsonTypeHandler");

        public Catalog() {
            super("bas_catalog", Catalog::new);
        }
    }
}