package com.platform.system.dcDict.service;

import java.util.List;
import com.platform.system.dcDict.domain.DictData;

/**
 * 字典 业务层
 * 
 * @author AllDataDC
 */
public interface IDictDataService
{
    /**
     * 根据条件分页查询字典数据
     * 
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    public List<DictData> selectDictDataList(DictData dictData);

    /**
     * 根据字典类型和字典键值查询字典数据信息
     * 
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    public String selectDictLabel(String dictType, String dictValue);

    /**
     * 根据字典数据ID查询信息
     * 
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    public DictData selectDictDataById(Long dictCode);

    /**
     * 批量删除字典数据
     * 
     * @param ids 需要删除的数据
     */
    public void deleteDictDataByIds(String ids);

    /**
     * 新增保存字典数据信息
     * 
     * @param dictData 字典数据信息
     * @return 结果
     */
    public int insertDictData(DictData dictData);

    /**
     * 修改保存字典数据信息
     * 
     * @param dictData 字典数据信息
     * @return 结果
     */
    public int updateDictData(DictData dictData);
}
