package com.alibaba.tesla.appmanager.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 部署工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DeploymentUtil {

    /**
     * 将指定的 list 切分为指定的 parts 数目的子 list
     *
     * @param list        列表
     * @param targetParts 目标份数
     * @return list of list
     */
    public static <T> List<List<T>> chopIntoParts(final List<T> list, final int targetParts) {
        final List<List<T>> parts = new ArrayList<>();
        final int chunkSize = list.size() / targetParts;
        int leftOver = list.size() % targetParts;
        int take = chunkSize;
        for (int i = 0, iT = list.size(); i < iT; i += take) {
            if (leftOver > 0) {
                leftOver--;
                take = chunkSize + 1;
            } else {
                take = chunkSize;
            }
            parts.add(new ArrayList<>(list.subList(i, Math.min(iT, i + take))));
        }
        return parts;
    }
}
