package com.hw.lineage.server.domain.service.impl;

import com.hw.lineage.common.enums.SqlStatus;
import com.hw.lineage.common.enums.SqlType;
import com.hw.lineage.common.util.Base64Utils;
import com.hw.lineage.common.util.EnumUtils;
import com.hw.lineage.server.domain.entity.task.Task;
import com.hw.lineage.server.domain.entity.task.TaskSql;
import com.hw.lineage.server.domain.service.TaskDomainService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.hw.lineage.common.enums.SqlType.*;
import static com.hw.lineage.common.util.Preconditions.checkArgument;

/**
 * @description: TaskDomainServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class TaskDomainServiceImpl implements TaskDomainService {

    private static final List<SqlType> SUPPORT_SQL_TYPE = Arrays.asList(CREATE, INSERT, DROP);

    @Override
    public void generateTaskSql(Task task) {
        String[] sqlArrays = task.getTaskSource().splitSource();

        Stream.of(sqlArrays).forEach(singleSql -> {
            SqlType sqlType = extractSqlType(singleSql);
            checkArgument(SUPPORT_SQL_TYPE.contains(sqlType)
                    , "currently only supports SQL starting with %s, not yet: %s", SUPPORT_SQL_TYPE, singleSql);

            TaskSql taskSql = new TaskSql()
                    .setTaskId(task.getTaskId())
                    .setSqlSource(Base64Utils.encode(singleSql))
                    .setSqlType(sqlType)
                    .setSqlStatus(SqlStatus.INIT)
                    .setInvalid(false);
            task.addTaskSql(taskSql);
        });
    }

    private SqlType extractSqlType(String singleSql) {
        String sqlType = singleSql.split("\\s+")[0];
        return EnumUtils.getSqlTypeByValue(sqlType);
    }

}
