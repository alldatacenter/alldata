package com.hw.lineage.server.infrastructure.persistence.mybatis.handler;

import com.hw.lineage.common.enums.basic.IntEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hw.lineage.common.util.Preconditions.checkNotNull;

/**
 * @description: IntEnumTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public abstract class IntEnumTypeHandler<E extends IntEnum> extends BaseTypeHandler<E> {
    private final Map<Integer, E> enumMap;

    protected IntEnumTypeHandler(Class<E> type) {
        E[] enums = type.getEnumConstants();
        checkNotNull(enums, "%s does not represent an enum type.", type.getSimpleName());
        this.enumMap = Stream.of(enums).collect(Collectors.toMap(IntEnum::value, e -> e));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.value());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return enumMap.get(rs.getInt(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return enumMap.get(rs.getInt(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return enumMap.get(cs.getInt(columnIndex));
    }
}
