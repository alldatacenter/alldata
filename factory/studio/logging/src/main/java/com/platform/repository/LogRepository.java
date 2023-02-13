
package com.platform.repository;

import com.platform.domain.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@Repository
public interface LogRepository extends JpaRepository<Log,Long>, JpaSpecificationExecutor<Log> {

    /**
     * 根据日志类型删除信息
     * @param logType 日志类型
     */
    @Modifying
    @Query(value = "delete from sys_log where log_type = ?1", nativeQuery = true)
    void deleteByLogType(String logType);
}
