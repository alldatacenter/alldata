package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.CheckReportEntity;
import cn.datax.service.data.quality.api.entity.DataReportEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 核查报告信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Mapper
public interface CheckReportDao extends BaseDao<CheckReportEntity> {

    @Override
    <E extends IPage<CheckReportEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<CheckReportEntity> queryWrapper);

    List<DataReportEntity> getReportBySource(@Param("checkDate") String checkDate);

    List<DataReportEntity> getReportByType(@Param("checkDate") String checkDate);

    List<DataReportEntity> getReportDetail(@Param("checkDate") String checkDate);
}
