package com.hw.lineage.server.infrastructure.persistence.mapper;

import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class RolePermissionDynamicSqlSupport {
    public static final RolePermission rolePermission = new RolePermission();

    public static final SqlColumn<Long> rid = rolePermission.rid;

    public static final SqlColumn<Long> roleId = rolePermission.roleId;

    public static final SqlColumn<Long> permissionId = rolePermission.permissionId;

    public static final SqlColumn<Boolean> invalid = rolePermission.invalid;

    public static final class RolePermission extends AliasableSqlTable<RolePermission> {
        public final SqlColumn<Long> rid = column("`rid`", JDBCType.BIGINT);

        public final SqlColumn<Long> roleId = column("`role_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> permissionId = column("`permission_id`", JDBCType.BIGINT);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public RolePermission() {
            super("rel_role_permission", RolePermission::new);
        }
    }
}