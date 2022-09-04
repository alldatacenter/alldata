package com.alibaba.tesla.appmanager.domain.req.namespace;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 命名空间创建请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceCreateReq implements Serializable {

    private static final long serialVersionUID = 3297427928679216048L;

    /**
     * Namespace ID
     */
    @NotBlank
    private String namespaceId;

    /**
     * Namespace 名称
     */
    @NotBlank
    private String namespaceName;

    /**
     * 扩展信息
     */
    private JSONObject namespaceExt;
}
