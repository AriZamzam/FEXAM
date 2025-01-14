package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hzero.boot.workflow.dto.RunTaskHistory;
import org.hzero.common.HZeroCacheKey;
import org.hzero.core.cache.CacheValue;
import org.hzero.core.cache.Cacheable;

import java.util.List;

@Getter
@Setter
public class InvCountHeaderDTO extends InvCountHeader implements Cacheable {
    @ApiModelProperty(value = "Error Message")
    private String errorMsg;

    private List<InvCountLineDTO> countOrderLineList;

    private List<InvMaterial> snapshotMaterialList;
    private List<InvBatch> snapshotBatchList;
    private String countTimeStr;
    private String employeeNumber = "47835";

    private List<UserDTO> counterList;
    private List<UserDTO> supervisorList;

    private boolean tenantAdminFlag = false;

    private String status;

    //for report
    private List<String> docStatus;
    private String companyCode;
    private String departmentCode;
    private String warehouseCode;

    @CacheValue(key = "hiam:tenant", primaryKey = "tenantId", searchKey = "tenantNum",
            structure = CacheValue.DataStructure.MAP_OBJECT)
    private String tenantCode;
    private IamDepartment department;
    private InvWarehouse warehouse;
    private List<RunTaskHistory> historyList;
    private String countDimensionMeaning;
    private String countModeMeaning;
    private String countStatusMeaning;
    private String countTypeMeaning;
    private String counterNameListString;
    private String supervisorNameListString;
    private String materialListString;
    private String batchListString;
    private String departmentName;

    @CacheValue(key = HZeroCacheKey.USER, primaryKey = "createdBy", searchKey = "realName",
            structure = CacheValue.DataStructure.MAP_OBJECT)
    private String creatorName;


}
