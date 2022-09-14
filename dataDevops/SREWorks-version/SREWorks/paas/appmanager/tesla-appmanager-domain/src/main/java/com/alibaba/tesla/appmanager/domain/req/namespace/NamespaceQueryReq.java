package com.alibaba.tesla.appmanager.domain.req.namespace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 命名空间查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceQueryReq implements Serializable {

    private static final long serialVersionUID = -1872143932301240289L;

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
     * 每页大小
     */
    @Min(1)
    @Max(100)
    private Integer pageSize;

    /**
     * 当前页数
     */
    @Min(1)
    private Integer pageNumber;
}
