package com.vince.xq.project.system.dbconfig.service;

import com.vince.xq.project.system.dbconfig.domain.Dbconfig;

import java.util.List;

/**
 * 岗位信息 服务层
 * 
 * @author ruoyi
 */
public interface IDbconfigService
{
    public List<Dbconfig> selectDbconfigList(Dbconfig dbconfig);

    public List<Dbconfig> selectDbconfigAll();

    /*public List<Dbconfig> selectDbconfigsByUserId(Long userId);*/

    public Dbconfig selectDbconfigById(Long id);

    public List<String> selectDbTypesAll();

    public int deleteDbconfigByIds(String ids);

    public int insertDbconfig(Dbconfig dbconfig);

    public int updateDbconfig(Dbconfig dbconfig);

    public int countUserPostById(Long postId);

    public String checkConnectNameUnique(Dbconfig dbconfig);

    public void testConnection(Dbconfig dbconfig) throws Exception;

}
