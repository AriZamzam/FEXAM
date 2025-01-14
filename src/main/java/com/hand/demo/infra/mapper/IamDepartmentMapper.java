package com.hand.demo.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.IamDepartment;

import java.util.List;

/**
 * (IamDepartment)应用服务
 *
 * @author Zamzam
 * @since 2024-12-17 10:22:16
 */
public interface IamDepartmentMapper extends BaseMapper<IamDepartment> {
    /**
     * 基础查询
     *
     * @param iamDepartment 查询条件
     * @return 返回值
     */
    List<IamDepartment> selectList(IamDepartment iamDepartment);
}

