package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 命名空间 DTO
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceDTO implements Serializable {
    private static final long serialVersionUID = 5045682904318757048L;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Namespace 名称
     */
    private String namespaceName;

    /**
     * 创建者
     */
    private String namespaceCreator;

    /**
     * 修改者
     */
    private String namespaceModifier;

    /**
     * 扩展信息
     */
    private JSONObject namespaceExt;
}
