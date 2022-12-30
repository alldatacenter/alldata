package cn.datax.service.workflow.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.workflow.api.query.FlowDefinitionQuery;
import cn.datax.service.workflow.api.vo.FlowDefinitionVo;
import cn.datax.service.workflow.service.BusinessService;
import cn.datax.service.workflow.service.FlowDefinitionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

@Api(tags = {"流程定义"})
@RestController
@RequestMapping("/definitions")
public class FlowDefinitionController extends BaseController {

	@Autowired
	private FlowDefinitionService flowDefinitionService;
	@Autowired
	private BusinessService businessService;

	@ApiOperation(value = "分页查询")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "categoryQuery", value = "查询实体flowDefinitionQuery", required = true, dataTypeClass = FlowDefinitionQuery.class)
	})
	@GetMapping("/page")
	public R page(FlowDefinitionQuery flowDefinitionQuery) {
		Page<FlowDefinitionVo> page = flowDefinitionService.page(flowDefinitionQuery);
		JsonPage<FlowDefinitionVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
		return R.ok().setData(jsonPage);
	}

	@PostMapping("/import/file")
	@ApiOperation(value = "部署流程模板文件")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "name", value = "模板名称（模板name）", required = true, dataType = "String"),
			@ApiImplicitParam(name = "category", value = "模板类别", required = false, dataType = "String"),
			@ApiImplicitParam(name = "tenantId", value = "租户ID", required = false, dataType = "String"),
			@ApiImplicitParam(name = "file", value = "模板文件", required = true, dataType = "__file")
	})
	public R deployByInputStream(String name, String category, String tenantId, @RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			return R.error("文件内容为空");
		}
		try (InputStream in = file.getInputStream()) {
			flowDefinitionService.deploy(name, category, tenantId, in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return R.ok();
	}

	@PostMapping("/import/files/zip")
	@ApiOperation(value = "部署压缩包形式的模板(.zip.bar)")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "name", value = "模板名称（模板name）", required = true, dataType = "String"),
			@ApiImplicitParam(name = "category", value = "模板类别", required = false, dataType = "String"),
			@ApiImplicitParam(name = "tenantId", value = "租户ID", required = false, dataType = "String"),
			@ApiImplicitParam(name = "file", value = "模板文件", required = true, dataType = "__file")
	})
	public R deployByZip(String name, String category, String tenantId, @RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			return R.error("文件内容为空");
		}
		try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream(), Charset.forName("UTF-8"))) {
			flowDefinitionService.deploy(name, category, tenantId, zipIn);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return R.ok();
	}

	@ApiOperation(value = "激活流程定义", notes = "根据url的id来指定激活流程定义")
	@ApiImplicitParam(name = "processDefinitionId", value = "流程定义ID", required = true, dataType = "String", paramType = "path")
	@PutMapping("/activate/{processDefinitionId}")
	public R activate(@PathVariable String processDefinitionId) {
		flowDefinitionService.activateProcessDefinitionById(processDefinitionId);
		return R.ok();
	}

	@ApiOperation(value = "挂起流程定义", notes = "根据url的id来指定挂起流程定义")
	@ApiImplicitParam(name = "processDefinitionId", value = "流程定义ID", required = true, dataType = "String", paramType = "path")
	@PutMapping("/suspend/{processDefinitionId}")
	public R suspend(@PathVariable String processDefinitionId) {
		flowDefinitionService.suspendProcessDefinitionById(processDefinitionId);
		return R.ok();
	}

	@ApiOperation(value = "删除流程定义", notes = "根据url的id来指定删除流程定义")
	@ApiImplicitParam(name = "deploymentId", value = "流程部署ID", required = true, dataType = "String", paramType = "path")
	@DeleteMapping("/delete/{deploymentId}")
	public R delete(@PathVariable String deploymentId) {
		ProcessDefinition processDefinition = flowDefinitionService.getByDeploymentId(deploymentId);
		businessService.checkHasDefId(processDefinition.getId());
		flowDefinitionService.deleteDeployment(deploymentId);
		return R.ok();
	}

	@GetMapping("/resource")
	@ApiOperation(value = "查看流程部署xml资源和png资源")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "processDefinitionId", value = "流程定义ID", required = true, dataType = "String"),
			@ApiImplicitParam(name = "resType", value = "资源类型(xml|image)", required = true, dataType = "String")
	})
	public void resource(String processDefinitionId, String resType, HttpServletResponse response) throws Exception {
		InputStream resourceAsStream = flowDefinitionService.resource(processDefinitionId, resType);
		byte[] b = new byte[1024];
		int len = -1;
		while ((len = resourceAsStream.read(b, 0, 1024)) != -1) {
			response.getOutputStream().write(b, 0, len);
		}
	}
}
