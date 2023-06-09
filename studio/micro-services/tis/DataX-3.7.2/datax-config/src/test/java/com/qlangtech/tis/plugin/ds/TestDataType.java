package com.qlangtech.tis.plugin.ds;

import junit.framework.TestCase;

import java.sql.Types;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-18 20:22
 **/
public class TestDataType extends TestCase {

    public void testDataTypeSerialize() {
        int decimalDigits = 4;
        int columnSize = 20;
        DataType dt = new DataType(Types.BINARY, "binary", columnSize);
        dt.setDecimalDigits(decimalDigits);

        DataType actual = DataType.ds(dt.getS());

        assertEquals(dt.type, actual.type);
        assertEquals(columnSize, actual.columnSize);
        assertEquals(decimalDigits, (int) actual.getDecimalDigits());

        dt.setDecimalDigits(null);
        actual = DataType.ds(dt.getS());

        assertEquals(dt.type, actual.type);
        assertEquals(columnSize, actual.columnSize);
        assertNull("getDecimalDigits must be null", actual.getDecimalDigits());
    }
}
