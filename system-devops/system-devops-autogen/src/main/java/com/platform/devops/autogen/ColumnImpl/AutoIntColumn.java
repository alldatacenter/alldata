package com.platform.devops.autogen.ColumnImpl;

import com.platform.devops.autogen.TableColumn;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class AutoIntColumn extends TableColumn {
    public AutoIntColumn(String name, String line) {
        super(name, line);
    }

    @Override
    public String getColumnType() {
        return "AutoIntColumn";
    }

    @Override
    public String getScalaType() {
        return "Int";
    }

    @Override
    public String getScalaDefine() {
        return getLowerName() + ": Int";
    }

    @Override
    public String getScalaParams() {
        return getLowerName() + ": Int";
    }

    @Override
    public String getScalaGR() {
        return "<<[Int]";
    }

    @Override
    public String getScalaFieldDefine() {
        return "val " + getLowerName() + ": Rep[Int] = column[Int](\"" + getLowerName() + "\", O.AutoInc, O.PrimaryKey)";
    }

}
