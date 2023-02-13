
package com.platform.repository;

import com.platform.domain.AlipayConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
public interface AliPayRepository extends JpaRepository<AlipayConfig,Long> {
}
