package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Action {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private String operator;

    @Column
    private String targetType;

    @Column
    private String targetValue;

    @Column
    private String content;

    public Action(String operator, String targetType, String content) {
        this.gmtCreate = System.currentTimeMillis() / 1000;
        this.gmtModified = System.currentTimeMillis() / 1000;
        this.operator = operator;
        this.targetType = targetType;
        this.content = content;
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

}
