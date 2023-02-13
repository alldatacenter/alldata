package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.domain.DTO.AppComponentInstanceDetail;
import com.alibaba.sreworks.domain.DTO.AppComponentType;
import com.alibaba.sreworks.domain.utils.AppUtil;

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
public class AppComponentInstance {

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
    private Long appInstanceId;

    @Column
    private Long appComponentId;

    @Column
    private String type;

    @Column
    private String name;

    @Column(columnDefinition = "text")
    private String detail;

    public AppComponentInstance(AppInstance appInstance, AppComponent appComponent) {
        this.gmtCreate = System.currentTimeMillis() / 1000;
        this.gmtModified = System.currentTimeMillis() / 1000;
        this.creator = appInstance.getCreator();
        this.lastModifier = appInstance.getCreator();
        this.appInstanceId = appInstance.getId();
        this.appComponentId = appComponent.getId();
        this.name = appComponent.getName();
        this.detail = appComponent.getDetail();
    }

    public AppComponentType type() {
        return AppComponentType.valueOf(type);
    }

    public AppComponentInstanceDetail detail() {
        AppComponentInstanceDetail detail = JSONObject.parseObject(this.detail, AppComponentInstanceDetail.class);
        return detail == null ? new AppComponentInstanceDetail() : detail;
    }

    public String microserviceName(AppInstance appInstance) {
        return appInstance.getStageId() + "-" + AppUtil.appmanagerId(appInstance.getAppId()) + "-" + name;
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

    public AppComponentInstance setMetricOn(Boolean metricOn, String user) {
        AppComponentInstanceDetail detail = detail();
        detail.setMetricOn(metricOn);
        setDetail(JSONObject.toJSONString(detail));
        setGmtModified(System.currentTimeMillis() / 1000);
        setLastModifier(user);
        return this;
    }

}
