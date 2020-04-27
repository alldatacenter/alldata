package com.platform.devops.autogen;

import com.platform.devops.autogen.ColumnImpl.*;
import com.platform.devops.autogen.ColumnImpl.*;
import org.apache.commons.lang.StringUtils;

/**
 * Created by wulinhao on 2020/04/13.
 */
public abstract class TableColumn {
    public TableInfo tableInfo;
    public String name;
    public String line;

    public TableColumn(String name, String line) {
        this.name = name;
        this.line = line;
    }
    public String getName(){
        return name;
    }
    public abstract String getColumnType();

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public String getLowerName() {
        return TmpUtils.underlineToLowerUpperFirst(name);
    }

    public String getUpperName() {
        return TmpUtils.underlineToUpperFirst(name);
    }

    public abstract String getScalaType();

    public String getScalaSqlDefine() {
        return getScalaDefine();
    }

    public abstract String getScalaDefine();

    public abstract String getScalaParams();

    public abstract String getScalaGR();

    public abstract String getScalaFieldDefine();


    static public TableColumn parse(String input) throws Exception {
        String line = input.trim();
        String[] temp = line.split(" ");
        String name = StringUtils.strip(temp[0], "`");
        String sqlType = StringUtils.strip(temp[1].toUpperCase(), ",");
        boolean notNull = line.toUpperCase().contains(" NOT NULL");
        String defaultV = null;
        if (line.toUpperCase().contains(" DEFAULT ")) {
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].toUpperCase().equals("DEFAULT")) {
                    defaultV = temp[i + 1];
                }
            }
        }
        defaultV = StringUtils.strip(defaultV, "\"',");
        if (line.contains("AUTO_INCREMENT")) {
            return new AutoIntColumn(name, line);
        } else if (name.endsWith("_json")) {
            return new JsonColumn(name, line);
        }
        if (sqlType.equals("TIMESTAMP")) {
            return new TimestampColumn(name, line, notNull, defaultV);
        } else if (sqlType.startsWith("VARCHAR")) {
            int length = Integer.valueOf(TmpUtils.findMatch("\\d+", sqlType));
            return new VarcharColumn(name, line, notNull, defaultV, length);
        } else if (sqlType.equals("TEXT")) {
            return new TextColumn(name, line, notNull);
        } else if (sqlType.startsWith("INT")) {
            int length = Integer.valueOf(TmpUtils.findMatch("\\d+", sqlType));
            return new IntColumn(name, line, notNull, defaultV, length);
        }
        throw new Exception("find unknow line " + line);
    }
}
