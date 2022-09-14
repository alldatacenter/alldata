package com.alibaba.sreworks.teammanage.server.services;

import com.alibaba.sreworks.domain.DO.TeamUser;
import com.alibaba.sreworks.domain.DTO.TeamUserRole;
import com.alibaba.sreworks.domain.repository.TeamUserRepository;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Data
public class TeamUserService {

    @Autowired
    TeamUserRepository teamUserRepository;

    public void assertUserAdmin(Long teamId, String user) throws Exception {
        TeamUser teamUser = teamUserRepository.findFirstByTeamIdAndUser(teamId, user);
        if (!teamUser.role().equals(TeamUserRole.ADMIN)) {
            throw new Exception("only admin can remove user");
        }
    }

}
