package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.Cluster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface ClusterRepository extends JpaRepository<Cluster, Long>, JpaSpecificationExecutor<Cluster> {

    List<Cluster> findAllByTeamId(Long teamId);

    Cluster findFirstById(Long id);

    @Query(value = ""
        + "SELECT "
        + "    cluster.*, "
        + "    team.name AS team_name, "
        + "    ta.name AS account_name,  "
        + "    ta.type AS account_type  "
        + "FROM "
        + "    cluster cluster "
        + "LEFT JOIN team "
        + "ON cluster.team_id = team.id "
        + "LEFT JOIN team_account ta "
        + "ON cluster.account_id = ta.id  "
        + "WHERE cluster.id = ?1 "
        , nativeQuery = true)
    JSONObject findObjectById(Long id);

    @Query(value = ""
        + "SELECT "
        + "    cluster.*, "
        + "    tu.name AS team_name, "
        + "    ta.name AS account_name,  "
        + "    ta.type AS account_type  "
        + "FROM "
        + "    cluster cluster "
        + "LEFT JOIN "
        + "    (SELECT team.* FROM team team JOIN team_user tu ON team.id = tu.team_id) tu "
        + "ON cluster.team_id = tu.id  "
        + "LEFT JOIN team_account ta "
        + "ON cluster.account_id = ta.id  "
        , nativeQuery = true)
    List<JSONObject> findObject();

    @Query(value = ""
        + "SELECT "
        + "    cluster.*, "
        + "    tu.name AS team_name, "
        + "    ta.name AS account_name,  "
        + "    ta.type AS account_type  "
        + "FROM "
        + "    cluster cluster "
        + "LEFT JOIN "
        + "    (SELECT team.* FROM team team JOIN team_user tu ON team.id = tu.team_id WHERE tu.USER = ?1 ) tu "
        + "ON cluster.team_id = tu.id  "
        + "LEFT JOIN team_account ta "
        + "ON cluster.account_id = ta.id  "
        + "WHERE tu.NAME IS NOT NULL AND cluster.name LIKE ?2 "
        , nativeQuery = true)
    List<JSONObject> findObjectByUserAndNameLike(String user, String name);

    @Query(value = ""
        + "SELECT "
        + "    cluster.*, "
        + "    team.NAME AS team_name, "
        + "    ta.type AS account_type "
        + "FROM cluster cluster "
        + "LEFT JOIN team team "
        + "ON cluster.team_id = team.id "
        + "LEFT JOIN team_account ta "
        + "ON cluster.account_id = ta.id  "
        + "WHERE team.visible_scope = 'PUBLIC' AND cluster.NAME LIKE ?1 "
        , nativeQuery = true)
    List<JSONObject> findObjectByVisibleScopeIsPublicAndNameLike(String name);
}
