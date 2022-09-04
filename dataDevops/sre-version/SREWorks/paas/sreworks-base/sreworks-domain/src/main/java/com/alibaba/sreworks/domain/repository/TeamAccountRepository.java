package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.sreworks.domain.DO.TeamAccount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author jinghua.yjh
 */
public interface TeamAccountRepository extends JpaRepository<TeamAccount, Long>, JpaSpecificationExecutor<TeamAccount> {

    TeamAccount findFirstById(Long id);

    List<TeamAccount> findAllByTeamIdOrderByIdDesc(Long teamId);

    List<TeamAccount> findAllByTeamIdAndNameLikeOrderByIdDesc(Long teamId, String name);

}
