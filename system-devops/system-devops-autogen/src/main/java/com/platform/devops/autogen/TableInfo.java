package com.platform.devops.autogen;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wulinhao on 2020/04/12.
 */
public class TableInfo {
    public String name;
    public String[] pkNames;
    public List<UKIndex> ukIndex = new ArrayList<>();
    public List<TableColumn> allColumns = new ArrayList<>();
    public List<TableColumn> pkColumns = new ArrayList<>();
    public List<TableColumn> otherColumns = new ArrayList<>();


    static public class UKIndex {
        public String name;
        public String[] cols;


        static Pattern r = Pattern.compile("UNIQE KEY `(.*)` \\((.*)\\)");

        static public UKIndex parse(String line) {
            UKIndex ukIndex = new UKIndex();
            Matcher m = r.matcher(line);
            if (m.find()) {
                ukIndex.name = m.group(0);
                ukIndex.cols = findCols(m.group(1));
            }
            return ukIndex;
        }
    }


    static String[] findCols(String lines) {
        System.out.println(lines);
        String[] ns = lines.split(",");
        String[] re = new String[ns.length];
        for (int j = 0; j < ns.length; j++) {
            re[j] = StringUtils.strip(ns[j], "`");
        }
        return re;
    }

    static TableInfo parse(String tableName, String createTableLine) throws Exception {
        TableInfo t = new TableInfo();
        t.name = tableName;
        String[] lines = createTableLine.split("\n");
        for (int i = 1; i < lines.length - 1; i++) {
            String l = lines[i].trim();
            System.out.println(l);
            if (l.startsWith("PRIMARY KEY")) {
                String names = StringUtils.strip(TmpUtils.findMatch("\\(.*\\)", l), "()");
                t.pkNames = findCols(names);
            } else if (l.startsWith("UNIQUE KEY")) {
                t.ukIndex.add(UKIndex.parse(l));
            } else if (l.startsWith("`")) {
                TableColumn one = TableColumn.parse(l);
                one.setTableInfo(t);
                t.allColumns.add(one);
            }
        }
        //检查字段顺序 必须是  pk1,pk2,create_time,update_time,c1,c2....
        for (int i = 0; i < t.pkNames.length; i++) {
            if (!t.pkNames[i].equals(t.allColumns.get(i).name)) {
                throw new Exception("primary key must use at first");
            }
        }

        if (!t.allColumns.get(t.pkNames.length).name.equals("create_time")) {
            throw new Exception("create_time update_time must follow the pks");
        }
        if (!t.allColumns.get(t.pkNames.length + 1).name.equals("update_time")) {
            throw new Exception("create_time update_time must follow the pks");
        }

        for (TableColumn o : t.allColumns) {
            if (ArrayUtils.contains(t.pkNames, o.name)) {
                t.pkColumns.add(o);
            } else if (o.name.equals("create_time") || o.name.equals("update_time")) {
                //skip
            } else {
                t.otherColumns.add(o);
            }
        }
        return t;
    }

    //use for build vm

    public String getLowerTable() {
        return TmpUtils.underlineToLowerUpperFirst(name);
    }

    public String getUpperTable() {
        return TmpUtils.underlineToUpperFirst(name);
    }

    public List<String> getNames() {
        List<String> re = new ArrayList<>();
        for (TableColumn one : allColumns) {
            re.add(TmpUtils.underlineToLowerUpperFirst(one.name));
        }
        return re;
    }

    public List<TableColumn> getJsonColumns() {
        List<TableColumn> re = new ArrayList<>();
        for (TableColumn one : allColumns) {
            if (one.name.endsWith("_json")) {
                re.add(one);
            }
        }
        return re;
    }

    public List<TableColumn> getPkColumns() {
        return pkColumns;
    }

    public List<TableColumn> getNoPkColumns() {
        return otherColumns;
    }

    public List<TableColumn> getAllColumns() {
        return allColumns;
    }


    public String getPkJoinNames() {
        String[] x = new String[pkNames.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = TmpUtils.underlineToUpperFirst(pkNames[i]);
        }
        String pkJoinNames = StringUtils.join(x, "And");
        return pkJoinNames;
    }

    public boolean isAutoTable() {
        TableColumn col = pkColumns.get(0);
        return col.getColumnType().equals("AutoIntColumn");
    }

    public TableColumn getFirstPkColumn() {
        return pkColumns.get(0);
    }
}
