package datart.server.service;

import datart.core.entity.Schedule;
import datart.core.entity.ScheduleLog;
import datart.core.mappers.ext.ScheduleMapperExt;
import datart.server.base.dto.ScheduleBaseInfo;
import datart.server.base.params.ScheduleBaseUpdateParam;
import org.quartz.SchedulerException;

import java.util.List;

public interface ScheduleService extends BaseCRUDService<Schedule, ScheduleMapperExt> {

    List<ScheduleBaseInfo> listSchedules(String orgId);

    List<ScheduleBaseInfo> listArchivedSchedules(String orgId);

    boolean execute(String scheduleId);

    boolean start(String scheduleId) throws SchedulerException;

    boolean stop(String scheduleId) throws SchedulerException;

    List<ScheduleLog> getScheduleLogs(String scheduleId, int count);

    boolean updateBase(ScheduleBaseUpdateParam updateParam);

    boolean unarchive(String id, String newName, String parentId, double index);

    boolean checkUnique(String orgId, String parentId, String name);

}
