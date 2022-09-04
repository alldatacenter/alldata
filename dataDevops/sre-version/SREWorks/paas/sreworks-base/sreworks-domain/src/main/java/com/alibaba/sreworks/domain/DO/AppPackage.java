package com.alibaba.sreworks.domain.DO;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DTO.AppDetail;

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
public class AppPackage {

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

    @Column(columnDefinition = "longtext")
    private String app;

    @Column(columnDefinition = "longtext")
    private String appComponentList;

    @Column
    private Long appPackageTaskId;

    @Column
    private String simpleVersion;

    @Column
    private String version;

    @Column
    private String status;

    @Column
    private Long appPackageId;

    @Column
    private Integer onSale;

    @Column(columnDefinition = "text")
    private String description;

    public Boolean onSale() {
        return onSale != null && onSale > 0;
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

    public App app() {
        return JSONObject.parseObject(app, App.class);
    }

    public List<AppComponent> appComponentList() {
        return JSONObject.parseArray(appComponentList).toJavaList(AppComponent.class);
    }

    public AppPackage(String operator, App app, List<AppComponent> appComponentList,
        Long appPackageTaskId, String simpleVersion) {

        this.gmtCreate = System.currentTimeMillis() / 1000;
        this.gmtModified = System.currentTimeMillis() / 1000;
        this.creator = operator;
        this.lastModifier = operator;
        this.appId = app.getId();
        this.app = JSONObject.toJSONString(app);
        this.appComponentList = JSONObject.toJSONString(appComponentList);
        this.appPackageTaskId = appPackageTaskId;
        this.simpleVersion = simpleVersion;
    }

}
