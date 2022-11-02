package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DTO.AppComponentDetail;
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
public class App {

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

    @Column(unique = true)
    private String name;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "longtext")
    private String annotations;

    @Column(columnDefinition = "longtext")
    private String labels;

    @Column
    private Long display;

    public AppDetail detail() {
        AppDetail detail = JSONObject.parseObject(this.detail, AppDetail.class);
        return detail == null ? new AppDetail() : detail;
    }

    public JSONObject annotations() {
        return JSONObject.parseObject(annotations);
    }

    public JSONObject labels() {
        return JSONObject.parseObject(labels);
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

}
