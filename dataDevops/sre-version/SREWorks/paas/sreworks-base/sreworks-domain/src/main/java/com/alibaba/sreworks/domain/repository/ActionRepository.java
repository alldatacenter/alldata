package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.sreworks.domain.DO.Action;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author jinghua.yjh
 */
public interface ActionRepository extends JpaRepository<Action, Long>, JpaSpecificationExecutor<Action> {

    Action findFirstById(Long id);

    List<Action> findAllByTargetTypeAndTargetValueOrderByIdDesc(String targetType, String targetValue);

}
