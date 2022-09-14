package com.alibaba.sreworks.domain.repository;

import java.util.List;

import javax.transaction.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.TeamUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface TeamUserRepository extends JpaRepository<TeamUser, Long>, JpaSpecificationExecutor<TeamUser> {

    TeamUser findFirstByTeamIdAndUser(Long teamId, String user);

    @Query(value = ""
        + "select a.*, IFNULL(b.user_num, 0) as user_num, IFNULL(c.account_num, 0) as account_num "
        + "from  "
        + "( "
        + "    select id, gmt_create, name, description, visible_scope, avatar "
        + "    from team where id in (select team_id from team_user where user = ?1) and name like ?2 "
        + ") a "
        + "left join "
        + "    (select team_id, count(*) as user_num from team_user group by team_id) b "
        + "on a.id = b.team_id  "
        + "left join "
        + "    (select team_id, count(*) as account_num from team_account group by team_id) c "
        + "on a.id = c.team_id "
        + "order by a.id desc "
        , nativeQuery = true)
    List<JSONObject> findObjectByUserAndNameLikeOrderByIdDesc(String user, String name);

    @Query(value = ""
        + "select a.id, a.gmt_create, b.gmt_access, a.name, a.description "
        + "from team a join team_user b "
        + "on a.id = b.team_id and b.user = ?1 "
        + "order by b.gmt_access desc limit ?2 "
        , nativeQuery = true)
    List<JSONObject> findObjectByUserOrderByGmtAccessDesc(String user, int limit);

    List<TeamUser> findAllByTeamId(Long teamId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    void deleteByTeamIdAndUser(Long teamId, String user);

    Long countByTeamId(Long teamId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query(value = "update team_user set gmt_access = now() where team_id = ?1 and user = ?2"
        , nativeQuery = true)
    void updateGmtAccessByTeamIdAndUser(Long teamId, String user);

    List<TeamUser> findAllByUser(String user);

}
