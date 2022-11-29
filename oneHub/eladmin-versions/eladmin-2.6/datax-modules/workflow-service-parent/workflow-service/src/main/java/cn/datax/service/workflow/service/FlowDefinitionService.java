package cn.datax.service.workflow.service;

import cn.datax.service.workflow.api.query.FlowDefinitionQuery;
import cn.datax.service.workflow.api.vo.FlowDefinitionVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

import java.io.InputStream;
import java.util.zip.ZipInputStream;

public interface FlowDefinitionService {

    /**
     * 分页查询流程定义
     * @param flowDefinitionQuery
     * @return
     */
    Page<FlowDefinitionVo> page(FlowDefinitionQuery flowDefinitionQuery);

    /**
     * 部署流程资源
     *
     * @param name     流程模板文件名字
     * @param category 流程模板文件类别
     * @param tenantId 租户ID
     * @param in       流程模板文件流
     * @return
     */
    void deploy(String name, String category, String tenantId, InputStream in);

    /**
     * 部署压缩包内的流程资源
     *
     * @param name           流程模板文件名字
     * @param category       流程模板文件类别
     * @param tenantId       租户ID
     * @param zipInputStream
     * @return
     */
    void deploy(String name, String category, String tenantId, ZipInputStream zipInputStream);

    /**
     * 激活流程定义
     * @param processDefinitionId
     */
    void activateProcessDefinitionById(String processDefinitionId);

    /**
     * 挂起流程定义
     * @param processDefinitionId
     */
    void suspendProcessDefinitionById(String processDefinitionId);

    /**
     * 删除流程定义
     * @param deploymentId
     */
    void deleteDeployment(String deploymentId);

    /**
     * 查看流程部署xml资源和png资源
     * @param processDefinitionId
     * @param resType
     * @return
     */
    InputStream resource(String processDefinitionId, String resType);

	ProcessDefinition getByDeploymentId(String deploymentId);

	Deployment getByCategoryId(String categoryId);
}
