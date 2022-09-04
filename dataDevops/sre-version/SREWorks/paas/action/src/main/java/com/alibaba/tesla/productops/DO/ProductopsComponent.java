package com.alibaba.tesla.productops.DO;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

/**
 * @author jiongen.zje
 */
@Slf4j
@Data
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"stageId", "componentId"})})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductopsComponent {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String componentId;

    @Column(columnDefinition = "longtext")
    private String config;

    @Column(columnDefinition = "longtext")
    private String interfaces;

    @Column(length = 1024)
    private String name;

    @Column(length = 1024)
    private String alias;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private String lastModifier;

    @Column
    private String stageId;

    @Column
    private Integer isImport;

    public JSONObject toJSONObject() {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(this));
        jsonObject.put("configObject", JSONObject.parseObject(this.config));
        jsonObject.put("interfaceObject", JSONObject.parseObject(this.interfaces));
        return jsonObject;
    }

}
