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
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"stageId", "nodeTypePath"})})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductopsNode {

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

    @Column
    private String appId;

    @Column(length = 1024)
    private String category;

    @Column(length = 1024)
    private String parentNodeTypePath;

    @Column(length = 1024)
    private String serviceType;

    @Column(length = 1024)
    private String nodeTypePath;

    @Column(length = 1024)
    private String version;

    @Column(columnDefinition = "longtext")
    private String config;

    @Column
    private Integer isImport;

    public Long order() {
        JSONObject config = JSONObject.parseObject(this.config);
        return config.getLongValue("order");
    }

    public JSONObject toJsonObject(boolean isRootNode, int level) {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(this));
        jsonObject.put("config", JSONObject.parseObject(config));
        jsonObject.put("children", new JSONArray());
        jsonObject.put("isRootNode", isRootNode);
        jsonObject.put("level", level);
        return jsonObject;
    }

}
