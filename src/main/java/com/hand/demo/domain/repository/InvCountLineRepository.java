package com.hand.demo.domain.repository;

import com.hand.demo.api.dto.InvCountLineDTO;
import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.InvCountLine;

import java.util.List;

/**
 * (InvCountLine)资源库
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:13
 */
public interface InvCountLineRepository extends BaseRepository<InvCountLine> {
    /**
     * 查询
     *
     * @param invCountLine 查询条件
     * @return 返回值
     */
    List<InvCountLineDTO> selectList(InvCountLineDTO invCountLine);

    /**
     * 根据主键查询（可关联表）
     *
     * @param countLineId 主键
     * @return 返回值
     */
    InvCountLine selectByPrimary(Long countLineId);
    List<InvCountLineDTO> selectLineReport(InvCountLineDTO invCountLine);
}
