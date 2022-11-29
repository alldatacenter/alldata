package cn.datax.service.workflow.service.impl;

import cn.datax.service.workflow.api.query.FlowDefinitionQuery;
import cn.datax.service.workflow.api.vo.FlowDefinitionVo;
import cn.datax.service.workflow.service.FlowDefinitionService;
import cn.datax.service.workflow.utils.BeanCopyUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.NativeDeploymentQuery;
import org.flowable.engine.repository.NativeProcessDefinitionQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class FlowDefinitionServiceImpl implements FlowDefinitionService {

	@Autowired
	private RepositoryService repositoryService;

	public static final String BPMN20_FILE_SUFFIX = ".bpmn20.xml";
	public static final String RESOURCE_TYPE_IMAGE = "image";
	public static final String RESOURCE_TYPE_XML = "xml";

	@Override
	public Page<FlowDefinitionVo> page(FlowDefinitionQuery flowDefinitionQuery) {
		NativeProcessDefinitionQuery nativeProcessDefinitionQuery = repositoryService.createNativeProcessDefinitionQuery();
		String sqlCount = "SELECT COUNT(RES.ID_) FROM ACT_RE_PROCDEF RES " +
				"LEFT JOIN ACT_RE_DEPLOYMENT RED ON RES.DEPLOYMENT_ID_ = RED.ID_ " +
				"WHERE 1=1 ";
		String sqlList = "SELECT RES.* FROM ACT_RE_PROCDEF RES " +
				"LEFT JOIN ACT_RE_DEPLOYMENT RED ON RES.DEPLOYMENT_ID_ = RED.ID_ " +
				"WHERE 1=1 ";
		if (StrUtil.isNotBlank(flowDefinitionQuery.getName())) {
			sqlCount += "AND RES.NAME_ like #{name} ";
			sqlList += "AND RES.NAME_ like #{name} ";
		}
		if (StrUtil.isNotBlank(flowDefinitionQuery.getKey())) {
			sqlCount += "AND RES.KEY_ like #{key} ";
			sqlList += "AND RES.KEY_ like #{key} ";
		}
		if (StrUtil.isNotBlank(flowDefinitionQuery.getCategoryId())) {
			sqlCount += "AND RED.CATEGORY_ = #{category} ";
			sqlList += "AND RED.CATEGORY_ = #{category} ";
		}
		sqlList += "ORDER BY RES.ID_ ASC ";
		long count = nativeProcessDefinitionQuery.sql(sqlCount)
				.parameter("name", flowDefinitionQuery.getName())
				.parameter("key", flowDefinitionQuery.getKey())
				.parameter("category", flowDefinitionQuery.getCategoryId())
				.count();
		List<ProcessDefinition> processDefinitionList = nativeProcessDefinitionQuery.sql(sqlList)
				.parameter("name", flowDefinitionQuery.getName())
				.parameter("key", flowDefinitionQuery.getKey())
				.parameter("category", flowDefinitionQuery.getCategoryId())
				.listPage((flowDefinitionQuery.getPageNum() - 1) * flowDefinitionQuery.getPageSize(), flowDefinitionQuery.getPageSize());
		List<FlowDefinitionVo> flowDefinitionVoList = BeanCopyUtil.copyListProperties(processDefinitionList, FlowDefinitionVo::new);
		Page<FlowDefinitionVo> page = new Page<>(flowDefinitionQuery.getPageNum(), flowDefinitionQuery.getPageSize());
		page.setRecords(flowDefinitionVoList);
		page.setTotal(count);
		return page;
	}

	@Override
	public void deploy(String name, String category, String tenantId, InputStream in) {
		DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
		deploymentBuilder.addInputStream(name + BPMN20_FILE_SUFFIX, in);
		deploymentBuilder.name(name);
		if (StrUtil.isNotBlank(tenantId)) {
			deploymentBuilder.tenantId(tenantId);
		}
		if (StrUtil.isNotBlank(category)) {
			deploymentBuilder.category(category);
		}
		Deployment deployment = deploymentBuilder.deploy();
	}

	@Override
	public void deploy(String name, String category, String tenantId, ZipInputStream zipInputStream) {
		DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
		deploymentBuilder.addZipInputStream(zipInputStream);
		deploymentBuilder.name(name);
		if (StrUtil.isNotBlank(tenantId)) {
			deploymentBuilder.tenantId(tenantId);
		}
		if (StrUtil.isNotBlank(category)) {
			deploymentBuilder.category(category);
		}
		Deployment deployment = deploymentBuilder.deploy();
	}

	@Override
	public void activateProcessDefinitionById(String processDefinitionId) {
		log.info("成功激活流程定义ID:{}", processDefinitionId);
		repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
	}

	@Override
	public void suspendProcessDefinitionById(String processDefinitionId) {
		log.info("成功激活流程定义ID:{}", processDefinitionId);
		repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
	}

	@Override
	public void deleteDeployment(String deploymentId) {
		log.info("成功删除流程部署ID:{}", deploymentId);
		repositoryService.deleteDeployment(deploymentId, true);
	}

	@Override
	public InputStream resource(String processDefinitionId, String resType) {
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		String resourceName = "";
		if (RESOURCE_TYPE_IMAGE.equals(resType)) {
			resourceName = processDefinition.getDiagramResourceName();
		} else if (RESOURCE_TYPE_XML.equals(resType)) {
			resourceName = processDefinition.getResourceName();
		}
		InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
		return resourceAsStream;
	}

	@Override
	public ProcessDefinition getByDeploymentId(String deploymentId) {
		NativeProcessDefinitionQuery nativeProcessDefinitionQuery = repositoryService.createNativeProcessDefinitionQuery();
		String sql = "SELECT ID_ FROM ACT_RE_PROCDEF WHERE DEPLOYMENT_ID_=#{DEPLOYMENT_ID_} LIMIT 1";
		return nativeProcessDefinitionQuery.sql(sql).parameter("DEPLOYMENT_ID_", deploymentId).singleResult();
	}

	@Override
	public Deployment getByCategoryId(String categoryId) {
		NativeDeploymentQuery nativeDeploymentQuery = repositoryService.createNativeDeploymentQuery();
		String sql = "SELECT ID_ FROM ACT_RE_DEPLOYMENT WHERE CATEGORY_=#{category} LIMIT 1";
		return nativeDeploymentQuery.sql(sql).parameter("category", categoryId).singleResult();
	}
}
