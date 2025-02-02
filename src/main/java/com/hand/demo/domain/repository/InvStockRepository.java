package com.hand.demo.domain.repository;

import com.hand.demo.api.dto.InvStockDTO;
import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.InvStock;

import java.util.List;

/**
 * (InvStock)资源库
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:41
 */
public interface InvStockRepository extends BaseRepository<InvStock> {
    /**
     * 查询
     *
     * @param invStock 查询条件
     * @return 返回值
     */
    List<InvStock> selectList(InvStock invStock);

    /**
     * 根据主键查询（可关联表）
     *
     * @param stockId 主键
     * @return 返回值
     */
    InvStock selectByPrimary(Long stockId);

    List<InvStockDTO> selectSum(InvStockDTO invStockDTO);
}
