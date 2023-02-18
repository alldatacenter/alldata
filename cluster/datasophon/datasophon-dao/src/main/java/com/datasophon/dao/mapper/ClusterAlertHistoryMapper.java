package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterAlertHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 集群告警历史表 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-07 12:04:38
 */
@Mapper
public interface ClusterAlertHistoryMapper extends BaseMapper<ClusterAlertHistory> {
	
}
