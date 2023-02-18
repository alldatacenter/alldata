package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.SessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-16 11:40:00
 */
@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {

    SessionEntity queryByUserIdAndIp(@Param("userId") Integer id, @Param("ip") String ip);

    List<SessionEntity> queryByUserId(@Param("userId") Integer id);

    void insertSession(SessionEntity session);
}
