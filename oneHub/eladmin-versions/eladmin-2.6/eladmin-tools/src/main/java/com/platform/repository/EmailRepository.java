
package com.platform.repository;

import com.platform.domain.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author AllDataDC
 * @date 2022-10-27
 */
public interface EmailRepository extends JpaRepository<EmailConfig,Long> {
}
