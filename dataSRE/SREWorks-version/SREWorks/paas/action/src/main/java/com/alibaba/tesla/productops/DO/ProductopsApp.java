package com.alibaba.tesla.productops.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Data
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"stageId", "appId"})})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductopsApp {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private String lastModifier;

    @Column
    private String templateName;

    @Column
    private String stageId;

    @Column
    private String appId;

    @Column
    private String environments;

    @Column
    private String version;

    @Column(columnDefinition = "longtext")
    private String config;

    public JSONObject toJsonObject() {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(this));
        jsonObject.put("config", JSONObject.parseObject(config));
        jsonObject.put("environments", JSONObject.parseArray(environments));
        return jsonObject;
    }

}
