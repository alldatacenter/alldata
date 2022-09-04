package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询指定 component package 任务运行状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskQueryReq implements Serializable {

    private static final long serialVersionUID = -2387163567502877778L;

    private Long componentPackageTaskId;
}
