
package com.platform.modules.mnt.service.impl;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import com.platform.modules.mnt.domain.DeployHistory;
import com.platform.modules.mnt.repository.DeployHistoryRepository;
import com.platform.modules.mnt.service.DeployHistoryService;
import com.platform.modules.mnt.service.dto.DeployHistoryDto;
import com.platform.modules.mnt.service.dto.DeployHistoryQueryCriteria;
import com.platform.modules.mnt.service.mapstruct.DeployHistoryMapper;
import com.platform.utils.FileUtil;
import com.platform.utils.PageUtil;
import com.platform.utils.QueryHelp;
import com.platform.utils.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Service
@RequiredArgsConstructor
public class DeployHistoryServiceImpl implements DeployHistoryService {

    private final DeployHistoryRepository deployhistoryRepository;
    private final DeployHistoryMapper deployhistoryMapper;

    @Override
    public Object queryAll(DeployHistoryQueryCriteria criteria, Pageable pageable){
        Page<DeployHistory> page = deployhistoryRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root,criteria,criteriaBuilder),pageable);
        return PageUtil.toPage(page.map(deployhistoryMapper::toDto));
    }

    @Override
    public List<DeployHistoryDto> queryAll(DeployHistoryQueryCriteria criteria){
        return deployhistoryMapper.toDto(deployhistoryRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root,criteria,criteriaBuilder)));
    }

    @Override
    public DeployHistoryDto findById(String id) {
        DeployHistory deployhistory = deployhistoryRepository.findById(id).orElseGet(DeployHistory::new);
        ValidationUtil.isNull(deployhistory.getId(),"DeployHistory","id",id);
        return deployhistoryMapper.toDto(deployhistory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DeployHistory resources) {
        resources.setId(IdUtil.simpleUUID());
        deployhistoryRepository.save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<String> ids) {
        for (String id : ids) {
            deployhistoryRepository.deleteById(id);
        }
    }

    @Override
    public void download(List<DeployHistoryDto> queryAll, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DeployHistoryDto deployHistoryDto : queryAll) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("部署编号", deployHistoryDto.getDeployId());
            map.put("应用名称", deployHistoryDto.getAppName());
            map.put("部署IP", deployHistoryDto.getIp());
            map.put("部署时间", deployHistoryDto.getDeployDate());
            map.put("部署人员", deployHistoryDto.getDeployUser());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
