package com.hw.lineage.flink.table.udf.functionb;

import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.annotation.FunctionHint;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.types.Row;

/**
 * @description: FlinkSplitFunction
 * @author: HamaWhite
 * @version: 1.0.0
 */
@FunctionHint(output = @DataTypeHint("ROW<word STRING, length INT>"))
public class FlinkSplitFunction extends TableFunction<Row> {

    public void eval(String str) {
        for (String s : str.split(" ")) {
            // use collect(...) to emit a row
            collect(Row.of(s, s.length()));
        }
    }
}

