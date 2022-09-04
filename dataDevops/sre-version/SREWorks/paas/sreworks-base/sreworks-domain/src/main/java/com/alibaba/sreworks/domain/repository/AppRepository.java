package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.App;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface AppRepository extends JpaRepository<App, Long>, JpaSpecificationExecutor<App> {

    App findFirstById(Long id);

    @Query(value = ""
        + "select app.*, team.name as team_name "
        + "from app app "
        + "left join team team on app.team_id = team.id "
        + "left join team_user tu on tu.team_id = team.id "
        + "where tu.user = ?1 and app.name like ?2 and app.display = 1 "
        + "order by app.id desc "
        , nativeQuery = true)
    List<JSONObject> findObjectByUser(String user, String name);

    @Query(value = ""
        + "select app.*, team.name as team_name "
        + "from app app "
        + "left join team team on app.team_id = team.id "
        + "left join team_user tu on tu.team_id = team.id "
        + "order by app.id desc "
        , nativeQuery = true)
    List<JSONObject> findObject();

    List<App> findAllByTeamIdAndDisplay(Long teamId, Long display);

}
