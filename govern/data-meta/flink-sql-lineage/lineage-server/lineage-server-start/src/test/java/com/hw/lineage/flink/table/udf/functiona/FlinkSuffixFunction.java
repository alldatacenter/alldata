package com.hw.lineage.flink.table.udf.functiona;

import org.apache.flink.table.functions.ScalarFunction;

/**
 * @description: FlinkSuffixFunction
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class FlinkSuffixFunction extends ScalarFunction {

    public String eval(String input) {
        return input.concat("-HamaWhite");
    }

}