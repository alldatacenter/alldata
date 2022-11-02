package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 组件包 DTO
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageDTO implements Serializable {

    private static final long serialVersionUID = 7449690812224502681L;

    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件类型下的唯一组件标识
     */
    private String componentName;

    /**
     * 版本号
     */
    private String packageVersion;

    /**
     * 存储位置相对路径
     */
    private String packagePath;

    /**
     * 创建者
     */
    private String packageCreator;

    /**
     * 包 MD5
     */
    private String packageMd5;

    /**
     * 包 Addon 描述信息
     */
    private JSONObject packageAddon;

    /**
     * 包配置选项信息
     */
    private JSONObject packageOptions;

    /**
     * 扩展信息 JSON
     */
    private String packageExt;
}
