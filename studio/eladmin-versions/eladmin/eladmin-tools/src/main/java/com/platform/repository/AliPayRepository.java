
package com.platform.repository;

import com.platform.domain.AlipayConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author AllDataDC
 * @date 2022-10-27
 */
public interface AliPayRepository extends JpaRepository<AlipayConfig,Long> {
}
