package com.hw.lineage.server.infrastructure.persistence.mapper;

import java.sql.JDBCType;
import java.util.Date;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class RoleUserDynamicSqlSupport {
    public static final RoleUser roleUser = new RoleUser();

    public static final SqlColumn<Long> id = roleUser.id;

    public static final SqlColumn<Long> userId = roleUser.userId;

    public static final SqlColumn<Long> roleId = roleUser.roleId;

    public static final SqlColumn<Long> createBy = roleUser.createBy;

    public static final SqlColumn<Date> createTime = roleUser.createTime;

    public static final SqlColumn<Long> updateBy = roleUser.updateBy;

    public static final SqlColumn<Date> updateTime = roleUser.updateTime;

    public static final class RoleUser extends AliasableSqlTable<RoleUser> {
        public final SqlColumn<Long> id = column("`id`", JDBCType.BIGINT);

        public final SqlColumn<Long> userId = column("`user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> roleId = column("`role_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> createBy = column("`create_by`", JDBCType.BIGINT);

        public final SqlColumn<Date> createTime = column("`create_time`", JDBCType.TIMESTAMP);

        public final SqlColumn<Long> updateBy = column("`update_by`", JDBCType.BIGINT);

        public final SqlColumn<Date> updateTime = column("`update_time`", JDBCType.TIMESTAMP);

        public RoleUser() {
            super("rel_role_user", RoleUser::new);
        }
    }
}