package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 尝试对指定的任务进行重试
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskRetryReq implements Serializable {

    private static final long serialVersionUID = -2785644449900501800L;

    private Long componentPackageTaskId;
}
