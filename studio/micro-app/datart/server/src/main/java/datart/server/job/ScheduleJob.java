package datart.server.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import datart.core.base.consts.AttachmentType;
import datart.core.base.consts.FileOwner;
import datart.core.common.Application;
import datart.core.common.FileUtils;
import datart.core.common.UUIDGenerator;
import datart.core.entity.Folder;
import datart.core.entity.Schedule;
import datart.core.entity.ScheduleLog;
import datart.core.entity.User;
import datart.core.mappers.ext.ScheduleLogMapperExt;
import datart.core.mappers.ext.ScheduleMapperExt;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.base.ResourceType;
import datart.security.manager.DatartSecurityManager;
import datart.server.base.dto.DashboardDetail;
import datart.server.base.dto.DatachartDetail;
import datart.server.base.dto.JobFile;
import datart.server.base.dto.ScheduleJobConfig;
import datart.server.base.params.DownloadCreateParam;
import datart.server.base.params.ViewExecuteParam;
import datart.server.common.JsParserUtils;
import datart.server.service.AttachmentService;
import datart.server.service.FolderService;
import datart.server.service.ShareService;
import datart.server.service.VizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public abstract class ScheduleJob implements Job, Closeable {

    public static final String SCHEDULE_KEY = "SCHEDULE_KEY";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected Schedule schedule;

    protected ScheduleJobConfig jobConfig;

    protected final ScheduleLogMapperExt scheduleLogMapper;

    protected final ShareService shareService;

    protected final DatartSecurityManager securityManager;

    protected final List<JobFile> attachments = new LinkedList<>();

    protected final VizService vizService;

    public ScheduleJob() {

        scheduleLogMapper = Application.getBean(ScheduleLogMapperExt.class);

        shareService = Application.getBean(ShareService.class);

        securityManager = Application.getBean(DatartSecurityManager.class);

        vizService = Application.getBean(VizService.class);

    }

    @Override
    public void execute(JobExecutionContext context) {
        String scheduleId = (String) context.getMergedJobDataMap().get(SCHEDULE_KEY);
        execute(scheduleId);
    }

    public boolean execute(String scheduleId) {
        Date start = new Date();
        int status = 1;
        String message = "SUCCESS";
        try {
            ScheduleMapperExt scheduleMapper = Application.getBean(ScheduleMapperExt.class);
            schedule = scheduleMapper.selectByPrimaryKey(scheduleId);
            login(schedule.getCreateBy());
            jobConfig = parseConfig(schedule);
            status = status << 1 | status;
            doGetData();
            status = status << 1 | status;
            doSend();
            status = status << 1 | status;
            return true;
        } catch (Exception e) {
            message = e.getMessage();
            log.error("schedule execute error", e);
            return false;
        } finally {
            insertLog(start, new Date(), scheduleId, status, message);
            try {
                close();
            } catch (IOException ignored) {
            }
        }
    }

    public void doGetData() throws Exception {
        ScheduleJobConfig config = parseConfig(schedule);
        if (CollectionUtils.isEmpty(config.getVizContents()) || CollectionUtils.isEmpty(config.getAttachments())) {
            return;
        }

        String path = FileUtils.concatPath(FileOwner.SCHEDULE.getPath(), schedule.getId());

        FolderService folderService = Application.getBean(FolderService.class);

        for (ScheduleJobConfig.VizContent vizContent : config.getVizContents()) {
            Folder folder = folderService.retrieve(vizContent.getVizId());
            DownloadCreateParam downloadCreateParam;
            if (ResourceType.DATACHART.name().equals(folder.getRelType())) {
                DatachartDetail datachart = vizService.getDatachart(folder.getRelId());
                downloadCreateParam = JsParserUtils.parseExecuteParam("chart", OBJECT_MAPPER.writeValueAsString(datachart));
            } else {
                DashboardDetail dashboard = vizService.getDashboard(folder.getRelId());
                downloadCreateParam = JsParserUtils.parseExecuteParam("board", OBJECT_MAPPER.writeValueAsString(dashboard));
            }

            if (config.getImageWidth() != null) {
                downloadCreateParam.setImageWidth(config.getImageWidth());
            }

            for (AttachmentType type : config.getAttachments()) {
                DownloadCreateParam param = setVizId(downloadCreateParam, folder, type);
                AttachmentService attachmentService = AttachmentService.matchAttachmentService(type);
                JobFile jobFile = new JobFile();
                File file = attachmentService.getFile(param, path, downloadCreateParam.getFileName());
                jobFile.setFile(file);
                jobFile.setType(type);
                attachments.add(jobFile);
            }
        }

    }

    private DownloadCreateParam setVizId(DownloadCreateParam downloadCreateParam, Folder folder, AttachmentType attachmentType) {
        DownloadCreateParam result = new DownloadCreateParam();
        BeanUtils.copyProperties(downloadCreateParam, result);

        if (ResourceType.DATACHART.name().equals(folder.getRelType())
                && !CollectionUtils.isEmpty(result.getDownloadParams())
                && result.getDownloadParams().size() == 1) {
            ViewExecuteParam viewExecuteParam = result.getDownloadParams().get(0);
            if (attachmentType.equals(AttachmentType.EXCEL)) {
                viewExecuteParam.setVizId(folder.getRelId());
                viewExecuteParam.setVizType(ResourceType.DATACHART);
            } else {
                viewExecuteParam.setVizId(folder.getId());
            }
        } else if (ResourceType.DASHBOARD.name().equals(folder.getRelType())
                && (attachmentType.equals(AttachmentType.IMAGE) || attachmentType.equals(AttachmentType.PDF))) {
            ViewExecuteParam viewExecuteParam = new ViewExecuteParam();
            viewExecuteParam.setVizId(folder.getId());
            viewExecuteParam.setVizType(ResourceType.DASHBOARD);
            result.setDownloadParams(Lists.newArrayList(viewExecuteParam));
        }
        return result;
    }

    public abstract void doSend() throws Exception;

    private ScheduleJobConfig parseConfig(Schedule schedule) throws JsonProcessingException {
        if (StringUtils.isBlank(schedule.getConfig())) {
            return null;
        }
        return OBJECT_MAPPER.readValue(schedule.getConfig(), ScheduleJobConfig.class);
    }

    private void insertLog(Date start, Date end, String scheduleId, int status, String message) {
        ScheduleLog scheduleLog = new ScheduleLog();
        scheduleLog.setId(UUIDGenerator.generate());
        scheduleLog.setScheduleId(scheduleId);
        scheduleLog.setStart(start);
        scheduleLog.setEnd(end);
        scheduleLog.setStatus(status);
        scheduleLog.setMessage(message);
        scheduleLogMapper.insert(scheduleLog);
    }

    @Override
    public void close() throws IOException {
        try {
            securityManager.logoutCurrent();
        } catch (Exception e) {
            log.error("schedule logout error", e);
        }

        for (JobFile jobFile : attachments) {
            FileUtils.delete(jobFile.getFile());
        }
    }

    private void login(String userId) {
        User user = Application.getBean(UserMapperExt.class).selectByPrimaryKey(userId);
        securityManager.runAs(user.getUsername());
    }

}
