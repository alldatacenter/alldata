package com.platform.devops.autogen.ColumnImpl;

import com.platform.devops.autogen.TableColumn;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class TextColumn extends TableColumn {
    boolean notNull;

    public TextColumn(String name, String line, boolean notNull) throws Exception {
        super(name, line);
        this.notNull = notNull;
    }

    @Override
    public String getColumnType() {
        return "TextColumn";
    }
    @Override
    public String getScalaType() {
        if (this.notNull) return "String";
        else return "Option[String]";
    }

    @Override
    public String getScalaDefine() {
        if (this.notNull) return String.format("%s:String", getLowerName());
        else return String.format("%s: Option[String] = None", getLowerName());
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
            return String.format("val %s: Rep[String] = column[String](\"%s\")", getLowerName(), name);
        } else {
            return String.format("val %s: Rep[Option[String]] = column[Option[String]](\"%s\", O.Default(None))",
                    getLowerName(), name);
        }
    }

    @Override
    public String getScalaParams() {
        return null;
    }

}
