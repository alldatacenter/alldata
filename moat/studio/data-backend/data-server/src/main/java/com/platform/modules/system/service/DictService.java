
package com.platform.modules.system.service;

import com.platform.modules.system.domain.Dict;
import com.platform.modules.system.service.dto.DictDto;
import com.platform.modules.system.service.dto.DictQueryCriteria;
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
public interface DictService {

    /**
     * 分页查询
     * @param criteria 条件
     * @param pageable 分页参数
     * @return /
     */
    Map<String,Object> queryAll(DictQueryCriteria criteria, Pageable pageable);

    /**
     * 查询全部数据
     * @param dict /
     * @return /
     */
    List<DictDto> queryAll(DictQueryCriteria dict);

    /**
     * 创建
     * @param resources /
     * @return /
     */
    void create(Dict resources);

    /**
     * 编辑
     * @param resources /
     */
    void update(Dict resources);

    /**
     * 删除
     * @param ids /
     */
    void delete(Set<Long> ids);

    /**
     * 导出数据
     * @param queryAll 待导出的数据
     * @param response /
     * @throws IOException /
     */
    void download(List<DictDto> queryAll, HttpServletResponse response) throws IOException;
}