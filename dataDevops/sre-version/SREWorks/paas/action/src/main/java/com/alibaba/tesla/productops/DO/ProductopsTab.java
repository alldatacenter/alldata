package com.alibaba.tesla.productops.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"stageId", "tabId"})})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductopsTab {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String tabId;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private String lastModifier;

    @Column
    private String stageId;

    @Column
    private String appId;

    @Column(length = 1024)
    private String nodeTypePath;

    @Column(columnDefinition = "longtext")
    private String elements;

    @Column(length = 1024)
    private String label;

    @Column(columnDefinition = "longtext")
    private String config;

    @Column(length = 1024)
    private String name;

    @Column
    private Integer isImport;

    public JSONObject toJSONObject() {
        JSONObject ret = JSONObject.parseObject(JSONObject.toJSONString(this));
        ret.put("elements", JSONObject.parse(elements));
        ret.put("config", JSONObject.parse(config));
        ret.put("id", tabId);
        return ret;
    }

}
