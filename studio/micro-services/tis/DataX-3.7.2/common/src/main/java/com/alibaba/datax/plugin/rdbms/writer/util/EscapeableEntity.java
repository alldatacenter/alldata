package com.alibaba.datax.plugin.rdbms.writer.util;

import com.qlangtech.tis.plugin.ds.IDBReservedKeys;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-03 10:38
 **/
public class EscapeableEntity {
    protected final IDBReservedKeys escapeChar;
    protected final boolean containEscapeChar;

    public EscapeableEntity(IDBReservedKeys escapeChar) {
        this.escapeChar = escapeChar;
        this.containEscapeChar = escapeChar.getEscapeChar().isPresent();
    }

    public String getEscapeChar() {
        return escapeChar.getEscapeChar().get();
    }

    public boolean isContainEscapeChar() {
        return containEscapeChar;
    }

    protected String escapeEntity(String val) {
        return escapeChar.getEscapedEntity(val);
    }

    protected String unescapeEntity(String val) {
//        if (containEscapeChar) {
//            return StringUtils.remove(val, this.escapeChar);
//        } else {
//            return val;
//        }
        return escapeChar.removeEscapeChar(val);
    }
}
