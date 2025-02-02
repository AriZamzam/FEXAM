package com.hand.demo.infra.repository.impl;

import com.hand.demo.api.dto.InvStockDTO;
import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvStock;
import com.hand.demo.domain.repository.InvStockRepository;
import com.hand.demo.infra.mapper.InvStockMapper;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * (InvStock)资源库
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:41
 */
@Component
public class InvStockRepositoryImpl extends BaseRepositoryImpl<InvStock> implements InvStockRepository {
    @Resource
    private InvStockMapper invStockMapper;

    @Override
    public List<InvStock> selectList(InvStock invStock) {
        return invStockMapper.selectList(invStock);
    }

    @Override
    public InvStock selectByPrimary(Long stockId) {
        InvStock invStock = new InvStock();
        invStock.setStockId(stockId);
        List<InvStock> invStocks = invStockMapper.selectList(invStock);
        if (invStocks.size() == 0) {
            return null;
        }
        return invStocks.get(0);
    }

    @Override
    public List<InvStockDTO> selectSum(InvStockDTO invStockDTO) {
        return invStockMapper.selectSum(invStockDTO);
    }

}

