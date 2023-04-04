package com.vince.xq.project.system.jobconfig.mapper;

import com.vince.xq.project.system.jobconfig.domain.Jobconfig;

import java.util.List;


public interface JobconfigMapper
{

    public List<Jobconfig> selectJobconfigList(Jobconfig dbconfig);

    public List<Jobconfig> selectJobconfigAll();

    public Jobconfig selectJobconfigById(Long id);

    public int deleteJobconfigByIds(Long[] ids);

    public int updateJobconfig(Jobconfig dbconfig);

    public int insertJobconfig(Jobconfig dbconfig);


    public List<Jobconfig> selectJobconfigsByUser(String createBy);
}
