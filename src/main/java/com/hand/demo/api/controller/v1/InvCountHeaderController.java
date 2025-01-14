package com.hand.demo.api.controller.v1;

import com.hand.demo.api.dto.*;
import com.hand.demo.domain.entity.InvBatch;
import com.hand.demo.domain.entity.InvCountLine;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.base.BaseController;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import com.hand.demo.app.service.InvCountHeaderService;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.domain.repository.InvCountHeaderRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvCountHeader)表控制层
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:01
 */

@RestController("invCountHeaderController.v1")
@RequestMapping("/v1/{organizationId}/inv-count-headers")
public class InvCountHeaderController extends BaseController {

    @Autowired
    private InvCountHeaderRepository invCountHeaderRepository;

    @Autowired
    private InvCountHeaderService invCountHeaderService;

    @ApiOperation(value = "list")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public ResponseEntity<Page<InvCountHeaderDTO>> list(InvCountHeaderDTO invCountHeader, @PathVariable Long organizationId,
                                                        @ApiIgnore @SortDefault(value = InvCountHeader.FIELD_CREATION_DATE,
                                                                direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvCountHeaderDTO> list = invCountHeaderService.selectList(pageRequest, invCountHeader);
        return Results.success(list);
    }

    @ApiOperation(value = "detail")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{countHeaderId}/detail")
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @ProcessCacheValue
    public ResponseEntity<InvCountHeaderDTO> detail(@PathVariable @SortDefault(value = InvCountHeader.FIELD_CREATION_DATE, direction = Sort.Direction.DESC) Long countHeaderId) {
        InvCountHeaderDTO invCountHeader = invCountHeaderService.selectDetail(countHeaderId);
        return Results.success(invCountHeader);
    }

    @ApiOperation(value = "orderSave")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/orderSave")
    public ResponseEntity<List<InvCountHeaderDTO>> orderSave(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
        //check if line exist and validate [S]
        for (InvCountHeaderDTO header : invCountHeaders){
            if (!CollectionUtils.isEmpty(header.getCountOrderLineList())){
                validList(header.getCountOrderLineList(), InvCountLine.ValidLineClass.class);
            }
        }
        //check if line exist and validate [E]
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
        return Results.success(invCountHeaderService.orderSave(invCountHeaders));
    }

    @ApiOperation(value = "manualSave")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/manualsave")
    public ResponseEntity<List<InvCountHeaderDTO>> manualSave(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
//        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
        return Results.success(invCountHeaderService.manualSave(invCountHeaders));
    }

    @ApiOperation(value = "manualSaveCheck")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/manualSaveCheck")
    public ResponseEntity<InvCountInfoDTO> manualSaveCheck(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
        return Results.success(invCountHeaderService.manualSaveCheck(invCountHeaders));
    }

    @ApiOperation(value = "checkAndRemove")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> checkAndRemove(@RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        SecurityTokenHelper.validToken(invCountHeaders);
        return Results.success(invCountHeaderService.checkAndRemove(invCountHeaders));
    }

    @ApiOperation(value = "orderExecution")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/orderexecution")
    public ResponseEntity<InvCountInfoDTO> orderExecution(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
        return Results.success(invCountHeaderService.orderExecution(invCountHeaders));
    }

    @ApiOperation(value = "executeCheck")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/executecheck")
    public ResponseEntity<InvCountInfoDTO> executeCheck(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
        return Results.success(invCountHeaderService.executeCheck(invCountHeaders));
    }

    @ApiOperation(value = "execute")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/execute")
    public ResponseEntity<List<InvCountHeaderDTO>> execute(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
        return Results.success(invCountHeaderService.execute(invCountHeaders));
    }

    @ApiOperation(value = "countSyncWms")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/countSyncWms")
    public ResponseEntity<InvCountInfoDTO> countSyncWms(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.ValidateClass.class);
        return Results.success(invCountHeaderService.countSyncWms(invCountHeaders));
    }

    @ApiOperation(value = "countResultSync")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/countResultSync")
    public ResponseEntity<InvCountHeaderDTO> countResultSync(@PathVariable Long organizationId, @RequestBody InvCountHeaderDTO invCountHeaders) {
        try {
            validObject(invCountHeaders, InvCountHeader.ValidateRequestClass.class);
            validList(invCountHeaders.getCountOrderLineList(), InvCountLine.ValidRequestClass.class);
        } catch (Exception e) {
            String message = e.getMessage();
            invCountHeaders.setErrorMsg(message);
            invCountHeaders.setStatus("E");
            return Results.error(invCountHeaders);
        }
        return Results.success(invCountHeaderService.countResultSync(invCountHeaders));
    }

    @ApiOperation(value = "submitCheck")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/submitCheck")
    public ResponseEntity<InvCountInfoDTO> submitCheck(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validObject(invCountHeaders, InvCountHeader.ValidateClass.class);
        return Results.success(invCountHeaderService.submitCheck(invCountHeaders));
    }

    @ApiOperation(value = "submit")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/submit")
    public ResponseEntity<List<InvCountHeaderDTO>> submit(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validObject(invCountHeaders, InvCountHeader.ValidateClass.class);
        return Results.success(invCountHeaderService.submit(invCountHeaders));
    }

    @ApiOperation(value = "orderSubmit")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/orderSubmit")
    public ResponseEntity<List<InvCountHeaderDTO>> orderSubmit(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        return Results.success(invCountHeaderService.orderSubmit(invCountHeaders));
    }

    @ApiOperation(value = "countingOrderReportDs")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/countingOrderReportDs")
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public ResponseEntity<List<InvCountHeaderDTO>> countingOrderReportDs(@PathVariable Long organizationId, InvCountHeaderDTO invCountHeaders) {
        List<InvCountHeaderDTO> countReport = invCountHeaderService.countingOrderReportDs(organizationId, invCountHeaders);
        return Results.success(countReport);
    }

    @ApiOperation(value = "callBack")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/callBack")
    public ResponseEntity<?> callBack(@PathVariable Long organizationId, @RequestBody WorkFlowEventDTO workFlowDTO) {
        return Results.success(invCountHeaderService.callBack(workFlowDTO));
    }

}

