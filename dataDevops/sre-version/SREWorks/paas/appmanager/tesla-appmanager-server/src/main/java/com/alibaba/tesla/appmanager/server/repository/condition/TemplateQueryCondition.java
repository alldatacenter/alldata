package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 模板查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQueryCondition implements Serializable {

    private static final long serialVersionUID = 3732123533749808811L;

    private String templateId;

    private String templateType;

    private String templateName;
}
