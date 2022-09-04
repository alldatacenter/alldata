package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.ClusterResource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface ClusterResourceRepository
    extends JpaRepository<ClusterResource, Long>, JpaSpecificationExecutor<ClusterResource> {

    ClusterResource findFirstById(Long id);

    List<ClusterResource> findAllByClusterId(Long clusterId);

    @Query(value = ""
        + "select cr.*, cluster.name as cluster_name, ta.name as account_name , team.name as team_name "
        + "from cluster_resource cr "
        + "left join cluster cluster on cr.cluster_id = cluster.id "
        + "left join team_account ta on cr.account_id = ta.id "
        + "left join team team on ta.team_id = team.id "
        + "where cr.id = ?1 "
        , nativeQuery = true)
    JSONObject findObjectById(Long id);

    List<ClusterResource> findAllByIdIn(List<Long> idList);

    @Query(value = ""
        + "select cr.*, ta.type as account_type "
        + "from cluster_resource cr left join team_account ta "
        + "on cr.account_id = ta.id "
        + "where cr.cluster_id = ?1 "
        , nativeQuery = true)
    List<JSONObject> findObjectByClusterId(Long clusterId);

}
