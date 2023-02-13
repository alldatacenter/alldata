package cn.datax.service.data.masterdata.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.data.masterdata.api.entity.ModelDataEntity;
import cn.datax.service.data.masterdata.api.query.ModelDataQuery;
import cn.datax.service.data.masterdata.service.ModelDataService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/datas")
public class ModelDataController extends BaseController {

    @Autowired
    private ModelDataService modelDataService;

    @GetMapping("/{id}")
    public R getModelDataById(@PathVariable String id, ModelDataEntity modelDataEntity) {
        Map<String, Object> data = modelDataService.getModelDataById(modelDataEntity);
        return R.ok().setData(data);
    }

    @PostMapping("/page")
    public R getPageModelDatas(@RequestBody ModelDataQuery modelDataQuery) {
        IPage<Map<String, Object>> page = modelDataService.getPageModelDatas(modelDataQuery);
        JsonPage<Map<String, Object>> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return R.ok().setData(jsonPage);
    }

    @PostMapping("/addData")
    public R addModelData(@RequestBody ModelDataEntity modelDataEntity) {
        modelDataService.addModelData(modelDataEntity);
        return R.ok();
    }

    @PutMapping("/updateData/{id}")
    public R updateModelData(@PathVariable String id, @RequestBody ModelDataEntity modelDataEntity) {
        modelDataService.updateModelData(modelDataEntity);
        return R.ok();
    }

    @PostMapping("/delData/{id}")
    public R deleteModelData(@PathVariable String id, @RequestBody ModelDataEntity modelDataEntity) {
        modelDataService.delModelData(modelDataEntity);
        return R.ok();
    }
}
