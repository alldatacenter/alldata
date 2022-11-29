package cn.datax.service.data.market.integration.controller;

import cn.datax.service.data.market.api.dto.ServiceExecuteDto;
import cn.datax.service.data.market.integration.service.ServiceExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ServiceExecuteController {

    @Autowired
    private ServiceExecuteService serviceExecuteService;

    @PostMapping("/execute")
    public Object execute(@RequestBody ServiceExecuteDto serviceExecuteDto, HttpServletRequest request) {
        Object obj = serviceExecuteService.execute(serviceExecuteDto, request);
        return obj;
    }
}
