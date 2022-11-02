package com.alibaba.tesla.appmanager.domain.res.componentpackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建 Component Package 响应
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageCreateRes implements Serializable {

    private static final long serialVersionUID = -7831078152057141339L;

    /**
     * Component Package 创建任务 ID
     */
    private Long componentPackageTaskId;
}
