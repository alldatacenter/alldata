package dynamicscripts


import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.domain.req.workflow.ExecuteWorkflowHandlerReq
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecuteWorkflowHandlerRes
import com.alibaba.tesla.appmanager.workflow.dynamicscript.WorkflowHandler
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Workflow Default Suspend Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
class WorkflowSuspendHandler implements WorkflowHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSuspendHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.WORKFLOW.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "suspend"

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 0

    /**
     * 执行逻辑
     * @param request Workflow 执行请求
     * @return Workflow 执行结果
     */
    @Override
    ExecuteWorkflowHandlerRes execute(ExecuteWorkflowHandlerReq request) throws InterruptedException {
        return ExecuteWorkflowHandlerRes.builder()
                .context(request.getContext())
                .configuration(request.getConfiguration())
                .suspend(true)
                .build()
    }
}
