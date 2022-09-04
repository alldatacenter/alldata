package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.Team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    Team findFirstById(Long id);

    @Query(value = ""
        + "select a.*, IFNULL(b.user_num, 0) as user_num, IFNULL(c.account_num, 0) as account_num "
        + "from  "
        + "( "
        + "    select * from team where visible_scope = ?1 and name like ?2 "
        + ") a "
        + "left join "
        + "    (select team_id, count(*) as user_num from team_user group by team_id) b "
        + "on a.id = b.team_id  "
        + "left join "
        + "    (select team_id, count(*) as account_num from team_account group by team_id) c "
        + "on a.id = c.team_id "
        + "order by a.id desc "
        , nativeQuery = true)
    List<JSONObject> findObjectByVisibleScopeAndNameLikeOrderByIdDesc(String visibleScope, String name);

}
