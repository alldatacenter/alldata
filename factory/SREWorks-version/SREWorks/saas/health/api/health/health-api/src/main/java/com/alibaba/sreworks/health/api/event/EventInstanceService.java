package com.alibaba.sreworks.health.api.event;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceUpdateReq;

import java.util.List;

public interface EventInstanceService extends BasicApi {
    /**
     * 按照App查询事件实例
     * @return
     */
    List<JSONObject> getEventsByApp(String appId, Long sTimestamp, Long eTimestamp);

    /**
     * 按照实体查询事件实例
     * @return
     */
    List<JSONObject> getEventsByInstance(String appId, String instanceId, Long sTimestamp, Long eTimestamp);

    /**
     * 按照实例查询事件实例
     * @return
     */
    List<JSONObject> getEvents(String appId, String instanceId, String appComponentName, String appComponentInstanceId, Long sTimestamp, Long eTimestamp);

    /**
     * 按照ID查询事件实例
     * @return
     */
    JSONObject getEventById(Long id);

    /**
     * 事件实例存在
     * @param id
     * @return
     */
    boolean existEvent(Long id);

    /**
     * 事件实例不存在
     * @param id
     * @return
     */
    boolean notExistEvent(Long id);

    /**
     * 按照事件定义批量新建事件实例
     *  @param defId 定义ID
     *  @param events 事件列表
     * @return
     * @throws Exception
     */
    int pushEvents(Integer defId, List<JSONObject> events) throws Exception;

    /**
     * 新建事件实例
     * @param req
     * @return
     * @throws Exception
     */
    int addEvent(EventInstanceCreateReq req) throws Exception;

    /**
     * 更新事件实例
     * @param req
     * @return
     * @throws Exception
     */
    int updateEvent(EventInstanceUpdateReq req) throws Exception;

    /**
     * 删除事件实例
     * @param id
     * @return
     */
    int deleteEvent(Long id) throws Exception;
}
