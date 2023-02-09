package com.qcloud.cos.model.ciModel.auditing;

import java.util.HashMap;
import java.util.Map;

/**
 * 审核结果汇总
 */
public class AuditingResult {
    /**
     * 是否命中,命中多个取最高值
     * 0表示未命中，1表示命中，2表示疑似
     */
    private Integer hitFlag = 0;

    /**
     * 违规类型
     * key为 命中审核类型 value为审核的结果分数 分数越高表示越敏感
     * 相同类型取最大值
     */
    private Map<String, Integer> hitMap;

    public Integer getHitFlag() {
        return hitFlag;
    }

    public void setHitFlag(int hitFlag) {
        this.hitFlag = hitFlag;
    }

    public Map<String, Integer> getHitMap() {
        if (hitMap == null) {
            hitMap = new HashMap<>();
        }
        return hitMap;
    }

    public void setHitMap(Map<String, Integer> hitMap) {
        this.hitMap = hitMap;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AuditingResult{");
        sb.append("hitFlag=").append(hitFlag);
        sb.append(", hitMap=").append(hitMap);
        sb.append('}');
        return sb.toString();
    }
}
