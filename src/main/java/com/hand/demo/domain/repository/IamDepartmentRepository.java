package com.hand.demo.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.IamDepartment;

import java.util.List;

/**
 * (IamDepartment)资源库
 *
 * @author Zamzam
 * @since 2024-12-17 10:22:16
 */
public interface IamDepartmentRepository extends BaseRepository<IamDepartment> {
    /**
     * 查询
     *
     * @param iamDepartment 查询条件
     * @return 返回值
     */
    List<IamDepartment> selectList(IamDepartment iamDepartment);

    /**
     * 根据主键查询（可关联表）
     *
     * @param departmentId 主键
     * @return 返回值
     */
    IamDepartment selectByPrimary(Long departmentId);
}
