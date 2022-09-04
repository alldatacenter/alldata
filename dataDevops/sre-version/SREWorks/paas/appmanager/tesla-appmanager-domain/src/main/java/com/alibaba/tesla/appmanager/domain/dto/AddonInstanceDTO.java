package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonInstanceDTO implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String addonId;

    private String addonInstanceId;

    private String addonVersion;

    private String namespaceId;

    private JSONObject resourceExt;
}
