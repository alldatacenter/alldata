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
public class ClusterResource {

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
    private Long clusterId;

    @Column
    private Long accountId;

    @Column
    private String type;

    @Column
    private String instanceName;

    @Column
    private String name;

    @Column(columnDefinition = "longtext")
    private String usageDetail;

    @Column(columnDefinition = "longtext")
    private String description;

    public JSONObject usageDetail() {
        JSONObject ud = JSONObject.parseObject(usageDetail);
        return ud == null ? new JSONObject() : ud;
    }

}
