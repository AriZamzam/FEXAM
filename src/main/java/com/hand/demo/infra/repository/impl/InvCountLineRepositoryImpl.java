package com.hand.demo.infra.repository.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountLineDTO;
import com.hand.demo.api.dto.UserDTO;
import org.apache.commons.collections.CollectionUtils;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvCountLine;
import com.hand.demo.domain.repository.InvCountLineRepository;
import com.hand.demo.infra.mapper.InvCountLineMapper;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * (InvCountLine)资源库
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:13
 */
@Component
public class InvCountLineRepositoryImpl extends BaseRepositoryImpl<InvCountLine> implements InvCountLineRepository {
    @Resource
    private InvCountLineMapper invCountLineMapper;

    @Override
    public List<InvCountLineDTO> selectList(InvCountLineDTO invCountLine) {
        List<InvCountLineDTO> invCountLineDTOList = invCountLineMapper.selectList(invCountLine);
        //convert ids to list
        for (InvCountLineDTO invCountLineDTO : invCountLineDTOList) {
            List<String> counters = Arrays.asList(invCountLineDTO.getCounterIds().split(","));
            List<UserDTO> userDTOList = new ArrayList<>();
            for (String count : counters) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(Long.parseLong(count));
                userDTOList.add(userDTO);
            }
            invCountLineDTO.setCounterList(userDTOList);
        }
        return invCountLineDTOList;
    }

    @Override
    public InvCountLineDTO selectByPrimary(Long countLineId) {
        InvCountLineDTO invCountLine = new InvCountLineDTO();
        invCountLine.setCountLineId(countLineId);
        List<InvCountLineDTO> invCountLines = selectList(invCountLine);
        if (invCountLines.size() == 0) {
            return null;
        }
        return invCountLines.get(0);
    }

    @Override
    @ProcessCacheValue
    public List<InvCountLineDTO> selectLineReport(InvCountLineDTO invCountLine) {
        //convert ids to list
        List<InvCountLineDTO> invCountLineDTOList = invCountLineMapper.selectLineReport(invCountLine);
        for (InvCountLineDTO invCountLineDTO : invCountLineDTOList) {
            List<String> counters = Arrays.asList(invCountLineDTO.getCounterIds().split(","));
            List<UserDTO> userDTOList = new ArrayList<>();
            for (String count : counters) {
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(Long.parseLong(count));
                userDTOList.add(userDTO);
            }
            invCountLineDTO.setCounterList(userDTOList);
        }
        return invCountLineDTOList;
    }

}

