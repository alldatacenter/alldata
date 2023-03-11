package com.hw.lineage.server.domain.repository;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.domain.entity.Function;
import com.hw.lineage.server.domain.query.function.FunctionCheck;
import com.hw.lineage.server.domain.query.function.FunctionEntry;
import com.hw.lineage.server.domain.query.function.FunctionQuery;
import com.hw.lineage.server.domain.repository.basic.Repository;
import com.hw.lineage.server.domain.vo.FunctionId;

import java.util.List;

/**
 * @description: FunctionRepository
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface FunctionRepository extends Repository<Function, FunctionId> {
    PageInfo<Function> findAll(FunctionQuery functionQuery);

     boolean check(FunctionCheck functionCheck);

    FunctionEntry findEntry(FunctionId functionId);


    List<Function> findMemory();
}