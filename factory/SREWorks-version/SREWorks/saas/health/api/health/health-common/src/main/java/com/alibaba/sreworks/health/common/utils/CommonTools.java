package com.alibaba.sreworks.health.common.utils;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用工具类
 *@author fangzong.lyj@alibaba-inc.com
 *@date 2021/12/29 20:35
 */
public class CommonTools {

    /**
     * 解析分步下标
     * @param size 元素总数量
     * @param step 步长
     * @return
     */
    public static List<IndexStepRange> generateStepIndex(int size, int step) {
        if (size <= 0 || step <= 0) {
            return null;
        }

        List<IndexStepRange> result = new ArrayList<>();
        int headIndex = 0;
        while (headIndex < size) {
            int tailIndex = headIndex + step;
            if (tailIndex >= size) {
                tailIndex = size;
            }
            IndexStepRange indexStepRange = IndexStepRange.builder().startIndex(headIndex).endIndex(tailIndex).build();
            result.add(indexStepRange);
            headIndex = tailIndex;
        }
        return result;
    }

    @Builder
    @Getter
    static
    public class IndexStepRange {
        /**
         * 开始索引
         */
        private int startIndex;

        /**
         * 结束索引
         */
        private int endIndex;
    }
}
