package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.AppCicd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface AppCicdRepository extends JpaRepository<AppCicd, Long>, JpaSpecificationExecutor<AppCicd> {

    AppCicd findFirstById(Long id);

    @Query(value = ""
        + "select ac.*, team.name as team_name, tu.user as user "
        + "from app_cicd ac "
        + "left join team team on ac.team_id = team.id "
        + "left join team_user tu on tu.team_id = team.id "
        + "where tu.user = ?1 "
        , nativeQuery = true)
    List<JSONObject> findObjectByUser(String user);

}
