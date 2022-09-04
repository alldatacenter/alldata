package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppMicroServiceDTO implements Serializable {
    private static final long serialVersionUID = -7004327153275993431L;

    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String appId;

    private String gitRepo;

    private String comment;

    private String type;

    private List<String> tags;
}
