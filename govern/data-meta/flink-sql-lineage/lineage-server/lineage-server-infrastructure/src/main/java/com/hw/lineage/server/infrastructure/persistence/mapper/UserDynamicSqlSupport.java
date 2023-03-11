package com.hw.lineage.server.infrastructure.persistence.mapper;

import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class UserDynamicSqlSupport {
    public static final User user = new User();

    public static final SqlColumn<Long> userId = user.userId;

    public static final SqlColumn<String> username = user.username;

    public static final SqlColumn<String> password = user.password;

    public static final SqlColumn<Boolean> locked = user.locked;

    public static final SqlColumn<Long> createTime = user.createTime;

    public static final SqlColumn<Long> modifyTime = user.modifyTime;

    public static final SqlColumn<Boolean> invalid = user.invalid;

    public static final class User extends AliasableSqlTable<User> {
        public final SqlColumn<Long> userId = column("`user_id`", JDBCType.BIGINT);

        public final SqlColumn<String> username = column("`username`", JDBCType.VARCHAR);

        public final SqlColumn<String> password = column("`password`", JDBCType.VARCHAR);

        public final SqlColumn<Boolean> locked = column("`locked`", JDBCType.BIT);

        public final SqlColumn<Long> createTime = column("`create_time`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyTime = column("`modify_time`", JDBCType.BIGINT);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public User() {
            super("bas_user", User::new);
        }
    }
}