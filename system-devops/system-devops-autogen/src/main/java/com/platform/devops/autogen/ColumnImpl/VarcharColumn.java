package com.platform.devops.autogen.ColumnImpl;

import com.platform.devops.autogen.TableColumn;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class VarcharColumn extends TableColumn {
    boolean notNull;
    String defaultV;
    int length;

    public VarcharColumn(String name, String line, boolean notNull, String defaultV, int length) throws Exception {
        super(name, line);
        this.notNull = notNull;
        this.defaultV = defaultV;
        this.length = length;
        if (!this.notNull) {
            throw new Exception("not allow null varchar must set default value");
        }
    }

    @Override
    public String getColumnType() {
        return "VarcharColumn";
    }

    @Override
    public String getScalaType() {
        if (this.notNull) return "String";
        else return "Option[String]";
    }

    @Override
    public String getScalaDefine() {
        if (this.notNull) {
            if (defaultV != null) {
                return String.format("%s: String = \"%s\"", getLowerName(), defaultV);
            } else return String.format("%s: String", getLowerName());
        } else {
            if (defaultV != null) return String.format("Option[String] = Some(\"%s\")", defaultV);
            else return "Option[String]";
        }
    }

    @Override
    public String getScalaGR() {
        if (this.notNull) {
            return "<<[String]";
        } else {
            return "<<?[String]";
        }
    }

    @Override
    public String getScalaFieldDefine() {
        if (this.notNull) {
            if (defaultV != null) {
                return String.format("val %s: Rep[String] = column[String](\"%s\", O.Length(%d, varying = true), O.Default(\"%s\"))",
                        getLowerName(), name, length, defaultV);
            } else {
                return String.format("val %s: Rep[String] = column[String](\"%s\", O.Length(%d, varying = true))",
                        getLowerName(), name, length);
            }
        } else {
            return null;
        }
    }

    @Override
    public String getScalaParams() {
        return getLowerName() + ": String";
    }

}
