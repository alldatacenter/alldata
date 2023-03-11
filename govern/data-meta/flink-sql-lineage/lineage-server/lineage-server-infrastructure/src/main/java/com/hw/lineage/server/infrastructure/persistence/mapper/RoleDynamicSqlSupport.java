package com.hw.lineage.server.infrastructure.persistence.mapper;

import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class RoleDynamicSqlSupport {
    public static final Role role = new Role();

    public static final SqlColumn<Long> roleId = role.roleId;

    public static final SqlColumn<String> roleName = role.roleName;

    public static final SqlColumn<Long> createTime = role.createTime;

    public static final SqlColumn<Long> modifyTime = role.modifyTime;

    public static final SqlColumn<Boolean> invalid = role.invalid;

    public static final class Role extends AliasableSqlTable<Role> {
        public final SqlColumn<Long> roleId = column("`role_id`", JDBCType.BIGINT);

        public final SqlColumn<String> roleName = column("`role_name`", JDBCType.VARCHAR);

        public final SqlColumn<Long> createTime = column("`create_time`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyTime = column("`modify_time`", JDBCType.BIGINT);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public Role() {
            super("bas_role", Role::new);
        }
    }
}