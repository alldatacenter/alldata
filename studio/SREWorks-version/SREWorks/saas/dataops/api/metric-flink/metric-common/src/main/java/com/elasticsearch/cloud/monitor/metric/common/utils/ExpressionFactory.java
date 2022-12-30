package com.elasticsearch.cloud.monitor.metric.common.utils;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by colin on 2017/5/23.
 */
public class ExpressionFactory {

    private static JexlEngine engine = new JexlBuilder().create();

    private static Map<String, JexlExpression> cache = new HashMap<>();

    public static JexlExpression getExpression(String str){
        JexlExpression  expression = cache.get(str);
        if(expression == null){
            expression = engine.createExpression(str);
            cache.put(str, expression);
        }
        return expression;
    }
}
