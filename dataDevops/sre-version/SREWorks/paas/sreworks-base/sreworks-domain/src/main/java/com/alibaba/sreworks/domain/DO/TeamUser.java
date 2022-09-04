package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DTO.TeamUserRole;

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
public class TeamUser {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column
    private Long gmtAccess;

    @Column
    private Long teamId;

    @Column
    private String user;

    @Column
    private String role;

    @Column
    private String creator;

    @Column
    private String lastModifier;

    @Column
    private Integer isConcern;

    public TeamUserRole role() {
        return TeamUserRole.valueOf(getRole());
    }

    public TeamUser(Long teamId, String user) {
        this.gmtCreate = System.currentTimeMillis() / 1000;
        this.gmtModified = System.currentTimeMillis() / 1000;
        this.gmtAccess = System.currentTimeMillis() / 1000;
        this.teamId = teamId;
        this.user = user;
        this.creator = user;
        this.lastModifier = user;
        this.isConcern = 0;
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

}