
package com.platform.modules.system.repository;

import com.platform.modules.system.domain.Dept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Set;

/**
* @author AllDataDC
* @date 2023-01-27
*/
public interface DeptRepository extends JpaRepository<Dept, Long>, JpaSpecificationExecutor<Dept> {

    /**
     * 根据 PID 查询
     * @param id pid
     * @return /
     */
    List<Dept> findByPid(Long id);

    /**
     * 获取顶级部门
     * @return /
     */
    List<Dept> findByPidIsNull();

    /**
     * 根据角色ID 查询
     * @param roleId 角色ID
     * @return /
     */
    @Query(value = "select d.* from sys_dept d, sys_roles_depts r where " +
            "d.dept_id = r.dept_id and r.role_id = ?1", nativeQuery = true)
    Set<Dept> findByRoleId(Long roleId);

    /**
     * 判断是否存在子节点
     * @param pid /
     * @return /
     */
    int countByPid(Long pid);

    /**
     * 根据ID更新sub_count
     * @param count /
     * @param id /
     */
    @Modifying
    @Query(value = " update sys_dept set sub_count = ?1 where dept_id = ?2 ",nativeQuery = true)
    void updateSubCntById(Integer count, Long id);
}