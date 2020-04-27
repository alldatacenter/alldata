package com.platform.devops.autogen.ColumnImpl;

import com.platform.devops.autogen.TableColumn;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class JsonColumn extends TableColumn {
    public JsonColumn(String name, String line) {
        super(name, line);
    }

    @Override
    public String getColumnType() {
        return "JsonColumn";
    }
    @Override
    public String getScalaType() {
        return "Option[String]";
    }

    @Override
    public String getScalaSqlDefine() {
        return String.format("%s: Option[String] = None", getLowerName());
    }

    @Override
    public String getScalaDefine() {
        return String.format("%s: %s%s = %s%s()", getLowerName(), tableInfo.getUpperTable(), getUpperName(),
                tableInfo.getUpperTable(), getUpperName());
    }

    @Override
    public String getScalaParams() {
        return null;
    }

    @Override
    public String getScalaGR() {
        return "<<?[String]";
    }

    @Override
    public String getScalaFieldDefine() {
        return String.format("val %s: Rep[Option[String]] = column[Option[String]](\"%s\", O.Default(None))",
                getLowerName(), name);
    }

}
