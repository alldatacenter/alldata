package cn.datax.service.quartz.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.quartz.api.dto.QrtzJobLogDto;
import cn.datax.service.quartz.api.entity.QrtzJobLogEntity;
import cn.datax.service.quartz.api.vo.QrtzJobLogVo;
import cn.datax.service.quartz.api.query.QrtzJobLogQuery;
import cn.datax.service.quartz.mapstruct.QrtzJobLogMapper;
import cn.datax.service.quartz.service.QrtzJobLogService;
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
 * 定时任务日志信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Api(tags = {"定时任务日志信息表"})
@RestController
@RequestMapping("/logs")
public class QrtzJobLogController extends BaseController {

    @Autowired
    private QrtzJobLogService qrtzJobLogService;

    @Autowired
    private QrtzJobLogMapper qrtzJobLogMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getQrtzJobLogById(@PathVariable String id) {
        QrtzJobLogEntity qrtzJobLogEntity = qrtzJobLogService.getQrtzJobLogById(id);
        return R.ok().setData(qrtzJobLogMapper.toVO(qrtzJobLogEntity));
    }

    /**
     * 分页查询信息
     *
     * @param qrtzJobLogQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "qrtzJobLogQuery", value = "查询实体qrtzJobLogQuery", required = true, dataTypeClass = QrtzJobLogQuery.class)
    })
    @GetMapping("/page")
    public R getQrtzJobLogPage(QrtzJobLogQuery qrtzJobLogQuery) {
        QueryWrapper<QrtzJobLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(qrtzJobLogQuery.getJobId()), "job_id", qrtzJobLogQuery.getJobId());
        IPage<QrtzJobLogEntity> page = qrtzJobLogService.page(new Page<>(qrtzJobLogQuery.getPageNum(), qrtzJobLogQuery.getPageSize()), queryWrapper);
        List<QrtzJobLogVo> collect = page.getRecords().stream().map(qrtzJobLogMapper::toVO).collect(Collectors.toList());
        JsonPage<QrtzJobLogVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param qrtzJobLog
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据qrtzJobLog对象添加信息")
    @ApiImplicitParam(name = "qrtzJobLog", value = "详细实体qrtzJobLog", required = true, dataType = "QrtzJobLogDto")
    @PostMapping()
    public R saveQrtzJobLog(@RequestBody @Validated({ValidationGroups.Insert.class}) QrtzJobLogDto qrtzJobLog) {
        qrtzJobLogService.saveQrtzJobLog(qrtzJobLog);
        return R.ok();
    }

    /**
     * 修改
     * @param qrtzJobLog
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "qrtzJobLog", value = "详细实体qrtzJobLog", required = true, dataType = "QrtzJobLogDto")
    })
    @PutMapping("/{id}")
    public R updateQrtzJobLog(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) QrtzJobLogDto qrtzJobLog) {
        qrtzJobLogService.updateQrtzJobLog(qrtzJobLog);
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
    public R deleteQrtzJobLogById(@PathVariable String id) {
        qrtzJobLogService.deleteQrtzJobLogById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteQrtzJobLogBatch(@PathVariable List<String> ids) {
        qrtzJobLogService.deleteQrtzJobLogBatch(ids);
        return R.ok();
    }
}
