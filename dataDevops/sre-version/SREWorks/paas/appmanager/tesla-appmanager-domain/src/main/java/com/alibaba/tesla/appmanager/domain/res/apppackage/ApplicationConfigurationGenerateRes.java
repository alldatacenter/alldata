package com.alibaba.tesla.appmanager.domain.res.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ApplicationConfiguration 配置创建结果对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationConfigurationGenerateRes implements Serializable {

    private static final long serialVersionUID = 3749993152344486728L;

    private String yaml;
}
