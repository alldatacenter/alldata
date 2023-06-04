package org.dromara.cloudeon.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;


@Entity
@Table(name = "ce_session")
@Data
public class SessionEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    /**
     *
     */
    private Integer userId;
    /**
     *
     */
    private String ip;
    /**
     *
     */
    private Date lastLoginTime;

}
