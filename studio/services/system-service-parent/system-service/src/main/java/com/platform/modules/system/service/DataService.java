
package com.platform.modules.system.service;

import com.platform.modules.system.service.dto.UserDto;
import java.util.List;

/**
 * 数据权限服务类
 * @author AllDataDC
 * @date 2023-01-27 
 */
public interface DataService {

    /**
     * 获取数据权限
     * @param user /
     * @return /
     */
    List<Long> getDeptIds(UserDto user);
}
