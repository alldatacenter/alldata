
package com.platform.modules.system.service;

import com.platform.modules.system.domain.Job;
import com.platform.modules.system.service.dto.JobDto;
import com.platform.modules.system.service.dto.JobQueryCriteria;
import org.springframework.data.domain.Pageable;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author AllDataDC
* @date 2023-01-27
*/
public interface JobService {

    /**
     * 根据ID查询
     * @param id /
     * @return /
     */
    JobDto findById(Long id);

    /**
     * 创建
     * @param resources /
     * @return /
     */
    void create(Job resources);

    /**
     * 编辑
     * @param resources /
     */
    void update(Job resources);

    /**
     * 删除
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * 分页查询
     * @param criteria 条件
     * @param pageable 分页参数
     * @return /
     */
    Map<String,Object> queryAll(JobQueryCriteria criteria, Pageable pageable);

    /**
     * 查询全部数据
     * @param criteria /
     * @return /
     */
    List<JobDto> queryAll(JobQueryCriteria criteria);

    /**
     * 导出数据
     * @param queryAll 待导出的数据
     * @param response /
     * @throws IOException /
     */
    void download(List<JobDto> queryAll, HttpServletResponse response) throws IOException;

    /**
     * 验证是否被用户关联
     * @param ids /
     */
    void verification(Set<Long> ids);
}