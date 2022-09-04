package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.AppInstance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface AppInstanceRepository extends JpaRepository<AppInstance, Long>, JpaSpecificationExecutor<AppInstance> {

    AppInstance findFirstById(Long id);

    List<AppInstance> findAllByStatusNotIn(List<String> statusList);

    @Query(value = ""
        + "select app_instance.*, "
        + "     app.name as app_name, "
        + "     app.labels as app_labels, "
        + "     cluster.name as cluster_name, "
        + "     team.name as team_name "
        + "from app_instance app_instance "
        + "left join app app on app_instance.app_id = app.id "
        + "left join cluster cluster on app_instance.cluster_id = cluster.id "
        + "left join team team on app_instance.team_id = team.id "
        + "left join team_user tu on tu.team_id = team.id "
        + "where tu.user = ?1 and app.name like ?2 "
        + "order by app_instance.id desc  "
        , nativeQuery = true)
    List<JSONObject> findObjectByUser(String user, String name);

    @Query(value = ""
        + "select app_instance.*, app.name as app_name, cluster.name as cluster_name, team.name as team_name "
        + "from app_instance app_instance "
        + "left join app app on app_instance.app_id = app.id "
        + "left join cluster cluster on app_instance.cluster_id = cluster.id "
        + "left join team team on app_instance.team_id = team.id "
        + "where app.name like ?1 and team.visible_scope = 'PUBLIC' "
        + "order by app_instance.id desc  "
        , nativeQuery = true)
    List<JSONObject> findPublicObject(String name);

    @Query(value = ""
        + "select app_instance.*, app.name as app_name, cluster.name as cluster_name, team.name as team_name "
        + "from app_instance app_instance "
        + "left join app app on app_instance.app_id = app.id "
        + "left join cluster cluster on app_instance.cluster_id = cluster.id "
        + "left join team team on app_instance.team_id = team.id "
        + "where app_instance.id = ?1 "
        , nativeQuery = true)
    JSONObject getObjectById(Long id);

    int countByAppId(Long appId);
}
