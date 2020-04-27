package com.platform.devops.autogen.ColumnImpl;

import com.platform.devops.autogen.TableColumn;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class TimestampColumn extends TableColumn {
    boolean notNull;
    String defaultV;

    public TimestampColumn(String name, String line, boolean notNull, String defaultV) throws Exception {
        super(name, line);
        this.notNull = notNull;
        this.defaultV = defaultV;
        if (!this.notNull) {
            throw new Exception("not allow null timestamp");
        }
    }

    @Override
    public String getColumnType() {
        return "TimestampColumn";
    }
    @Override
    public String getScalaType() {
        if (notNull) return "java.sql.Timestamp";
        else return "Option[java.sql.Timestamp]";
    }

    @Override
    public String getScalaDefine() {
        if (notNull) return getLowerName() + ": java.sql.Timestamp";
        else return getLowerName() + ": Option[java.sql.Timestamp]";
    }

    @Override
    public String getScalaGR() {
        if (notNull) return "<<[java.sql.Timestamp]";
        else return "<<?[java.sql.Timestamp]";
    }

    @Override
    public String getScalaFieldDefine() {
        return "val " + getLowerName() + ": Rep[java.sql.Timestamp] = column[java.sql.Timestamp](\"" + name + "\")";
    }

    @Override
    public String getScalaParams() {
        return null;
    }

}
