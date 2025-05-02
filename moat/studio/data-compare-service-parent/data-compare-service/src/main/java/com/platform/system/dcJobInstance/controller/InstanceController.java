package com.platform.system.dcJobInstance.controller;

import com.platform.common.aspectj.lang.annotation.Log;
import com.platform.common.aspectj.lang.enums.BusinessType;
import com.platform.common.web.controller.BaseController;
import com.platform.common.web.domain.AjaxResult;
import com.platform.common.web.page.TableDataInfo;
import com.platform.system.dcJobInstance.domain.Instance;
import com.platform.system.dcJobInstance.service.IInstanceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 岗位信息操作处理
 *
 * @author AllDataDC
 */
@Controller
@RequestMapping("/dcJobInstance")
public class InstanceController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(InstanceController.class);

    private String prefix = "compare/dcJobInstance";

    @Autowired
    private IInstanceService instanceService;

    @GetMapping()
    public String index() {
        return prefix + "/index";
    }

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Instance Instance) {
        startPage();
        List<Instance> list = instanceService.selectInstanceList(Instance);
        return getDataTable(list);
    }

    @Log(title = "instance管理", businessType = BusinessType.INSERT)
    @PostMapping("/insert")
    @ResponseBody
    public AjaxResult insert(Instance instance) {
        try {
            return toAjax(instanceService.insertInstance(instance));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 新增保存
     */
    @Log(title = "add", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated Instance Instance) {
        try {
            return toAjax(instanceService.insertInstance(Instance));
        } catch (Exception e) {
            e.printStackTrace();
            return error(e.getMessage());
        }
    }

    @GetMapping("/detail/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        Instance instance = instanceService.selectInstanceById(id);
        boolean numFlag = instance.getPvDiff().equals("0") && instance.getUvDiff().equals("0");
        boolean consistencyFlag = instance.getCountDiff().equals(instance.getOriginTableCount()) &&
                instance.getCountDiff().equals(instance.getToTableCount());
        mmap.put("Instance", instance);
        mmap.put("numFlag", numFlag);
        mmap.put("consistencyFlag", consistencyFlag);
        return prefix + "/detail";
    }

    @GetMapping("/getDiffDetail/{id}")
    @ResponseBody
    public TableDataInfo getDiffDetail(@PathVariable("id") Long id) throws Exception {
        startPage();
        List<LinkedHashMap<String, String>> list = instanceService.getDiffDetail(id);
        return getDataTable(list);
    }

    /**
     * 查看差异case
     */
    @GetMapping("/diffDetail/{id}")
    public String diffDetail(@PathVariable("id") Long id) {
        return prefix + "/diffDetail";
    }

}
