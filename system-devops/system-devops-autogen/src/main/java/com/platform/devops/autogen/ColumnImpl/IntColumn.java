package com.platform.devops.autogen.ColumnImpl;

import com.platform.devops.autogen.TableColumn;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class IntColumn extends TableColumn {
    boolean notNull;
    String defaultV;
    int length;


    public IntColumn(String name, String line, boolean notNull, String defaultV, int length) throws Exception {
        super(name, line);
        this.notNull = notNull;
        this.defaultV = defaultV;
        this.length = length;
        if (!this.notNull) {
            throw new Exception("int must not null");
        }
    }

    @Override
    public String getColumnType() {
        return "IntColumn";
    }

    @Override
    public String getScalaParams() {
        return getLowerName() + ": Int";
    }

    @Override
    public String getScalaType() {
        return "Int";
    }


    @Override
    public String getScalaSqlDefine() {
        if (defaultV != null) {
            return String.format("%s: Int = %s", getLowerName(), defaultV);
        } else return String.format("%s: Int", getLowerName(), defaultV);
    }

    @Override
    public String getScalaDefine() {
        if (this.notNull) {
            if (defaultV != null) {
                return String.format("%s: Int = %s", getLowerName(), defaultV);
            } else return String.format("%s: Int", getLowerName(), defaultV);
        } else {
            if (defaultV != null) return String.format("Option[Int] = Some(%s)", defaultV);
            else return "Option[Int]";
        }
    }

    @Override
    public String getScalaGR() {
        return "<<[Int]";
    }

    @Override
    public String getScalaFieldDefine() {
        return String.format("val %s: Rep[Int] = column[Int](\"%s\")",
                getLowerName(), name, length);
    }
}
