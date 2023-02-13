package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.model.SwitchViewUserDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.SwitchViewUserService;
import com.alibaba.tesla.authproxy.web.input.SwitchViewUserParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 切换身份的用户列表维护 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Controller
@RequestMapping("switchView/users")
@Slf4j
public class SwitchViewUserController extends BaseController {

    @Autowired
    private SwitchViewUserService switchViewUserService;

    @Autowired
    private AuthPolicy authPolicy;

    /**
     * 获取全量的白名单用户
     *
     * @return
     */
    @GetMapping
    @ResponseBody
    public TeslaBaseResult getUsers() {
        List<SwitchViewUserDO> users = switchViewUserService.select();
        return TeslaResultFactory.buildSucceedResult(users);
    }

    /**
     * 获取指定用户的信息
     */
    @GetMapping(value = "{empId}")
    @ResponseBody
    public TeslaBaseResult getUser(@PathVariable("empId") String empId,
                                   @RequestParam("switchEmpId") String switchEmpId) throws Exception {
        SwitchViewUserDO user = switchViewUserService.getByEmpId(empId);
        if (user == null) {
            return TeslaResultFactory.buildForbiddenResult();
        }
        UserDO switchUser = authPolicy.getAuthServiceManager().getUserByEmpId(switchEmpId);
        SwitchViewUserDetail data = SwitchViewUserDetail.builder()
            .fromEmpId(user.getEmpId())
            .fromLoginName(user.getLoginName())
            .fromBucId(user.getBucId())
            .toEmpId(switchUser.getEmpId())
            .toLoginName(switchUser.getLoginName())
            .toBucId(String.valueOf(switchUser.getBucId()))
            .build();
        return TeslaResultFactory.buildSucceedResult(data);
    }

    /**
     * 增加白名单用户
     *
     * @return
     */
    @PostMapping
    @ResponseBody
    public TeslaBaseResult addUser(@Valid @RequestBody SwitchViewUserParam param,
                                   BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        switchViewUserService.addUser(param.getEmpId());
        return TeslaResultFactory.buildSucceedResult();
    }

    /**
     * 删除白名单用户
     *
     * @return
     */
    @DeleteMapping(value = "{empId}")
    @ResponseBody
    public TeslaBaseResult deleteUser(@PathVariable("empId") String empId) throws Exception {
        switchViewUserService.deleteUser(empId);
        return TeslaResultFactory.buildSucceedResult();
    }
}


@Data
@Builder
class SwitchViewUserDetail {

    private String fromEmpId;

    private String fromLoginName;

    private String fromBucId;

    private String toEmpId;

    private String toLoginName;

    private String toBucId;
}