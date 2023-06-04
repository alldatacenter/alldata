package com.platform.common;

import com.platform.system.dcJobConfig.domain.Jobconfig;
import com.platform.system.dcDbConfig.domain.Dbconfig;
import com.platform.system.dcJobInstance.domain.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class RunUtil {
    private static final Logger log = LoggerFactory.getLogger(RunUtil.class);

    public static final String PV_UV_SQL = "select base.pv,\n" +
            "       verify.pv as verify_pv,\n" +
            "       base.pv - verify.pv as diff_pv,\n" +
            "       base.uv,\n" +
            "       verify.uv as verify_uv,\n" +
            "       base.uv - verify.uv as diff_uv\n" +
            "  from (\n" +
            "        select 'num' as compareKey,\n" +
            "               count(1) as pv,\n" +
            "               count(distinct %s) as uv\n" +
            "          from %s\n" +
            "         %s\n" +
            "       )base\n" +
            "  left outer join (\n" +
            "        select 'num' as compareKey,\n" +
            "               count(1) as pv,\n" +
            "               count(distinct %s) as uv\n" +
            "          from %s\n" +
            "         %s\n" +
            "       )verify\n" +
            "    on base.compareKey=verify.compareKey";

    public static final String CONSISTENCY_SQL = "select compareKey,sum(base_num) as base_num,sum(verify_num) as verify_num,sum(base_verify_equal_num) as base_verify_equal_num from (\n" +
            "\n" +
            "select Coalesce(base.compareKey, verify.compareKey) as compareKey,\n" +
            "       sum(case when base.record_key is not null or base.record_key !='' then 1 else 0 end) as base_num,\n" +
            "       sum(case when verify.record_key is not null or verify.record_key !='' then 1 else 0 end) as verify_num,\n" +
            "       sum(case when base.record_key = verify.record_key then 1 else 0 end) as base_verify_equal_num\n" +
            "  from (\n" +
            "        select 'consistency' as compareKey,\n" +
            "               MD5(concat(%s)) as record_key\n" +
            "          from %s\n" +
            "         %s\n" +
            "       )base\n" +
            "  left join (\n" +
            "        select 'consistency' as compareKey,\n" +
            "               MD5(concat(%s)) as record_key\n" +
            "          from %s\n" +
            "          %s\n" +
            "       )verify\n" +
            "    on base.compareKey=verify.compareKey\n" +
            "   and base.record_key=verify.record_key\n" +
            " group by Coalesce(base.compareKey, verify.compareKey)\n" +
            "\n" +
            "UNION\n" +
            "\n" +
            "select Coalesce(base.compareKey, verify.compareKey) as compareKey,\n" +
            "       sum(case when base.record_key is not null or base.record_key !='' then 1 else 0 end) as base_num,\n" +
            "       sum(case when verify.record_key is not null or verify.record_key !='' then 1 else 0 end) as verify_num,\n" +
            "       sum(case when base.record_key = verify.record_key then 1 else 0 end) as base_verify_equal_num\n" +
            "  from (\n" +
            "        select 'consistency' as compareKey,\n" +
            "               MD5(concat(%s)) as record_key\n" +
            "          from %s\n" +
            "          %s\n" +
            "       )base\n" +
            "  right join (\n" +
            "        select 'consistency' as compareKey,\n" +
            "               MD5(concat(%s)) as record_key\n" +
            "          from %s\n" +
            "         %s\n" +
            "       )verify\n" +
            "    on base.compareKey=verify.compareKey\n" +
            "   and base.record_key=verify.record_key\n" +
            " group by Coalesce(base.compareKey, verify.compareKey)\n" +
            "\n" +
            ") t GROUP BY compareKey";

    private static final String CHECK_SQL = "select base.originTablePrimary as base_originTablePrimary,\n" +
            "\t   fields_list_check\n" +
            "  from (\n" +
            "        select 'num' as compareKey,\n" +
            "               originTablePrimary,\n" +
            "               originTableFields\n" +
            "          from originTableName\n" +
            "          originTableFilter\n" +
            "       )base\n" +
            "  left join (\n" +
            "        select 'num' as compareKey,\n" +
            "originTablePrimary,\n" +
            "               originTableFields\n" +
            "          from toTableName\n" +
            "          toTableFilter\n" +
            "       )verify\n" +
            "on base.compareKey=verify.compareKey\n" +
            "   and if(base.originTablePrimary is null, '-', base.originTablePrimary) = if(verify.originTablePrimary is null, '-', verify.originTablePrimary)\n" +
            " where \n" +
            "\tfields_list_check_filter\n"
            + "union\n"
            + "select base.originTablePrimary as base_originTablePrimary,\n" +
            "\t   fields_list_check\n" +
            "  from (\n" +
            "        select 'num' as compareKey,\n" +
            "               originTablePrimary,\n" +
            "               originTableFields\n" +
            "          from originTableName\n" +
            "          originTableFilter\n" +
            "       )base\n" +
            "  right join (\n" +
            "        select 'num' as compareKey,\n" +
            "originTablePrimary,\n" +
            "               originTableFields\n" +
            "          from toTableName\n" +
            "          toTableFilter\n" +
            "       )verify\n" +
            "on base.compareKey=verify.compareKey\n" +
            "   and if(base.originTablePrimary is null, '-', base.originTablePrimary) = if(verify.originTablePrimary is null, '-', verify.originTablePrimary)\n" +
            " where \n" +
            "\tfields_list_check_filter\n";

    public static final String COLUMN = "if(column is null, '-', column)";

    public static final String fields_list_check = "base.column as base_column,\n" +
            "       verify.column as verify_column,\n" +
            "       case when if(base.column is null, '-',base.column) = if(verify.column is null, '-', verify.column) then '一致'\n" +
            "            else '不一致'\n" +
            "             end as column_is_pass";

    public static final String fields_list_check_filter = "if(base.column is null, '-', base.column) <> if(verify.column is null, '-', verify.column)";

    public static Instance run(Dbconfig dbconfig, Jobconfig jobconfig) throws Exception {

        DbTypeEnum dbTypeEnum = DbTypeEnum.findEnumByType(dbconfig.getType());

        String compareNumSql = String.format(PV_UV_SQL, jobconfig.getOriginTablePrimary(), jobconfig.getOriginTableName(), Optional.ofNullable(jobconfig.getOriginTableFilter()).orElse("")
                , jobconfig.getToTablePrimary(), jobconfig.getToTableName(), Optional.ofNullable(jobconfig.getToTableFilter()).orElse(""));

        log.info("====compareNumSql====");
        log.info(compareNumSql);
        log.info("====compareNumSql====");

        String[] fileds = jobconfig.getOriginTableFields().split(",");
        List<String> originColumns = new ArrayList<>();
        originColumns.add(COLUMN.replace("column", jobconfig.getOriginTablePrimary()));
        for (int i = 0; i < fileds.length; i++) {
            originColumns.add(COLUMN.replace("column", fileds[i]));
        }
        List<String> toColumns = new ArrayList<>();
        toColumns.add(COLUMN.replace("column", jobconfig.getToTablePrimary()));
        for (int i = 0; i < fileds.length; i++) {
            toColumns.add(COLUMN.replace("column", fileds[i]));
        }
        String consistencyNumSql = String.format(CONSISTENCY_SQL,
                String.join(",", originColumns),
                jobconfig.getOriginTableName(),
                Optional.ofNullable(jobconfig.getOriginTableFilter()).orElse(""),

                String.join(",", toColumns),
                jobconfig.getToTableName(),
                Optional.ofNullable(jobconfig.getToTableFilter()).orElse(""),

                String.join(",", originColumns),
                jobconfig.getOriginTableName(),
                Optional.ofNullable(jobconfig.getOriginTableFilter()).orElse(""),

                String.join(",", toColumns),
                jobconfig.getToTableName(),
                Optional.ofNullable(jobconfig.getToTableFilter()).orElse(""));

        log.info("====consistencyNumSql====");
        log.info(consistencyNumSql);
        log.info("====consistencyNumSql====");

        Instance instance = runNum(dbconfig, dbTypeEnum.getConnectDriver(), compareNumSql, consistencyNumSql);
        instance.setMagnitudeSql(compareNumSql);
        instance.setConsistencySql(consistencyNumSql);
        return instance;
    }

    public static List<LinkedHashMap<String, String>> runDiffDetail(Dbconfig dbconfig, Jobconfig jobconfig) throws Exception {
        String[] fileds = jobconfig.getOriginTableFields().split(",");
        List<String> fields_list = new ArrayList<>();
        List<String> fields_list_filter = new ArrayList<>();
        for (int i = 0; i < fileds.length; i++) {
            fields_list.add(fields_list_check.replaceAll("column", fileds[i]));
            fields_list_filter.add(fields_list_check_filter.replaceAll("column", fileds[i]));
        }
        String check_sql = CHECK_SQL.replaceAll("originTablePrimary", jobconfig.getOriginTablePrimary())
                .replaceAll("originTableFields", jobconfig.getOriginTableFields())
                .replaceAll("originTableName", jobconfig.getOriginTableName())
                .replaceAll("toTableName", jobconfig.getToTableName())
                .replaceAll("originTableFilter", Optional.ofNullable(jobconfig.getOriginTableFilter()).orElse(""))
                .replaceAll("toTableFilter", Optional.ofNullable(jobconfig.getToTableFilter()).orElse(""))
                .replaceAll("fields_list_check_filter", String.join(" or ", fields_list_filter))
                .replaceAll("fields_list_check", String.join(",", fields_list));

        log.info("====check_sql====");
        log.info(check_sql);
        log.info("====check_sql====");

        DbTypeEnum dbTypeEnum = DbTypeEnum.findEnumByType(dbconfig.getType());
        try {
            Class.forName(dbTypeEnum.getConnectDriver());
        } catch (ClassNotFoundException e) {
            throw new Exception("注册驱动失败");
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbconfig.getUrl(), dbconfig.getUserName(), dbconfig.getPwd());
            Statement stat = conn.createStatement();
            ResultSet re = stat.executeQuery(check_sql);
            ResultSetMetaData metaData = re.getMetaData();  //获取列集
            int columnCount = metaData.getColumnCount(); //获取列的数量
            List<LinkedHashMap<String, String>> list = new ArrayList<>();
            while (re.next()) {
                LinkedHashMap<String, String> hashMap = new LinkedHashMap();
                for (int i = 0; i < columnCount; i++) { //循环列
                    String columnName = metaData.getColumnName(i + 1); //通过序号获取列名,起始值为1
                    String columnValue = re.getString(columnName);  //通过列名获取值.如果列值为空,columnValue为null,不是字符型
                    hashMap.put(columnName, columnValue);
                }
                list.add(hashMap);
            }
            re.close();
            stat.close();
            conn.close();
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("连接数据库失败");
        }

    }

    //量级对比
    public static Instance runNum(Dbconfig dbconfig, String connectDriver, String compareSql, String consistencyNumSql) throws Exception {
        Instance instance = new Instance();
        try {
            Class.forName(connectDriver);
        } catch (ClassNotFoundException e) {
            throw new Exception("注册驱动失败");
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbconfig.getUrl(), dbconfig.getUserName(), dbconfig.getPwd());
            Statement stat = conn.createStatement();
            ResultSet re = stat.executeQuery(compareSql);
            while (re.next()) {
                instance.setOriginTablePv(re.getString(1));
                instance.setToTablePv(re.getString(2));
                instance.setPvDiff(re.getString(3));
                instance.setOriginTableUv(re.getString(4));
                instance.setToTableUv(re.getString(5));
                instance.setUvDiff(re.getString(6));
            }
            re.close();
            re = stat.executeQuery(consistencyNumSql);
            while (re.next()) {
                instance.setOriginTableCount(re.getString(2));
                instance.setToTableCount(re.getString(3));
                instance.setCountDiff(re.getString(4));
            }
            re.close();
            stat.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("连接数据库失败");
        }
        return instance;
    }
}
