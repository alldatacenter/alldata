package com.alibaba.tesla.appmanager.trait.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.TraitProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.domain.dto.TraitDTO;
import com.alibaba.tesla.appmanager.domain.req.trait.TraitQueryReq;
import com.alibaba.tesla.appmanager.domain.req.trait.TraitReconcileReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * Trait 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/traits")
@RestController
public class TraitController extends BaseController {

    @Autowired
    private TraitProvider trait;

    /**
     * @api {post} /traits 应用并更新 Trait
     * @apiName PostTrait
     * @apiGroup Trait API
     * @apiDescription BODY 为 Trait YAML 内容
     * @apiHeader {String} Content-Type text/plain
     *
     * @apiSuccessExample
     * {
     *     "code": 200,
     *     "message": "SUCCESS",
     *     "data": {}
     * }
     */
    @PostMapping("")
    @ResponseBody
    public TeslaBaseResult apply(@RequestBody String request) {
        trait.apply(request, "SYSTEM");
        return buildSucceedResult(new HashMap<String, String>());
    }

    /**
     * @api {get} /traits 获取 Trait 列表
     * @apiName GetTraitList
     * @apiGroup Trait API
     *
     * @apiParam (GET Parameters) {String} name 过滤条件: Trait 名称
     * @apiParam (GET Parameters) {String} className 过滤条件: 类名称
     * @apiParam (GET Parameters) {String} definitionRef 过滤条件: 定义 Schema 引用对象
     * @apiParam (GET Parameters) {Number} page 当前页
     * @apiParam (GET Parameters) {Number} pageSize 每页大小
     *
     * @apiSuccessExample 示例返回
     * {
     *     "code": 200,
     *     "message": "SUCCESS",
     *     "data": {
     *         "page": 1,
     *         "pageSize": 2,
     *         "total": 11,
     *         "items": [
     *             {
     *                 "name": "applyStsSecret.flyadmin.alibaba.com",
     *                 "className": "com.alibaba.tesla.appmanager.trait.plugin.ApplyStsSecretTrait",
     *                 "definitionRef": "applyStsSecret.abmchart.schema.abm.io",
     *                 "traitDefinition": null,
     *                 "label": "Apply StsSecret Trait"
     *             },
     *             {
     *                 "name": "extraNamespace.flyadmin.alibaba.com",
     *                 "className": "com.alibaba.tesla.appmanager.trait.plugin.ExtraNamespaceTrait",
     *                 "definitionRef": "extraNamespace.abmchart.schema.abm.io",
     *                 "traitDefinition": null,
     *                 "label": "Extra NameSpace Trait"
     *             }
     *         ],
     *         "empty": false
     *     }
     * }
     */
    @GetMapping("")
    public TeslaBaseResult query(@ModelAttribute TraitQueryReq request) {
        return buildSucceedResult(trait.list(request, "SYSTEM"));
    }

    /**
     * @api {get} /traits/:name 获取指定的 Trait
     * @apiName GetTrait
     * @apiGroup Trait API
     *
     * @apiParam (Path Parameters) {String} name Trait 名称
     *
     * @apiSuccessExample 示例返回
     * {
     *     "code": 200,
     *     "message": "SUCCESS",
     *     "data": {
     *         "name": "applyStsSecret.flyadmin.alibaba.com",
     *         "className": "com.alibaba.tesla.appmanager.trait.plugin.ApplyStsSecretTrait",
     *         "definitionRef": "applyStsSecret.abmchart.schema.abm.io",
     *         "traitDefinition": null,
     *         "label": "Apply StsSecret Trait"
     *     }
     * }
     */
    // 获取指定的 Trait
    @GetMapping("/{name:.+}")
    @ResponseBody
    public TeslaBaseResult get(@PathVariable("name") String name) {
        TraitQueryReq req = TraitQueryReq.builder().name(name).build();
        TraitDTO response = trait.get(req, "SYSTEM");
        return buildSucceedResult(response);
    }

    /**
     * @api {delete} /traits/:name 删除指定的 Trait
     * @apiName DeleteTrait
     * @apiGroup Trait API
     *
     * @apiParam (Path Parameters) {String} name Trait 名称
     *
     * @apiSuccessExample 示例返回
     * {
     *     "code": 200,
     *     "message": "SUCCESS",
     *     "data": 1
     * }
     */
    // 删除指定的 Trait
    @DeleteMapping("/{name:.+}")
    @ResponseBody
    public TeslaBaseResult delete(@PathVariable("name") String name) {
        TraitQueryReq req = TraitQueryReq.builder().name(name).build();
        int response = trait.delete(req, "SYSTEM");
        return buildSucceedResult(response);
    }

    /**
     * @api {post} /traits/reconcile Kubernetes Reconcile Callback API
     * @apiName PostTraitReconcile
     * @apiGroup Trait API
     *
     * @apiParam (Path Parameters) {String} name Trait 名称
     *
     * @apiSuccessExample 示例返回
     * {
     *     "code": 200,
     *     "message": "SUCCESS",
     *     "data": {}
     * }
     */
    // 获取指定的 Trait
    @PostMapping("/reconcile")
    @ResponseBody
    public TeslaBaseResult reconcile(@RequestBody TraitReconcileReq request) {
        JSONObject properties = request.getProperties();
        if (properties == null) {
            return buildClientErrorResult("properties cannot be null");
        }
        String traitName = properties.getString("traitName");
        if (StringUtils.isEmpty(traitName)) {
            return buildClientErrorResult("traitName is required in properties");
        }
        JSONObject object = request.getObject();
        trait.reconcile(traitName, object, properties);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }

    /**
     * @api {post} /traits/:name/reconcile Kubernetes Reconcile Callback API
     * @apiName PostTraitReconcile
     * @apiGroup Trait API
     *
     * @apiParam (Path Parameters) {String} name Trait 名称
     *
     * @apiSuccessExample 示例返回
     * {
     *     "code": 200,
     *     "message": "SUCCESS",
     *     "data": {}
     * }
     */
    // 获取指定的 Trait
    @PostMapping("/{name:.+}/reconcile")
    @ResponseBody
    public TeslaBaseResult reconcile(@PathVariable("name") String name, @RequestBody String request) {
        JSONObject payload = JSONObject.parseObject(request);
        trait.reconcile(name, payload);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
