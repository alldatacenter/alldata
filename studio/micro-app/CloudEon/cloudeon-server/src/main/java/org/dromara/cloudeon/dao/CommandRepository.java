package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.CommandEntity;
import org.dromara.cloudeon.enums.CommandState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommandRepository extends JpaRepository<CommandEntity, Integer> {
    List<CommandEntity> findByClusterIdOrderBySubmitTimeDesc(Integer clusterId);

    long countByCommandStateAndClusterId(CommandState commandState, Integer clusterId);

    //update或delete时必须使用@Modifying对方法进行注解，才能使得ORM知道现在要执行的是写操作
    @Modifying
    @Query("update CommandEntity sc set sc.commandState = 'ERROR' where sc.commandState = 'RUNNING' or sc.commandState = 'WAITTING' ")
    public void updateRunningCommand2Error();
}