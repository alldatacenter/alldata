package cn.datax.service.data.market.integration.service.impl;

import cn.datax.common.exception.DataException;
import cn.datax.service.data.market.api.dto.ServiceExecuteDto;
import cn.datax.service.data.market.api.entity.ServiceIntegrationEntity;
import cn.datax.service.data.market.api.enums.ReqMethod;
import cn.datax.service.data.market.api.enums.ServiceType;
import cn.datax.service.data.market.integration.dao.ServiceIntegrationDao;
import cn.datax.service.data.market.integration.service.ServiceExecuteService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ServiceExecuteServiceImpl implements ServiceExecuteService {

    @Autowired
    private ServiceIntegrationDao serviceIntegrationDao;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Object execute(ServiceExecuteDto execute, HttpServletRequest request) {
        // 密钥校验
        String serviceKey = request.getHeader("service_key");
        String secretKey = request.getHeader("secret_key");
        if (StrUtil.isBlank(serviceKey) || StrUtil.isBlank(secretKey)) {
            throw new DataException("service_key或secret_key空");
        }
        if (StrUtil.isBlank(execute.getServiceNo())) {
            throw new DataException("服务编号不能为空");
        }
        ServiceIntegrationEntity serviceIntegrationEntity = serviceIntegrationDao.selectOne(Wrappers.<ServiceIntegrationEntity>lambdaQuery()
                .eq(ServiceIntegrationEntity::getServiceNo, execute.getServiceNo()));
        if (serviceIntegrationEntity == null) {
            throw new DataException("找不到服务：" + execute.getServiceNo());
        }
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response = null;
        if (ServiceType.HTTP.getKey().equals(serviceIntegrationEntity.getServiceType())) {
            String header = execute.getHeader();
            String param = execute.getParam();
            if (StrUtil.isNotBlank(header)) {
                try {
                    Map<String, Object>  headerMap = objectMapper.readValue(header, new TypeReference<HashMap<String,Object>>(){});
                    Iterator<Map.Entry<String, Object>> headerEntries = headerMap.entrySet().iterator();
                    while(headerEntries.hasNext()){
                        Map.Entry<String, Object> entry = headerEntries.next();
                        headers.add(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                } catch (JsonProcessingException e) {
                    throw new DataException("请求头格式有问题");
                }
            }
            if (StrUtil.isNotBlank(param)) {
                try {
                    Map<String, Object>  paramMap = objectMapper.readValue(param, new TypeReference<HashMap<String,Object>>(){});
                    Iterator<Map.Entry<String, Object>> paramEntries = paramMap.entrySet().iterator();
                    while(paramEntries.hasNext()){
                        Map.Entry<String, Object> entry = paramEntries.next();
                        params.add(entry.getKey(), entry.getValue());
                    }
                } catch (JsonProcessingException e) {
                    throw new DataException("请求参数格式有问题");
                }
            }
            // http服务
            HttpMethod httpMethod = HttpMethod.GET;
            if (ReqMethod.POST.getDesc().equals(serviceIntegrationEntity.getHttpService().getHttpMethod())) {
                httpMethod = HttpMethod.POST;
            }
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(params, headers);
            try {
                response = restTemplate.exchange(serviceIntegrationEntity.getHttpService().getUrl(), httpMethod, httpEntity, String.class);
            } catch (Exception e) {
                throw new DataException(e.getMessage());
            }
        } else if (ServiceType.WEBSERVICE.getKey().equals(serviceIntegrationEntity.getServiceType())) {
            // webservice服务
            headers.add("SOAPAction", serviceIntegrationEntity.getWebService().getTargetNamespace() + serviceIntegrationEntity.getWebService().getMethod());
            headers.add("Content-Type", "text/xml;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(execute.getSoap(), headers);
            try {
                response = restTemplate.exchange(serviceIntegrationEntity.getWebService().getWsdl(), HttpMethod.POST, entity, String.class);
            } catch (Exception e) {
                throw new DataException(e.getMessage());
            }
        }
        return response.getBody();
    }
}
