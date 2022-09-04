package com.alibaba.sreworks.domain.repository;

import java.util.List;

import javax.transaction.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface AppComponentInstanceRepository
    extends JpaRepository<AppComponentInstance, Long>, JpaSpecificationExecutor<AppComponentInstance> {

    AppComponentInstance findFirstById(Long id);

    List<AppComponentInstance> findAllByAppInstanceId(Long appInstanceId);

    @Transactional(rollbackOn = Exception.class)
    @Modifying
    void deleteAllByAppInstanceId(Long appInstanceId);

    @Query(value = ""
        + "select app_component_instance.*, team.name as team_name, app.name as app_name from app_component_instance "
        + "left join app_instance on app_component_instance.app_instance_id = app_instance.id "
        + "left join team on app_instance.team_id = team.id "
        + "left join app on app.id = app_instance.app_id "
        + "where app_instance.cluster_id = ?1 "
        , nativeQuery = true)
    List<JSONObject> findObjectByClusterId(Long clusterId);

}
