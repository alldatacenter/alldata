package cn.datax.service.system.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.system.api.dto.DeptDto;
import cn.datax.service.system.api.entity.DeptEntity;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-09-04
 */
public interface DeptService extends BaseService<DeptEntity> {

    DeptEntity saveDept(DeptDto dept);

    DeptEntity updateDept(DeptDto dept);

    void deleteDeptById(String id);
}
