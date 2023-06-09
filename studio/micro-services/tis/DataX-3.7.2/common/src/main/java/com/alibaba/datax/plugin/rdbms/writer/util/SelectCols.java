package com.alibaba.datax.plugin.rdbms.writer.util;

import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.common.util.ListUtil;
import com.alibaba.datax.plugin.rdbms.writer.Key;
import com.qlangtech.tis.plugin.ds.IDBReservedKeys;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-30 14:23
 **/
public class SelectCols extends EscapeableEntity implements Iterable<String> {
    protected final List<String> columns;

    @Override
    public Iterator<String> iterator() {
        return this.columns.iterator();
    }

//    public static SelectCols createSelectCols(Configuration conf) {
//        return createSelectCols(conf, null);
//    }

    public static SelectCols createSelectCols(Configuration conf, IDBReservedKeys escapeChar) {
        return new SelectCols(
                conf.getList(Key.COLUMN, String.class), escapeChar);
    }

    public static SelectCols createSelectCols(List<String> allColumns) {
        return new SelectCols(allColumns, null);
    }

    private SelectCols(List<String> columns, IDBReservedKeys escapeChar) {
        super(escapeChar);
        this.columns = columns.stream().map((c) -> unescapeEntity(c)).collect(Collectors.toList());
        if (this.columns == null || this.columns.isEmpty()) {
            throw new IllegalArgumentException("param colums can not be empty ");
        }
    }

    public boolean isSelectAllCols() {
        return 1 == this.size() && "*".equals(this.columns.get(0));
    }

    public void makeSureNoValueDuplicate(
            boolean caseSensitive) {
//        if (null == aList || aList.isEmpty()) {
//            throw new IllegalArgumentException("您提供的作业配置有误, List不能为空.");
//        }

        if (1 == this.columns.size()) {
            return;
        } else {
            List<String> list = null;
            if (!caseSensitive) {
                list = ListUtil.valueToLowerCase(this.columns);
            } else {
                list = new ArrayList<String>(this.columns);
            }

            Collections.sort(list);

            for (int i = 0, len = list.size() - 1; i < len; i++) {
                if (list.get(i).equals(list.get(i + 1))) {
                    throw DataXException
                            .asDataXException(
                                    CommonErrorCode.CONFIG_ERROR,
                                    String.format(
                                            "您提供的作业配置信息有误, String:[%s] 不允许重复出现在列表中: [%s].",
                                            list.get(i),
                                            StringUtils.join(this.columns, ",")));
                }
            }
        }
    }

    public String getCols() {
        return columns.stream().map((c) -> {
            return escapeEntity(c);
        }).collect(Collectors.joining(","));
    }

    /**
     * 只适用于MySQL
     *
     * @return
     */
    public String onDuplicateKeyUpdateString() {
        if (columns == null || columns.size() < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" ON DUPLICATE KEY UPDATE ");
        boolean first = true;
        for (String column : columns) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(column);
            sb.append("=VALUES(");
            sb.append(column);
            sb.append(")");
        }

        return sb.toString();
    }

    public int size() {
        return columns.size();
    }

    public boolean containsCol(String name) {
        return columns.contains(name);
    }

}
