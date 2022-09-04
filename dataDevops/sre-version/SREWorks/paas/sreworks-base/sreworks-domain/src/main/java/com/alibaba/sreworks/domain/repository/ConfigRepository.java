package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.sreworks.domain.DO.Config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;

/**
 * @author jinghua.yjh
 */
public interface ConfigRepository extends JpaRepository<Config, Long>, JpaSpecificationExecutor<Config> {

    Config findFirstByName(String name);

    List<Config> findAllByNameLike(String name);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    Integer deleteByName(String name);

}
