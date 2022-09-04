package com.alibaba.sreworks.job.master.domain.repository;

import com.alibaba.sreworks.job.master.domain.DO.SreworksConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author jinghua.yjh
 */
public interface SreworksConfigRepository
    extends JpaRepository<SreworksConfig, Long>, JpaSpecificationExecutor<SreworksConfig> {

    SreworksConfig findFirstByName(String name);

    int deleteAllByName(String name);

}
