package com.platform.system.dcJobConfig.service;

import com.platform.system.dcJobConfig.domain.Jobconfig;

import java.util.List;

/**
 * 岗位信息 服务层
 * 
 * @author AllDataDC
 */
public interface IJobconfigService
{
    public List<Jobconfig> selectJobconfigList(Jobconfig dbconfig);

    public List<Jobconfig> selectJobconfigAll();

    /*public List<Jobconfig> selectJobconfigsByUserId(Long userId);*/

    public Jobconfig selectJobconfigById(Long id);

    public List<String> selectDbTypesAll();

    public int deleteJobconfigByIds(String ids);

    public int insertJobconfig(Jobconfig dbconfig);

    public int updateJobconfig(Jobconfig dbconfig);

    public int countUserPostById(Long postId);

    public void checkTableName(Jobconfig jobconfig) throws Exception;

}
