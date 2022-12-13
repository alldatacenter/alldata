package cn.datax.common.database.dialect;

/**
 * ORACLE Oracle12c+数据库方言
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public class Oracle12cDialect extends OracleDialect {

    @Override
    public String buildPaginationSql(String originalSql, long offset, long count) {
        StringBuilder sqlBuilder = new StringBuilder(originalSql);
        sqlBuilder.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(count).append(" ROWS ONLY ");
        return sqlBuilder.toString();
    }
}
