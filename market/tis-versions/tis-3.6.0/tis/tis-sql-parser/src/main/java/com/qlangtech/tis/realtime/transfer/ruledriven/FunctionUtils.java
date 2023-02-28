/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.realtime.transfer.ruledriven;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.qlangtech.tis.realtime.transfer.IRowValueGetter;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class FunctionUtils {

    // 流计算调用的函数算子
    // ================================================================================
    // return op_and(r.getColumn("draw_status"),8)

    /**
     * Returns the string or bytes resulting from concatenating the strings or bytes passed in as parameters in order. For example, concat('foo', 'bar') results in 'foobar'. Note that this function can take any number of input strings.
     * @param a
     * @param b
     * @return
     */
    public static String concat(String a, String b) {
        return StringUtils.join(new Object[]{a, b});
    }

    /**
     * Returns the position of the first occurrence of substr in str. Returns null if either of the arguments are null and returns 0 if substr could not be found in str. Be aware that this is not zero based. The first character in str has index 1.
     *
     * @param str
     * @param substr
     * @return
     */
    public static int instr(String str, String substr) {
        return StringUtils.indexOf(str, substr);
    }

    public static int op_and(String val, int andVal) {
        return op_and(Integer.parseInt(val), andVal);
    }

    public static int op_and(int val, int andVal) {
        return (val) & andVal;
    }

    public static List<String> split(String val, String pattern) {
        List<String> result = Lists.newArrayList();
        try {
            Pattern p = patternCache.get(pattern);
            Matcher matcher = p.matcher(val);
            int current = 0;
            int start = 0;
            while (matcher.find()) {
                start = matcher.start();
                if (start > current) {
                    result.add(StringUtils.substring(val, current, start));
                }
                // System.out.println("start:" + matcher.start() + ",end:" + matcher.end() +
                // ",group:" + matcher.group());
                current = matcher.end();
            }
            if (current < val.length()) {
                result.add(StringUtils.substring(val, current));
            }
            return result;
        } catch (ExecutionException e) {
            throw new RuntimeException("val:" + val + ",pattern:" + pattern, e);
        }
    }

    public static String getArrayIndexProp(List<String> array, int index) {
        if (index < 0 || index >= array.size()) {
            return StringUtils.EMPTY;
        }
        return array.get(index);
    }

    public static void main(String[] args) {
        List<String> split = split("20:32|已婚男士|15253653555|8f931168c2b44f7bb05e77e27286ce0c", "\\|");
        System.out.println("xxxx:" + getArrayIndexProp(split, 3));
        // Method[] methods = FunctionUtils.class.getMethods();
        // final Set<String> mnames = Sets.newHashSet();
        // for (Method m : methods) {// Modifier.STATIC
        //
        // if ((m.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) ==
        // (Modifier.STATIC | Modifier.PUBLIC)) {
        //
        // if (mnames.add(m.getName())) {
        // System.out.println(
        // "import static com.qlangtech.tis.realtime.transfer.core.FunctionUtils." +
        // m.getName() + ";");
        // }
        // }
        // }
        //
        // Class<?>[] clazzs = FunctionUtils.class.getDeclaredClasses();
        //
        // for (Class clazz : clazzs) {
        //
        // System.out.println("import " + StringUtils.replace(clazz.getName(), "$", ".")
        // + ";");
        //
        // }
        // min(GroupValues groupVales, IValGetter valGetter)
        Method method = FunctionUtils.getMethod("min", GroupValues.class, IValGetter.class);
        System.out.println(method);
        System.out.println(method.getReturnType());
        method = FunctionUtils.getMethod("concat_ws", String.class, Object[].class);
        //
        System.out.println(method);
        method = FunctionUtils.getMethod("round", double.class, int.class);
        System.out.println(method);
        // for (Method m : FunctionUtils.class.getMethods()) {
        // System.out.println(m);
        // }
        //
        // System.out.println(method.getReturnType());
    }

    public static List<Object> collect_list(GroupValues groupValues, IValGetter valGetter) {
        return groupValues.vals.stream().map((d) -> valGetter.getVal(d)).collect(Collectors.toList());
    }

    public static Set<Object> collect_set(GroupValues groupValues, IValGetter valGetter) {
        return groupValues.vals.stream().map((r) -> valGetter.getVal(r)).collect(Collectors.toSet());
    }

    public static int count(GroupValues groupVales, IIntValGetter getter) {
        if (groupVales == null || groupVales.vals == null) {
            return 0;
        }
        return groupVales.vals.size();
    }

    // 默认的比对方式
    private static final Comparator<Object> rowValueGetterComparator = new Comparator<Object>() {

        @Override
        public int compare(Object o1, Object o2) {
            return ((Comparable) o1).compareTo(o2);// - o2.hashCode();
        }
    };

    public static Object min(GroupValues groupVales, IValGetter valGetter) {
        Optional<Object> min = groupVales.vals.stream().map((r) -> valGetter.getVal(r)).min(rowValueGetterComparator);
        if (!min.isPresent()) {
            throw new IllegalStateException("datas can not find minist elements");
        }
        return min.get();
    }

    public static Object max(GroupValues groupVales, IValGetter valGetter) {

        Optional<Object> max = groupVales.vals.stream().map((r) -> valGetter.getVal(r)).max(rowValueGetterComparator);
        if (!max.isPresent()) {
            throw new IllegalStateException("datas can not find maxist elements");
        }
        return max.get();
    }

    /**
     * 四舍五入保留scale位小数
     *
     * @param input
     * @param scale
     * @return
     */
    public static double round(double input, int scale) {
        if (Double.isNaN(input) || Double.isInfinite(input)) {
            return input;
        }
        return BigDecimal.valueOf(input).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static double sum(GroupValues datas, IDoubleValGetter valGetter) {
        double sum = 0;
        for (IRowValueGetter g : datas.vals) {
            sum += valGetter.getVal(g);
        }
        return sum;
    }

    public static interface IValGetter {

        Object getVal(IRowValueGetter g);
    }

    public static interface IStrValGetter {

        String getVal(IRowValueGetter g);
    }

    public static interface IDoubleValGetter {

        double getVal(IRowValueGetter g);
    }

    public static interface IIntValGetter {

        int getVal(IRowValueGetter g);
    }

    // 自定义的udf
    public static Object defaultVal(Object... o) {
        for (int i = 0; i < o.length; i++) {
            if (o[i] != null) {
                return o[i];
            }
        }
        return null;
    }

    // jsonPropPattern
    private static final Pattern jsonPropPattern = Pattern.compile("\\$\\.([^\\s]+?)");

    public static String get_json_object(String val, String pattern) {
        if (!StringUtils.startsWith(val, "{")) {
            return null;
        }
        try {
            Matcher matcher = jsonPropPattern.matcher(pattern);
            if (!matcher.matches()) {
                throw new IllegalStateException("json prop pattern:" + pattern + " is not match pattern:" + jsonPropPattern);
            }
            JSONTokener tokener = new JSONTokener(val);
            JSONObject o = new JSONObject(tokener);
            String propKey = matcher.group(1);
            if (o.isNull(propKey)) {
                return null;
            }
            return o.getString(propKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static double defaultDoubleVal(Object... o) {
        for (int i = 0; i < o.length; i++) {
            if (o[i] != null) {
                if (o[i] instanceof String) {
                    return Double.parseDouble((String) o[i]);
                } else {
                    return (double) o[i];
                }
            }
        }
        return 0d;
    }

    public static String concat_ws(String separator, Set<Object> valList) {
        return concat_ws_collection(separator, valList);
    }

    public static String concat_ws(String separator, List<Object> valList) {
        return concat_ws_collection(separator, valList);
    }

    private static String concat_ws_collection(String separator, Collection<Object> valList) {
        return valList.stream().map((r) -> String.valueOf(r)).collect(Collectors.joining(separator));
    }

    public static String concat_ws(String separator, Object... objs) {
        List<Object> vals = new ArrayList<>();
        for (Object o : objs) {
            if (o instanceof Collection) {
                // for (Object e : )) {
                // vals.add()
                // }
                vals.addAll((Collection) o);
            } else {
                vals.add(o);
            }
        }
        return concat_ws(separator, vals);
    }

    public static class Case {

        private final Callable<Boolean> matchIf;

        private final Object dftVal;

        public Case(Callable<Boolean> matchIf, Object dftVal) {
            super();
            this.matchIf = matchIf;
            this.dftVal = dftVal;
        }

        public boolean matchIf() {
            try {
                return matchIf.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Object caseIfFunc(Object defaultVal, Case... cases) {
        for (Case c : cases) {
            if (c.matchIf()) {
                return c.dftVal;
            }
        }
        return defaultVal;
    }

    /**
     * 类型转换
     *
     * @param type
     * @param val
     * @return
     */
    @SuppressWarnings("all")
    public static <T> T typeCast(String type, Object val) {
        // FunctionVisitor.castToType.get(type);
        TypeCast<T> c = (TypeCast<T>) TypeCast.getTypeCast(type);
        if (c == null) {
            throw new IllegalStateException("type:" + type + " relevant TypeCast can not be null");
        }
        return c.cast(val);
    }

    private static final LoadingCache<String, Pattern> patternCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Pattern>() {

        @Override
        public Pattern load(String pattern) throws Exception {
            return Pattern.compile(pattern);
        }
    });

    // regexp(cd.id,'^E_')
    public static boolean rlike(String val, String pattern) {
        return regexp(val, pattern);
    }

    public static boolean regexp(String val, String pattern) {
        try {
            Pattern p = patternCache.get(pattern);
            Matcher m = p.matcher(val);
            return m.find();
        } catch (ExecutionException e) {
            throw new RuntimeException("val:" + val + ",pattern:" + pattern, e);
        }
    }

    // ================================================================================
    public static Method getMethod(String name, Class<?>... parameterTypes) {
        try {
            return FunctionUtils.class.getMethod(name, parameterTypes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
