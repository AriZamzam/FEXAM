package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvCountLine;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hzero.core.cache.Cacheable;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class InvCountLineDTO extends InvCountLine implements Cacheable {

    private String materialCode;
    private String materialName;
    private String baseUnitCode;
    private String batchCode;
    private String counterName;
    private List<UserDTO> counterList;
    private String supervisorIds;
    private boolean tenantAdminFlag = false;
}
