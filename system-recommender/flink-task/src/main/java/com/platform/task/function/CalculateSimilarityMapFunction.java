package com.platform.schedule.function;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.types.Row;


public class CalculateSimilarityMapFunction extends ScalarFunction {
   public Row eval(String product1, String product2, Long cocount, Integer count1, Integer count2) {
       Double coSim = cocount/Math.sqrt(count1 * count2);
       return Row.of(product1, product2, coSim);
   }

    @Override
    public TypeInformation<?> getResultType(Class<?>[] signature) {
        return Types.ROW(Types.STRING, Types.STRING, Types.DOUBLE);
    }
}
