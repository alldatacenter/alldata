package com.alibaba.tesla.appmanager.domain.res.appmeta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 获取应用版本返回
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppGetVersionRes implements Serializable {

    /**
     * 应用实际版本
     */
    private String version;
}
