package com.hand.demo.infra.mapper;

import com.hand.demo.api.dto.InvStockDTO;
import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvStock;

import java.util.List;

/**
 * (InvStock)应用服务
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:41
 */
public interface InvStockMapper extends BaseMapper<InvStock> {
    /**
     * 基础查询
     *
     * @param invStock 查询条件
     * @return 返回值
     */
    List<InvStock> selectList(InvStock invStock);
    List<InvStockDTO> selectSum(InvStockDTO invStockDTO);
}

