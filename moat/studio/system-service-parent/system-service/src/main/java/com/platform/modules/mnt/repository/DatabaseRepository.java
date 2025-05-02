
package com.platform.modules.mnt.repository;

import com.platform.modules.mnt.domain.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
* @author AllDataDC
* @date 2023-01-27
*/
public interface DatabaseRepository extends JpaRepository<Database, String>, JpaSpecificationExecutor<Database> {
}
