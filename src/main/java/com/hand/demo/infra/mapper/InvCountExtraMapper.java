package com.hand.demo.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvCountExtra;

import java.util.List;

/**
 * (InvCountExtra)应用服务
 *
 * @author Zamzam
 * @since 2024-12-17 10:22:48
 */
public interface InvCountExtraMapper extends BaseMapper<InvCountExtra> {
    /**
     * 基础查询
     *
     * @param invCountExtra 查询条件
     * @return 返回值
     */
    List<InvCountExtra> selectList(InvCountExtra invCountExtra);
}

