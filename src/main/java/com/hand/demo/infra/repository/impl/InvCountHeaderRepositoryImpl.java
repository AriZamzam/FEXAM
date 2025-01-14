package com.hand.demo.infra.repository.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.UserDTO;
import org.apache.commons.collections.CollectionUtils;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.domain.repository.InvCountHeaderRepository;
import com.hand.demo.infra.mapper.InvCountHeaderMapper;

import javax.annotation.Resource;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvCountHeader)资源库
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:01
 */
@Component
public class InvCountHeaderRepositoryImpl extends BaseRepositoryImpl<InvCountHeader> implements InvCountHeaderRepository {
    @Resource
    private InvCountHeaderMapper invCountHeaderMapper;

    @Override
    @ProcessCacheValue
    public List<InvCountHeaderDTO> selectList(InvCountHeaderDTO invCountHeader) {
        List<InvCountHeaderDTO> invCountHeaderDTOList = invCountHeaderMapper.selectList(invCountHeader);
        //convert ids to list
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaderDTOList) {
            //region V1.0 [S]
//            List<String> counters = Arrays.asList(invCountHeaderDTO.getCounterIds().split(","));
//            List<UserDTO> userDTOList = new ArrayList<>();
//            for (String count : counters) {
//                UserDTO userDTO = new UserDTO();
//                userDTO.setUserId(Long.parseLong(count));
//                userDTOList.add(userDTO);
//            }
//            invCountHeaderDTO.setCounterList(userDTOList);
//            List<String> supervisors = Arrays.asList(invCountHeaderDTO.getSupervisorIds().split(","));
//            userDTOList = new ArrayList<>();
//            for (String supervise : supervisors) {
//                UserDTO userDTO = new UserDTO();
//                userDTO.setUserId(Long.parseLong(supervise));
//                userDTOList.add(userDTO);
//            }
//            invCountHeaderDTO.setSupervisorList(userDTOList);
            //endregion V1.0 [E]
            //region V1.1 [S]
            // Parse and set Counter List
            List<UserDTO> counterList = Arrays.stream(invCountHeaderDTO.getCounterIds().split(","))
                    .map(Long::parseLong)
                    .map(userId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setUserId(userId);
                        return userDTO;
                    })
                    .collect(Collectors.toList());
            invCountHeaderDTO.setCounterList(counterList);

            // Parse and set Supervisor List
            List<UserDTO> supervisorList = Arrays.stream(invCountHeaderDTO.getSupervisorIds().split(","))
                    .map(Long::parseLong)
                    .map(userId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setUserId(userId);
                        return userDTO;
                    })
                    .collect(Collectors.toList());
            invCountHeaderDTO.setSupervisorList(supervisorList);
            //endregion V1.1 [E]

        }
        return invCountHeaderDTOList;
    }

    @Override
    public InvCountHeaderDTO selectByPrimary(Long countHeaderId) {
        InvCountHeaderDTO invCountHeader = new InvCountHeaderDTO();
        invCountHeader.setCountHeaderId(countHeaderId);
//        List<InvCountHeaderDTO > invCountHeaders = invCountHeaderMapper.selectList(invCountHeader);
        List<InvCountHeaderDTO> invCountHeaders = this.selectList(invCountHeader);
        if (invCountHeaders.size() == 0) {
            return null;
        }
        return invCountHeaders.get(0);
    }

    @Override
    @ProcessCacheValue
    public List<InvCountHeaderDTO> selectReport(InvCountHeaderDTO invCountHeader) {
        List<InvCountHeaderDTO> invCountHeaderDTOList = invCountHeaderMapper.selectReport(invCountHeader);

        //convert ids to list
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaderDTOList) {
            //region V1.0
//            List<String> counters = Arrays.asList(invCountHeaderDTO.getCounterIds().split(","));
//            List<UserDTO> userDTOList = new ArrayList<>();
//            for (String count : counters) {
//                UserDTO userDTO = new UserDTO();
//                userDTO.setUserId(Long.parseLong(count));
//                userDTOList.add(userDTO);
//            }
//            invCountHeaderDTO.setCounterList(userDTOList);
//            List<String> supervisors = Arrays.asList(invCountHeaderDTO.getSupervisorIds().split(","));
//            userDTOList = new ArrayList<>();
//            for (String supervise : supervisors) {
//                UserDTO userDTO = new UserDTO();
//                userDTO.setUserId(Long.parseLong(supervise));
//                userDTOList.add(userDTO);
//            }
//            invCountHeaderDTO.setSupervisorList(userDTOList);
            //endregion V1.0
            //region V1.1 [S]
            // Parse and set Counter List
            List<UserDTO> counterList = Arrays.stream(invCountHeaderDTO.getCounterIds().split(","))
                    .map(Long::parseLong)
                    .map(userId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setUserId(userId);
                        return userDTO;
                    })
                    .collect(Collectors.toList());
            invCountHeaderDTO.setCounterList(counterList);

            // Parse and set Supervisor List
            List<UserDTO> supervisorList = Arrays.stream(invCountHeaderDTO.getSupervisorIds().split(","))
                    .map(Long::parseLong)
                    .map(userId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setUserId(userId);
                        return userDTO;
                    })
                    .collect(Collectors.toList());
            invCountHeaderDTO.setSupervisorList(supervisorList);
            //endregion V1.1 [E]
        }

        return invCountHeaderDTOList;
    }

}

