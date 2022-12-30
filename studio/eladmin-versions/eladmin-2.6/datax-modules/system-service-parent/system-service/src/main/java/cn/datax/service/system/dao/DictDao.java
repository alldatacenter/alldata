package cn.datax.service.system.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.system.api.entity.DictEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 字典编码信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-17
 */
@Mapper
public interface DictDao extends BaseDao<DictEntity> {

    /**
     * 查询有效字典集合
     *
     * @return
     * @param status
     */
    List<DictEntity> queryDictList(@Param("status") String status);
}
