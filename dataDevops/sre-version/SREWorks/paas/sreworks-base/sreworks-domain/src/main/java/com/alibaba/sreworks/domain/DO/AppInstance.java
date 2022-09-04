package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DTO.AppInstanceDetail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

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
public class AppInstance {

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
    private String name;

    @Column
    private Long teamId;

    @Column
    private Long clusterId;

    @Column
    private Long appId;

    @Column
    private Long appPackageId;

    @Column
    private String appDeployId;

    @Column
    private String stageId;

    @Column
    private String status;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "longtext")
    private String ac;

    public AppInstanceDetail detail() {
        AppInstanceDetail detail = JSONObject.parseObject(this.detail, AppInstanceDetail.class);
        return detail == null ? new AppInstanceDetail() : detail;
    }

    public String namespace() {
        return StringUtils.isEmpty(name) ? String.format("%s-%s", getStageId(), getId()) : name;
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

}
