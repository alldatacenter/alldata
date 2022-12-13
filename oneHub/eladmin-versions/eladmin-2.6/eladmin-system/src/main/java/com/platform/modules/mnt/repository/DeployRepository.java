
package com.platform.modules.mnt.repository;

import com.platform.modules.mnt.domain.Deploy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
* @author zhanghouying
* @date 2022-10-27
*/
public interface DeployRepository extends JpaRepository<Deploy, Long>, JpaSpecificationExecutor<Deploy> {
}
