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
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"stageId", "name"})})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductopsElement {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String elementId;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private String lastModifier;

    @Column
    private String stageId;

    @Column
    private String name;

    @Column
    private String version;

    @Column
    private String type;

    @Column
    private String appId;

    @Column(columnDefinition = "longtext")
    private String config;

    @Column
    private Integer isImport;

    public JSONObject toJSONObject() {
        JSONObject ret = JSONObject.parseObject(JSONObject.toJSONString(this));
        ret.put("config", JSONObject.parse(config));
        return ret;
    }

}
