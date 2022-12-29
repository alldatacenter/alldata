package datart.core.mappers.ext;

import datart.core.common.Application;
import org.springframework.jdbc.core.JdbcTemplate;

public interface BaseMapper {

    default <T> T executeQuery(String sql, Class<T> requiredType, Object... args) {
        JdbcTemplate jdbcTemplate = Application.getBean(JdbcTemplate.class);
        return jdbcTemplate.queryForObject(sql, requiredType, args);
    }

    default int executeDelete(String sql, Object... args) {
        JdbcTemplate jdbcTemplate = Application.getBean(JdbcTemplate.class);
        return jdbcTemplate.update(sql, args);
    }

}