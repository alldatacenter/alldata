package com.alibaba.sreworks.domain.DO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
public class Avatar {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Long gmtCreate;

    @Column
    private Long gmtModified;

    @Column(columnDefinition = "longblob")
    private String content;

    public Avatar(String content) {
        this.gmtCreate = System.currentTimeMillis() / 1000;
        this.gmtModified = System.currentTimeMillis() / 1000;
        this.content = content;
    }

}