
package com.platform.repository;

import com.platform.domain.GenConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
public interface GenConfigRepository extends JpaRepository<GenConfig,Long> {

    /**
     * 查询表配置
     * @param tableName 表名
     * @return /
     */
    GenConfig findByTableName(String tableName);
}
