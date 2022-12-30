package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DTO.AppComponentAppPackageDetail;
import com.alibaba.sreworks.domain.DTO.AppComponentDetail;
import com.alibaba.sreworks.domain.DTO.AppComponentHelmDetail;
import com.alibaba.sreworks.domain.DTO.AppComponentRepoDetail;
import com.alibaba.sreworks.domain.DTO.AppComponentType;

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
public class AppComponent {

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
    private Long appId;

    @Column
    private String type;

    @Column
    private String name;

    @Column
    private Long exId;

    @Column(columnDefinition = "text")
    private String typeDetail;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(columnDefinition = "text")
    private String description;

    public AppComponentType type() {
        return AppComponentType.valueOf(type);
    }

    public AppComponentRepoDetail repoDetail() {
        AppComponentRepoDetail repoDetail = JSONObject.parseObject(this.typeDetail, AppComponentRepoDetail.class);
        return repoDetail == null ? new AppComponentRepoDetail() : repoDetail;
    }

    public AppComponentHelmDetail helmDetail() {
        AppComponentHelmDetail helmDetail = JSONObject.parseObject(this.typeDetail, AppComponentHelmDetail.class);
        return helmDetail == null ? new AppComponentHelmDetail() : helmDetail;
    }

    public AppComponentAppPackageDetail appPackageDetail() {
        AppComponentAppPackageDetail appPackageDetail = JSONObject.parseObject(
            this.typeDetail, AppComponentAppPackageDetail.class);
        return appPackageDetail == null ? new AppComponentAppPackageDetail() : appPackageDetail;
    }

    public AppComponentDetail detail() {
        AppComponentDetail detail = JSONObject.parseObject(this.detail, AppComponentDetail.class);
        return detail == null ? new AppComponentDetail() : detail;
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

}
