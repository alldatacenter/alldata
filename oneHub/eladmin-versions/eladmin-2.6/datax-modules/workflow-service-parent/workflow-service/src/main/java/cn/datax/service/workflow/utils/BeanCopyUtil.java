package cn.datax.service.workflow.utils;

import cn.datax.service.workflow.utils.functional.BeanCopyUtilCallBack;
import cn.hutool.core.bean.BeanUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * BeanUtil  扩展
 */
public class BeanCopyUtil extends BeanUtil {
    /**
     * 集合数据的拷贝
     * @param sources: 数据源类
     * @param target: 目标类::new(eg: UserVO::new)
     * @return
     */
    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target) {
        return copyListProperties(sources, target, null, null);
    }
    /**
     * 集合数据的拷贝
     * @param sources: 数据源类
     * @param target: 目标类::new(eg: UserVO::new)
     * @param ignoreProperties 忽略属性
     * @return
     */
    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target, String... ignoreProperties) {
        return copyListProperties(sources, target, null, ignoreProperties);
    }

    /**
     * 带回调函数的集合数据的拷贝（可自定义字段拷贝规则）
     * @param sources: 数据源类
     * @param target: 目标类::new(eg: UserVO::new)
     * @param callBack: 回调函数 属性字段要借助回调函数拷贝
     * @param ignoreProperties 忽略属性
     * @return
     */
    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target, BeanCopyUtilCallBack<S, T> callBack, String... ignoreProperties) {
        List<T> list = new ArrayList<>(sources.size());
        for (S source : sources) {
            T t = target.get();
            copyProperties(source, t, ignoreProperties);
            list.add(t);
            if (callBack != null) {
                // 回调
                callBack.callBack(source, t);
            }
        }
        return list;
    }
}
