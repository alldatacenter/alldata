package cn.datax.service.data.market.integration.service;

import cn.datax.service.data.market.api.dto.ServiceExecuteDto;

import javax.servlet.http.HttpServletRequest;

public interface ServiceExecuteService {

    Object execute(ServiceExecuteDto serviceExecuteDto, HttpServletRequest request);
}
