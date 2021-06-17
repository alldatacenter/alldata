package com.platform.schedule.task.offline;

import com.platform.schedule.entity.Property;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

public class BaseTask {
    public static void baseTask() throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        // mysql datasource
        TypeInformation[] fieldTypes = new TypeInformation[] {
                BasicTypeInfo.INT_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO
        };
        RowTypeInfo rowTypeInfo = new RowTypeInfo(BasicTypeInfo.INT_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO);
        JDBCInputFormat jdbcInputFormat = JDBCInputFormat.buildJDBCInputFormat()
                .setDrivername(new String(Property.getStrValue("mysql.driver")))
                .setDBUrl(new String(Property.getStrValue("mysql.url")))
                .setUsername(new String(Property.getStrValue("mysql.username")))
                .setPassword(new String(Property.getStrValue("mysql.password")))
                .setQuery("select productId, name, tags from product")
                .setRowTypeInfo(rowTypeInfo)
                .finish();
        DataSet dataSet = env.createInput(jdbcInputFormat, rowTypeInfo);
        dataSet.map(new MapFunction<Row, Row>() {
            @Override
            public Row map(Row r) throws Exception {
                String s = r.getField(2).toString().replace("|", " ");
                return  Row.of(r.getField(0), r.getField(1), s);
            }
        }).print();
        // TODO 计算 tf-idf
        env.execute();
    }
    public static void main(String[] args) throws Exception {
        baseTask();
    }
}
