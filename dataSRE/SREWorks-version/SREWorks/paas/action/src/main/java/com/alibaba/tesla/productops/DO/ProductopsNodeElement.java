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
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"stageId", "elementId"})})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductopsNodeElement {

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
    private String stageId;

    @Column(length = 1024)
    private String nodeTypePath;

    @Column(unique = true)
    private String elementId;

    @Column(length = 1024)
    private String nodeName;

    @Column
    private String type;

    @Column
    private String appId;

    @Column
    private Long nodeOrder;

    @Column
    private String tags;

    @Column(columnDefinition = "longtext")
    private String config;

    @Column
    private Integer isImport;

    public JSONObject toJSONObject() {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(this));
        jsonObject.put("name", nodeName);
        jsonObject.put("order", nodeOrder);
        return jsonObject;
    }

}
