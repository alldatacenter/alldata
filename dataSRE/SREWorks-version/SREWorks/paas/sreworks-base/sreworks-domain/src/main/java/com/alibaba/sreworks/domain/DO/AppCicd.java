package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
public class AppCicd {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private String creator;

    @Column
    private String lastModifier;

    @Column
    private Long teamId;

    @Column
    private String name;

    @Column
    private String authToken;

    @Column
    private String timerTrigger;

    @Column
    private String scmTrigger;

    @Column(columnDefinition = "longtext")
    private String script;

    @Column(columnDefinition = "longtext")
    private String description;

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

}