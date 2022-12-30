package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.YamlUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class Cluster {

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
    private Long accountId;

    @Column
    private String clusterName;

    @Column
    private String name;

    @Column(columnDefinition = "longtext")
    private String kubeconfig;

    @Column(columnDefinition = "longtext")
    private String description;

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

    public JSONObject kubeJsonObject() throws JsonProcessingException {
        return YamlUtil.toJsonObject(kubeconfig);
    }

}
