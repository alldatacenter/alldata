package com.alibaba.sreworks.job.master.domain.DO;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

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
public class SreworksJobTask {

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
    private String operator;

    @Column
    private String appId;

    @Column
    private String name;

    @Column
    private String alias;

    @Column
    private Long execTimeout;

    @Column
    private String execType;

    @Column(columnDefinition = "longtext")
    private String execContent;

    @Column
    private Long execRetryTimes;

    @Column
    private Long execRetryInterval;

    @Column(columnDefinition = "longtext")
    private String varConf;

    @Column
    private String sceneType;

    @Column(columnDefinition = "longtext")
    private String sceneConf;

    public Long execRetryTimes() {
        return execRetryTimes == null ? 0 : execRetryTimes;
    }

    public Long execRetryInterval() {
        return execRetryInterval == null ? 0 : execRetryInterval;
    }

    public JSONObject varConf() {
        return StringUtil.isEmpty(varConf) ? new JSONObject() : JSONObject.parseObject(varConf);
    }

    public JSONObject sceneConf() {
        return StringUtil.isEmpty(sceneConf) ? new JSONObject() : JSONObject.parseObject(sceneConf);
    }

}
