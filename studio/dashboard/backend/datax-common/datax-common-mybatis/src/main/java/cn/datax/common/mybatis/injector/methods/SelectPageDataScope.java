package cn.datax.common.mybatis.injector.methods;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

public class SelectPageDataScope extends AbstractMethod {

    public SelectPageDataScope() {
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        SqlMethod sqlMethod = SqlMethod.SELECT_PAGE;
        String sql = String.format(sqlMethod.getSql(), this.sqlFirst(), this.sqlSelectColumns(tableInfo, true), tableInfo.getTableName(), this.sqlWhereEntityWrapper(true, tableInfo), this.sqlComment());
        SqlSource sqlSource = this.languageDriver.createSqlSource(this.configuration, sql, modelClass);
        return this.addSelectMappedStatementForTable(mapperClass, "selectPageDataScope", sqlSource, tableInfo);
    }
}
