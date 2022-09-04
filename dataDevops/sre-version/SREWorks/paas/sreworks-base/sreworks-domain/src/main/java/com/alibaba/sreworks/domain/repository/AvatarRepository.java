package com.alibaba.sreworks.domain.repository;


import com.alibaba.sreworks.domain.DO.Avatar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author jinghua.yjh
 */
public interface AvatarRepository extends JpaRepository<Avatar, Long>, JpaSpecificationExecutor<Avatar> {

    Avatar findFirstById(Long id);

}
