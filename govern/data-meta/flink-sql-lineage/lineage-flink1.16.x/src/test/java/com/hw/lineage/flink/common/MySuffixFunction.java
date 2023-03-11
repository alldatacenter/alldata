package com.hw.lineage.flink.common;

import org.apache.flink.table.functions.ScalarFunction;

/**
 * @description: MySuffixFunction
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class MySuffixFunction extends ScalarFunction {

    public String eval(String input) {
        return input.concat("-HamaWhite");
    }

}
