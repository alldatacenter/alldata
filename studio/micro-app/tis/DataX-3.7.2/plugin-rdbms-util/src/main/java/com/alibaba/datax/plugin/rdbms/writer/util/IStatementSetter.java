package com.alibaba.datax.plugin.rdbms.writer.util;

import com.alibaba.datax.common.element.Column;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-04 13:14
 **/
public interface IStatementSetter {
    void set(PreparedStatement statement, int colIndex, Column column) throws SQLException;
}
