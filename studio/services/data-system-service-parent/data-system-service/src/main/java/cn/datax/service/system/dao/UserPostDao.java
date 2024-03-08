package cn.datax.service.system.dao;

import cn.datax.service.system.api.entity.UserPostEntity;
import cn.datax.common.base.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Mapper
public interface UserPostDao extends BaseDao<UserPostEntity> {

    void insertBatch(List<UserPostEntity> list);

    void deleteByUserId(String id);

    void deleteByUserIds(List<String> ids);
}
