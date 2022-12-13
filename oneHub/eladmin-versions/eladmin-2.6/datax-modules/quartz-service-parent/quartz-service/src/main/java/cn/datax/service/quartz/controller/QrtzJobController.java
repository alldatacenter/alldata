package cn.datax.service.quartz.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.quartz.api.dto.QrtzJobDto;
import cn.datax.service.quartz.api.entity.QrtzJobEntity;
import cn.datax.service.quartz.api.vo.QrtzJobVo;
import cn.datax.service.quartz.api.query.QrtzJobQuery;
import cn.datax.service.quartz.mapstruct.QrtzJobMapper;
import cn.datax.service.quartz.service.QrtzJobService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 定时任务信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Api(tags = {"定时任务信息表"})
@RestController
@RequestMapping("/jobs")
public class QrtzJobController extends BaseController {

    @Autowired
    private QrtzJobService qrtzJobService;

    @Autowired
    private QrtzJobMapper qrtzJobMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getQrtzJobById(@PathVariable String id) {
        QrtzJobEntity qrtzJobEntity = qrtzJobService.getQrtzJobById(id);
        return R.ok().setData(qrtzJobMapper.toVO(qrtzJobEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getQrtzJobList() {
        QueryWrapper<QrtzJobEntity> queryWrapper = new QueryWrapper<>();
        List<QrtzJobEntity> list = qrtzJobService.list(queryWrapper);
        List<QrtzJobVo> collect = list.stream().map(qrtzJobMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param qrtzJobQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "qrtzJobQuery", value = "查询实体qrtzJobQuery", required = true, dataTypeClass = QrtzJobQuery.class)
    })
    @GetMapping("/page")
    public R getQrtzJobPage(QrtzJobQuery qrtzJobQuery) {
        QueryWrapper<QrtzJobEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(qrtzJobQuery.getJobName()), "job_name", qrtzJobQuery.getJobName());
        IPage<QrtzJobEntity> page = qrtzJobService.page(new Page<>(qrtzJobQuery.getPageNum(), qrtzJobQuery.getPageSize()), queryWrapper);
        List<QrtzJobVo> collect = page.getRecords().stream().map(qrtzJobMapper::toVO).collect(Collectors.toList());
        JsonPage<QrtzJobVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param qrtzJob
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据qrtzJob对象添加信息")
    @ApiImplicitParam(name = "qrtzJob", value = "详细实体qrtzJob", required = true, dataType = "QrtzJobDto")
    @PostMapping()
    public R saveQrtzJob(@RequestBody @Validated({ValidationGroups.Insert.class}) QrtzJobDto qrtzJob) {
        qrtzJobService.saveQrtzJob(qrtzJob);
        return R.ok();
    }

    /**
     * 修改
     * @param qrtzJob
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "qrtzJob", value = "详细实体qrtzJob", required = true, dataType = "QrtzJobDto")
    })
    @PutMapping("/{id}")
    public R updateQrtzJob(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) QrtzJobDto qrtzJob) {
        qrtzJobService.updateQrtzJob(qrtzJob);
        return R.ok();
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteQrtzJobById(@PathVariable String id) {
        qrtzJobService.deleteQrtzJobById(id);
        return R.ok();
    }

    /**
     * 暂停任务
     * @param id
     * @return
     */
    @ApiOperation(value = "暂停任务", notes = "根据url的id来暂停指定任务")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/pause/{id}")
    public R pauseById(@PathVariable("id") String id) {
        qrtzJobService.pauseById(id);
        return R.ok();
    }

    /**
     * 恢复任务
     * @param id
     * @return
     */
    @ApiOperation(value = "恢复任务", notes = "根据url的id来恢复指定任务")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/resume/{id}")
    public R resumeById(@PathVariable("id") String id) {
        qrtzJobService.resumeById(id);
        return R.ok();
    }

    /**
     * 立即执行任务
     * @param id
     * @return
     */
    @ApiOperation(value = "立即执行任务", notes = "根据url的id来立即执行指定任务")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/run/{id}")
    public R runById(@PathVariable("id") String id) {
        qrtzJobService.runById(id);
        return R.ok();
    }
}
