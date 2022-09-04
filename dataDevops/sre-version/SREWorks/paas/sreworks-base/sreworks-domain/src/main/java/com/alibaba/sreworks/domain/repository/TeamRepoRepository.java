package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface TeamRepoRepository extends JpaRepository<TeamRepo, Long>, JpaSpecificationExecutor<TeamRepo> {

    TeamRepo findFirstById(Long id);

    List<TeamRepo> findAllByTeamId(Long teamId);

    @Query(value = ""
        + "select tr.*, team.name as team_name "
        + "from team_repo tr "
        + "left join team team on tr.team_id = team.id "
        + "left join team_user tu on tu.team_id = team.id "
        + "where tu.user = ?1 and tr.name like ?2 "
        + "order by tr.id desc "
        , nativeQuery = true)
    List<JSONObject> findObjectByUser(String user, String name);

}
