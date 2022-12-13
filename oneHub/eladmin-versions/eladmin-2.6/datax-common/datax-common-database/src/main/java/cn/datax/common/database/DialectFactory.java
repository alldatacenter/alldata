package cn.datax.common.database;

import cn.datax.common.database.constants.DbType;
import cn.datax.common.database.dialect.DialectRegistry;

/**
 * 方言工厂类
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public class DialectFactory {

    private static final DialectRegistry DIALECT_REGISTRY = new DialectRegistry();

    public static DbDialect getDialect(DbType dbType) {
        return DIALECT_REGISTRY.getDialect(dbType);
    }
}
