package cn.datax.service.data.standard.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.R;
import cn.datax.service.data.standard.api.dto.ManualMappingDto;
import cn.datax.service.data.standard.service.DictMappingService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = {"字典对照映射"})
@RestController
@RequestMapping("/mappings")
public class DictMappingController extends BaseController {

    @Autowired
    private DictMappingService dictMappingService;

    @GetMapping("/{id}")
    public R getDictMapping(@PathVariable String id) {
        Map<String, Object> map = dictMappingService.getDictMapping(id);
        return R.ok().setData(map);
    }

    @PostMapping("/auto/{id}")
    public R dictAutoMapping(@PathVariable String id) {
        dictMappingService.dictAutoMapping(id);
        return R.ok();
    }

    @PostMapping("/manual")
    public R dictManualMapping(@RequestBody @Validated ManualMappingDto manualMappingDto) {
        dictMappingService.dictManualMapping(manualMappingDto);
        return R.ok();
    }

    @PostMapping("/cancel/{id}")
    public R dictCancelMapping(@PathVariable String id) {
        dictMappingService.dictCancelMapping(id);
        return R.ok();
    }
}
