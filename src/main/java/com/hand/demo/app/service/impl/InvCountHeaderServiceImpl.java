package com.hand.demo.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hand.demo.api.dto.*;
import com.hand.demo.app.service.InvCountExtraService;
import com.hand.demo.app.service.InvCountLineService;
import com.hand.demo.domain.entity.*;
import com.hand.demo.domain.repository.*;
import com.hand.demo.infra.constant.Constants;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.domain.AuditDomain;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.util.StringUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.apaas.common.userinfo.domain.UserVO;
import org.hzero.boot.apaas.common.userinfo.infra.feign.IamRemoteService;
import org.hzero.boot.interfaces.sdk.dto.ResponsePayloadDTO;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.boot.platform.profile.ProfileClient;
import org.hzero.boot.workflow.WorkflowClient;
import org.hzero.boot.workflow.dto.RunInstance;
import org.hzero.boot.workflow.dto.RunTaskHistory;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.domian.Condition;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountHeaderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * (InvCountHeader)应用服务
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:01
 */
@Log4j2
@Service
public class InvCountHeaderServiceImpl implements InvCountHeaderService {
    //region Global Variable [S]
    @Autowired
    private InvCountHeaderRepository invCountHeaderRepository;

    @Autowired
    private RedisHelper redisHelper;

    @Autowired
    private LovAdapter lovAdapter;

    @Autowired
    private InvWarehouseRepository invWarehouseRepository;

    @Autowired
    private CodeRuleBuilder codeRuleBuilder;

    @Autowired
    private InvMaterialRepository invMaterialRepository;

    @Autowired
    private InvBatchRepository invBatchRepository;

    @Autowired
    private InvCountLineRepository invCountLineRepository;

    @Autowired
    private InvStockRepository invStockRepository;

    @Autowired
    private IamCompanyRepository iamCompanyRepository;

    @Autowired
    private IamDepartmentRepository iamDepartmentRepository;

    @Autowired
    private InvCountExtraRepository extraRepository;

    @Autowired
    private Utils utils;

    @Autowired
    private ProfileClient profileClient;

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private InvCountExtraRepository invCountExtraRepository;

    @Autowired
    private IamRemoteService iamRemoteService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    InvCountExtraService invCountExtraService;

    @Autowired
    InvCountLineService invCountLineService;
    //endregion Global Variable [E]

    @Override
    public Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeaderDTO invCountHeader) {
        invCountHeader.setTenantAdminFlag(Boolean.TRUE.equals(getTenantData().getTenantAdminFlag())); //Set tenant admin flag[S.E]
        return PageHelper.doPageAndSort(pageRequest, () -> invCountHeaderRepository.selectList(invCountHeader));
    }

    @Override
    public InvCountHeaderDTO selectDetail(Long countHeaderId) {
        InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(countHeaderId); //get header data[S.E]

        //Convert to DTO [S]
        InvCountHeaderDTO invCountHeaderDTO = new InvCountHeaderDTO();
        BeanUtils.copyProperties(invCountHeader, invCountHeaderDTO);
        //Convert to DTO [E]

        invCountHeaderDTO.setTenantAdminFlag(Boolean.TRUE.equals(getTenantData().getTenantAdminFlag())); //Set tenant admin flag[S.E]
        invCountHeaderDTO.setSnapshotMaterialList(invMaterialRepository.selectByIds(invCountHeaderDTO.getSnapshotMaterialIds())); //get snapshot material [S.E]
        invCountHeaderDTO.setSnapshotBatchList(invBatchRepository.selectByIds(invCountHeaderDTO.getSnapshotBatchIds()));//get snapshot batch [S.E]

        //get lines and do sorting [S]
        InvCountLineDTO invCountLine = (InvCountLineDTO) new InvCountLineDTO().setSupervisorIds(invCountHeaderDTO.getSupervisorIds()).setCountHeaderId(countHeaderId);
        List<InvCountLineDTO> invCountLineList = invCountLineRepository.selectList(invCountLine);
        invCountLineList.sort(Comparator.comparing(AuditDomain::getCreationDate));
        invCountHeaderDTO.setCountOrderLineList(invCountLineList);
        //get lines and do sorting [E]
        return invCountHeaderDTO;
    }

    @Override
    public List<InvCountHeaderDTO> orderSave(List<InvCountHeaderDTO> invCountHeaders) {
        //validation [S]
        InvCountInfoDTO result = manualSaveCheck(invCountHeaders);
        if (StringUtil.isNotEmpty(result.getTotalErrorMsg())) {
            throw new CommonException(result.getTotalErrorMsg());
        }
        //validation [E]
        this.manualSave(invCountHeaders);
        return invCountHeaders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<InvCountHeaderDTO> manualSave(List<InvCountHeaderDTO> invCountHeaders) {
        //set Default Status [S]
        Map<String, String> variableMap = new HashMap<>();
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            if (invCountHeaderDTO.getCountHeaderId() == null) {
                invCountHeaderDTO.setCountStatus(Constants.STATUS_DRAFT);
                variableMap.put("customSegment", invCountHeaderDTO.getTenantId().toString());
                invCountHeaderDTO.setCountNumber(codeRuleBuilder.generateCode(Constants.RULE_CODE_COUNT_NUMBER, variableMap));
                //set count time string by current localdate [S] //todo can be null (exception)
                if (StringUtil.isNotEmpty(invCountHeaderDTO.getCountTimeStr())) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                    if (invCountHeaderDTO.getCountType().equals("MONTH")) {
                        try {
                            // Parse the string to check if it matches the pattern
                            LocalDate.parse(invCountHeaderDTO.getCountTimeStr(), formatter);
                        } catch (DateTimeParseException e) {

                        }
                    } else {

                    }
                }
//                invCountHeaderDTO.setCountTimeStr(
//                        invCountHeaderDTO.getCountType().equals("MONTH") ? LocalDate.now().getYear() + "-" + LocalDate.now().getMonth() : String.valueOf(LocalDate.now().getYear()) //TODO not from current date but input
//                );
                //set count time string by current localdate [E]
            }
        }
        //set Default Status [E]
//        generateCountNumber(invCountHeaders); //generate count number [S.E]
        //collect by header id [S]
        List<InvCountHeader> insertList = invCountHeaders.stream().filter(line -> line.getCountHeaderId() == null).collect(Collectors.toList());
        List<InvCountHeader> updateList = invCountHeaders.stream().filter(line -> line.getCountHeaderId() != null).collect(Collectors.toList());
        //collect by header id [E]
        invCountHeaderRepository.batchInsertSelective(insertList); // do insert header [S.E]
        //do check if update [S]
        if (!updateList.isEmpty()) {
            //if have line, update line too [S]
            this.getCounters(invCountHeaders);
            for (InvCountHeaderDTO header : invCountHeaders) {
                //do check if counterlist is exist
                if (CollectionUtils.isEmpty(header.getCounterList())){

                }
                //if status is incounting and current user is counter, do check counter [S]
                if (header.getCountStatus().equals(Constants.STATUS_INCOUNTING)
                        && header.getCounterList().stream().filter(line -> line.getUserId().equals(DetailsHelper.getUserDetails().getUserId())).findFirst().orElse(null) != null) {
                    this.manualCount(header); //todo can use this
                }
                //if status is incounting and current user is counter, do check counter [E]
                invCountLineService.saveData(new ArrayList<>(header.getCountOrderLineList())); //update line [S.E] //todo can use new arraylist instead
            }
            //if have line, update line too [E]
        }
        //do check if update [E]
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList); //do update header [S.E]
        return invCountHeaders;
    }

    @Override
    public InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();
        //update validation
        if (!invCountHeaders.isEmpty()) {
//            String errorMessage = "";
            StringBuilder errorMessage = new StringBuilder();
            Long currentOperator = DetailsHelper.getUserDetails().getUserId();
            //get all collection of header id [S]
            Set<Long> headerIds = invCountHeaders.stream().map(InvCountHeaderDTO::getCountHeaderId).collect(Collectors.toSet());
            List<InvCountHeader> invCountHeaderList = invCountHeaderRepository.selectByIds(headerIds.stream()
                    .map(String::valueOf) // Convert Long to String
                    .collect(Collectors.joining(",")));
            //get all collection of header id [E]
            //get all collection of warehouse id [S]
            Set<Long> warehouseIds = invCountHeaders.stream().map(InvCountHeaderDTO::getWarehouseId).collect(Collectors.toSet());
            List<InvWarehouse> invWarehouseList = invWarehouseRepository.selectByIds(warehouseIds.stream()
                    .map(String::valueOf) // Convert Long to String
                    .collect(Collectors.joining(",")));
            //get all collection of warehouse id [E]
            for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
                //do query verification

                //Skip Validation For Insert [S]
                if (invCountHeaderDTO.getCountHeaderId() == null) {
                    invCountInfoDTO.setSuccessList(invCountHeaders);
                    return invCountInfoDTO;
                }
                //Skip Validation For Insert [E]
//                InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(invCountHeaderDTO.getCountHeaderId()); //get header data [S.E]
                InvCountHeader invCountHeader = invCountHeaderList.stream().filter(header -> header.getCountHeaderId().equals(invCountHeaderDTO.getCountHeaderId())).findFirst().orElse(new InvCountHeader());
                //status validation [S]
                if (StringUtils.isNotBlank(invCountHeaderDTO.getStatus()) && !invCountHeaderDTO.getCountStatus().equals(invCountHeader.getCountStatus())) {
//                    errorMessage += "count status could not be updated manually!" + ",";
                    errorMessage.append("count status could not be updated manually!,");
                }
                //status validation [E]
                //count number validation [S]
                if (StringUtil.isNotEmpty(invCountHeaderDTO.getCountNumber()) && !invCountHeaderDTO.getCountNumber().equals(invCountHeader.getCountNumber())) {
//                    errorMessage += "count number could not be updated manually!" + ",";
                    errorMessage.append("count number could not be updated manually!,");
                }
                //count number validation [E]
                String[] validStatuses = {Constants.STATUS_DRAFT, Constants.STATUS_INCOUNTING, Constants.STATUS_REJECTED, Constants.STATUS_WITHDRAWN}; //valid status array [S.E] //todo constant
                String status = invCountHeader.getCountStatus(); //status from header [S.E]
                String[] superVisors = invCountHeader.getSupervisorIds().split(","); //supervisor list from header[S.E]
                String[] counters = invCountHeader.getCounterIds().split(","); //counter list from header[S.E]
                //Valid Status validation [S]
                if (!Arrays.asList(validStatuses).contains(status)) { //todo remove check from list
                    invCountInfoDTO.setErrorList(invCountHeaders);
//                    errorMessage += "only draft, in counting, rejected, and withdrawn status can be modified!" + ",";
                    errorMessage.append("only draft, in counting, rejected, and withdrawn status can be modified!,");

                }
                if (Constants.STATUS_DRAFT.equals(status) && !invCountHeader.getCreatedBy().equals(currentOperator) && !invCountHeaderDTO.getCreatedBy().equals(currentOperator)) {
//                    errorMessage += "Document in draft status can only be modified by the document creator!" + ",";
                    errorMessage.append("Document in draft status can only be modified by the document creator!,");
                }
                if (Arrays.asList(validStatuses).subList(1, validStatuses.length).contains(status)) {
//                    InvWarehouse invWarehouse = invWarehouseRepository.selectByPrimary(invCountHeaderDTO.getWarehouseId()); //get warehouse data [S.E] //todo query outside from collection of warehouse id
                    InvWarehouse invWarehouse = invWarehouseList.stream().filter(invWarehouse1 -> invWarehouse1.getWarehouseId().equals(invCountHeaderDTO.getWarehouseId())).findFirst().orElse(new InvWarehouse());
                    List<String> supervisorIds = Arrays.asList(superVisors); //convert supervisorid to string [S.E]

                    //WMS Warehouse Validation [S]
                    if (invWarehouse.getIsWmsWarehouse() == 1 && !supervisorIds.contains(currentOperator.toString())) {
//                        errorMessage += "The current warehouse is a WMS warehouse, and only the supervisor is allowed to operate, ";
                        errorMessage.append("The current warehouse is a WMS warehouse, and only the supervisor is allowed to operate,");
                    }
                    //WMS Warehouse Validation [E]

                    //Operator Validation [S]
                    if (!invCountHeader.getCreatedBy().equals(currentOperator) &&
                            !Arrays.asList(superVisors).contains(currentOperator.toString()) &&
                            !Arrays.asList(counters).contains(currentOperator.toString())) {
//                        errorMessage += "Only the document creator, counter, and supervisor can modify the document for the status of in counting, rejected, withdrawn, ";
                        errorMessage.append("Only the document creator, counter, and supervisor can modify the document for the status of in counting, rejected, withdrawn!,");
                    }
                    //Operator Validation [E]
                }
                //Valid Status validation [E]
                //return Error Message [S]
//                if (!errorMessage.isEmpty()) {
                if (errorMessage.length() > 0) {
//                    invCountInfoDTO.setErrorList(invCountHeaders); //set error list [S.E]
                    errorList.add(invCountHeaderDTO);
                    invCountInfoDTO.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1)); //set error message and remove last , [S.E]
//                    return invCountInfoDTO;
                } else {
                    successList.add(invCountHeaderDTO);
                }
                //return Error Message [E]
            }
        }
//        invCountInfoDTO.setSuccessList(invCountHeaders); //set success list [S.E]
        invCountInfoDTO.setErrorList(errorList);
        invCountInfoDTO.setSuccessList(successList);
        return invCountInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public InvCountInfoDTO checkAndRemove(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
//        String errorMessage = "";
        StringBuilder errorMessage = new StringBuilder();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            InvCountHeaderDTO invCountHeader = this.selectDetail(invCountHeaderDTO.getCountHeaderId()); //get header data from DB [S.E]
            //Status Validation [S]
            if (!Constants.STATUS_DRAFT.equals(invCountHeader.getCountStatus())) {
//                errorMessage += "status verification, Only allow draft status to be deleted!" + ",";
                errorMessage.append("status verification, Only allow draft status to be deleted!,");
            }
            //Status Validation [E]
            //Operator Validation [S]
            if (!DetailsHelper.getUserDetails().getUserId().equals(invCountHeader.getCreatedBy())) {
//                errorMessage += "current user verification, Only current user is document creator allow delete document!" + ",";
                errorMessage.append("current user verification, Only current user is document creator allow delete document!,");
            }
            //Operator Validation [E]
            //return Error Message [S]
            //                if (!errorMessage.isEmpty()) {
            if (errorMessage.length() > 0) {
                errorList.add(invCountHeader);
                //invCountInfoDTO.setErrorList(invCountHeaders); //set error list [S.E]
                invCountInfoDTO.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1)); //set error message and remove last , [S.E]
            } else {
                successList.add(invCountHeader);
            }
            //return Error Message [E]
            //do line delete [S]
//            List<InvCountLine> lines = new ArrayList<>();
//            for (InvCountLineDTO lineDTO : invCountHeaderDTO.getCountOrderLineList()) {
//                InvCountLine line = new InvCountLine();
//                BeanUtils.copyProperties(lineDTO, line);
//                lines.add(line);
//            }
//            invCountLineRepository.batchDeleteByPrimaryKey(lines);
//            invCountLineRepository.batchDeleteByPrimaryKey(new ArrayList<>(invCountHeaderDTO.getCountOrderLineList()));
            //do line delete [E]
            invCountHeaderRepository.deleteByPrimaryKey(invCountHeaderDTO.getCountHeaderId()); //do phisical delete [S.E]
        }
//        invCountInfoDTO.setSuccessList(invCountHeaders); //set success list [S.E]
        invCountInfoDTO.setErrorList(errorList);
        invCountInfoDTO.setSuccessList(successList);
        return invCountInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public InvCountInfoDTO orderExecution(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO infoDTO = new InvCountInfoDTO();
        //SAVE
        infoDTO = this.manualSaveCheck(invCountHeaders);
        if (StringUtil.isNotEmpty(infoDTO.getTotalErrorMsg())) {
            throw new CommonException(infoDTO.getTotalErrorMsg());
        }
        invCountHeaders = self().manualSave(invCountHeaders);

        //EXECUTE
        infoDTO = this.executeCheck(invCountHeaders);
        if (StringUtil.isNotEmpty(infoDTO.getTotalErrorMsg())) {
            throw new CommonException(infoDTO.getTotalErrorMsg());
        }
        invCountHeaders = self().execute(invCountHeaders);

        //SYNC WMS
        infoDTO = this.countSyncWms(invCountHeaders);
        if (StringUtil.isNotEmpty(infoDTO.getTotalErrorMsg())) {
            throw new CommonException(infoDTO.getTotalErrorMsg());
        }
        return infoDTO;
    }

    @Override
    public InvCountInfoDTO executeCheck(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();
        List<LovValueDTO> countDimensionList = lovAdapter.queryLovValue(Constants.LOV_CODE_COUNT_DIMENSION, BaseConstants.DEFAULT_TENANT_ID);
        List<LovValueDTO> countModeList = lovAdapter.queryLovValue(Constants.LOV_CODE_COUNT_MODE, BaseConstants.DEFAULT_TENANT_ID);
        List<LovValueDTO> countTypeList = lovAdapter.queryLovValue(Constants.LOV_CODE_COUNT_TYPE, BaseConstants.DEFAULT_TENANT_ID);
        Set<Long> headerIds = invCountHeaders.stream().map(InvCountHeaderDTO::getCountHeaderId).collect(Collectors.toSet());

        Map<Long, InvCountHeader> invCountHeaderList = invCountHeaderRepository.selectByIds(headerIds.stream()
                .map(String::valueOf) // Convert Long to String
                .collect(Collectors.joining(","))).stream().collect(Collectors.toMap(InvCountHeader::getCountHeaderId, invCountHeader -> invCountHeader));

        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            InvCountHeader header = invCountHeaderList.get(invCountHeaderDTO.getCountHeaderId());
            StringBuilder errorMessage = new StringBuilder();
            //Status Validation [S]
            if (!Constants.STATUS_DRAFT.equals(header.getCountStatus())) {
                errorMessage.append("document status validation: Only draft status can execute!,");
            }
            //Status Validation [E]
            //current user validation [S]
            if (!DetailsHelper.getUserDetails().getUserId().equals(header.getCreatedBy())) {
                errorMessage.append("current login user validation: Only the document creator can execute!,");
            }
            //current user validation [E]
            //value set validation [S]
            //dimension Validation [S]
            LovValueDTO lovValueDTO = countDimensionList.stream().filter(value -> value.getValue().equals(invCountHeaderDTO.getCountDimension())).findFirst().orElse(null);
            if (lovValueDTO == null) {
                errorMessage.append("Value set validation: count dimension incorrect!,");
            }
            //dimension Validation [E]
            //mode validation [S]
            lovValueDTO = countModeList.stream().filter(value -> value.getValue().equals(invCountHeaderDTO.getCountMode())).findFirst().orElse(null);
            if (lovValueDTO == null) {
                errorMessage.append("Value set validation: count mode incorrect!,");
            }
            //mode validation [E]
            //type validation [S]
            lovValueDTO = countTypeList.stream().filter(value -> value.getValue().equals(invCountHeaderDTO.getCountType())).findFirst().orElse(null);
            if (lovValueDTO == null) {
                errorMessage.append("Value set validation: count type incorrect!,");
            }
            //type validation [E]
            //value set validation [E]
            //company validation [S]
            if (ObjectUtils.isEmpty(iamCompanyRepository.selectByPrimary(invCountHeaderDTO.getCompanyId()))) {
                errorMessage.append("Company validation: there is no company with id ").append(invCountHeaderDTO.getCompanyId()).append("!,");
            }
            //company validation [E]
            //department validation [S]
            if (invCountHeaderDTO.getDepartmentId() != null) {
                if (ObjectUtils.isEmpty(iamDepartmentRepository.selectByPrimary(invCountHeaderDTO.getDepartmentId()))) {
                    errorMessage.append("Department validation: there is no department with id ").append(invCountHeaderDTO.getCompanyId()).append("!,");
                }
            }
            //department validation [E]
            //warehouse validation [S]
            if (ObjectUtils.isEmpty(invWarehouseRepository.selectByPrimary(invCountHeaderDTO.getWarehouseId()))) {
                errorMessage.append("Warehouse validation: there is no warehouse with id ").append(invCountHeaderDTO.getCompanyId()).append("!,");
            }
            //warehouse validation [E]
            //on hand qty validation [S]
            InvStock invStock = new InvStock();
            invStock.setTenantId(invCountHeaderDTO.getTenantId());
            invStock.setCompanyId(invCountHeaderDTO.getCompanyId());
            invStock.setDepartmentId(invCountHeaderDTO.getDepartmentId());
            invStock.setWarehouseId(invCountHeaderDTO.getWarehouseId());
            if (StringUtils.isNotBlank(invCountHeaderDTO.getSnapshotMaterialIds())) {
                invStock.setMaterialIdLongs(Arrays.stream(invCountHeaderDTO.getSnapshotMaterialIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
            }
            if (StringUtils.isNotBlank(invCountHeaderDTO.getSnapshotBatchIds())) {
                invStock.setBatchIdLongs(Arrays.stream(invCountHeaderDTO.getSnapshotBatchIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
            }
            if (CollectionUtils.isEmpty(invStockRepository.selectList(invStock))) {
                errorMessage.append("Unable to query on hand quantity data!,");
            }
            //on hand qty validation [E]
            //return Error [S]
            if (errorMessage.length() > 0) {
                errorList.add(invCountHeaderDTO);
                invCountInfoDTO.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1));
                return invCountInfoDTO;
            } else {
                successList.add(invCountHeaderDTO);
            }
            //return Error [E]
        }
        invCountInfoDTO.setErrorList(errorList);
        invCountInfoDTO.setSuccessList(successList);
        return invCountInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaders) {
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            //get stock data [S]
            InvStockDTO invStockDTO = new InvStockDTO();
            //Set Data for grouping [S]
            invStockDTO.setTenantId(invCountHeaderDTO.getTenantId());
            invStockDTO.setCompanyId(invCountHeaderDTO.getCompanyId());
            if (invCountHeaderDTO.getDepartmentId() != null) {
                invStockDTO.setDepartmentId(invCountHeaderDTO.getDepartmentId());
            }
            invStockDTO.setWarehouseId(invCountHeaderDTO.getWarehouseId());
            String dimension = invCountHeaderDTO.getCountDimension(); //get dimension [S.E]
            //Set Data for grouping [E]
            if (StringUtils.isNotBlank(invCountHeaderDTO.getSnapshotMaterialIds())) {
                invStockDTO.setMaterialIdLongs(Arrays.stream(invCountHeaderDTO.getSnapshotMaterialIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
            }
            if (StringUtils.isNotBlank(invCountHeaderDTO.getSnapshotBatchIds())) {
                invStockDTO.setBatchIdLongs(Arrays.stream(invCountHeaderDTO.getSnapshotBatchIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
            }
            if (dimension.equals(Constants.DIMENSION_SKU)) {
                invStockDTO.setLot(false);
            } else if (dimension.equals(Constants.DIMENSION_LOT)) {
                invStockDTO.setLot(true);
            }
            List<InvStockDTO> invStockDTOList = invStockRepository.selectSum(invStockDTO); //get summary from stock [S.E]
            //get stock data [E]

            //set Line Data [S]
            AtomicInteger a = new AtomicInteger(0);
            List<InvCountLineDTO> invCountLineDTOList = new ArrayList<>();
            for (InvStockDTO invStock1 : invStockDTOList) {
                InvCountLineDTO invCountLine = new InvCountLineDTO();
                //get from header [S]
                invCountLine.setTenantId(invCountHeaderDTO.getTenantId());
                invCountLine.setCountHeaderId(invCountHeaderDTO.getCountHeaderId());
                invCountLine.setCounterIds(invCountHeaderDTO.getCounterIds());
                invCountLine.setLineNumber(a.incrementAndGet());
                invCountLine.setWarehouseId(invCountHeaderDTO.getWarehouseId());
                //get from header [E]
                //get from stock [S]
                invCountLine.setBatchId(invStock1.getBatchId());
                invCountLine.setMaterialId(invStock1.getMaterialId());
                invCountLine.setUnitCode(invStock1.getUnitCode());
                invCountLine.setMaterialCode(invStock1.getMaterialCode());
                invCountLine.setSnapshotUnitQty(invStock1.getSummary());
                //get from stock [E]
                invCountLineDTOList.add(invCountLine); //add to new list [S.E]
            }
            //set Line Data [E]

            invCountLineService.saveData(new ArrayList<>(invCountLineDTOList)); //insert line to db [S.E]
            //add to header [S]
//            for (int i = 0; i < invCountLineList.size(); i++) {
//                invCountLineDTOList.get(i).setCountLineId(invCountLineList.get(i).getCountLineId());
//            }
            invCountHeaderDTO.setCountOrderLineList(invCountLineDTOList);
            //add to header [E]
            //update header status
            invCountHeaderDTO.setCountStatus(Constants.STATUS_INCOUNTING);
        }
//        List<InvCountHeader> updateList = invCountHeaders.stream().filter(header -> header.getCountHeaderId() != null).collect(Collectors.toList());
        invCountHeaderRepository.batchUpdateOptional(new ArrayList<>(invCountHeaders), InvCountHeader.FIELD_COUNT_STATUS);
//        invCountHeaderRepository.batchUpdateOptional(updateList, InvCountHeader.FIELD_COUNT_STATUS);
        return invCountHeaders;
    }

    @Override
    public InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();
        StringBuilder errorMessage = new StringBuilder();
        //check warehouse [S]
        Set<Long> warehouseIds = invCountHeaders.stream().map(InvCountHeaderDTO::getWarehouseId).collect(Collectors.toSet());
        Map<Long, InvWarehouse> invWarehouseMap = invWarehouseRepository.selectByIds(warehouseIds.stream()
                .map(String::valueOf) // Convert Long to String
                .collect(Collectors.joining(","))).stream().collect(Collectors.toMap(InvWarehouse::getWarehouseId, warehouse -> warehouse));
        //check warehouse [E]
        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            Map<String, InvCountExtra> invExtraMap = initializeInvExtraMap(invCountHeaderDTO); // Initialize extra data [S.E]

            int isWmsWarehouse = invWarehouseMap.get(invCountHeaderDTO.getWarehouseId()).getIsWmsWarehouse();//get WMS Warehouse status [S.E]

            if (isWmsWarehouse == 1) {
                processWmsSync(invCountHeaderDTO, invExtraMap, errorMessage); //Do Precess WMS SYNC
            } else {
                setSkippedStatus(invExtraMap); //Skipped
            }

            //insert to Extra Table
            saveOrUpdateInvExtra(invExtraMap, invCountHeaderDTO);

            //set error and success list [S]
            if (errorMessage.length() > 0) {
                errorList.add(invCountHeaderDTO);
            } else {
                successList.add(invCountHeaderDTO);
            }
            //set error and success list [E]
        }
        // Set final results
        invCountInfoDTO.setErrorList(errorList);
        invCountInfoDTO.setSuccessList(successList);
        invCountInfoDTO.setTotalErrorMsg(trimTrailingComma(errorMessage));
        return invCountInfoDTO;
    }

    @Override
    public InvCountHeaderDTO countResultSync(InvCountHeaderDTO invCountHeader) {
        log.info("Result Sync Called with payload: {}", invCountHeader);
        StringBuilder errorMessage = new StringBuilder();
        AtomicReference<String> status = new AtomicReference<>("S");

        //check if header id exist
        if (invCountHeader.getCountHeaderId() == null && invCountHeader.getCountNumber() != null){
            InvCountHeader header = invCountHeaderRepository.selectOne(invCountHeader);
            invCountHeader.setCountHeaderId(header.getCountHeaderId());
            invCountHeader.setWarehouseId(header.getWarehouseId());
        }

        InvWarehouse invWarehouse = new InvWarehouse();
        invWarehouse.setTenantId(invCountHeader.getTenantId());
        invWarehouse.setWarehouseId(invCountHeader.getWarehouseId());
        List<InvWarehouse> invWarehouseList = invWarehouseRepository.selectList(invWarehouse);
        //Check if current warehouse is WMS Warehouse [S]
        int isWmsWarehouse = invWarehouseList.stream()
                .filter(value -> value.getWarehouseId().equals(invCountHeader.getWarehouseId())) // Filter by warehouseID
                .mapToInt(InvWarehouse::getIsWmsWarehouse) // Map to the isWmsWarehouse (int value)
                .findFirst() // Get the first match
                .orElse(0);

        if (isWmsWarehouse == 0) {
            errorMessage.append("The current warehouse is not a WMS warehouse, operations are not allowed!,");
            status.set("E");
        }
        //Check if current warehouse is WMS Warehouse [E]



        //check line data [S]
        Condition condition = new Condition(InvCountLine.class);
        condition.createCriteria().andEqualTo(InvCountLine.FIELD_COUNT_HEADER_ID, invCountHeader.getCountHeaderId());
        List<InvCountLine> lineList = invCountLineRepository.selectByCondition(condition);
        if (invCountHeader.getCountOrderLineList().size() != lineList.size()) {
            errorMessage.append("The counting order line data is inconsistent with the INV system, please check the data !,");
            status.set("E");
        }
        lineList.forEach(invCountLine -> {
            boolean isConsistent = invCountHeader.getCountOrderLineList().stream()
                    .anyMatch(lineData -> invCountLine.getCountLineId().equals(lineData.getCountLineId()));

            if (!isConsistent) {
                errorMessage.append("The counting order line data is inconsistent with the INV system, please check the data!,");
                status.set("E");
            }
        });
        //check line data [E]
        if (status.get().equals("S")) {
            invCountHeader.setStatus(status.get());
            lineList.forEach(invCountLine -> {
                InvCountLineDTO lineDto = invCountHeader.getCountOrderLineList().stream().filter(invCountLineDTO -> invCountLineDTO.getCountLineId().equals(invCountLine.getCountLineId())).findFirst().orElse(new InvCountLineDTO());
                invCountLine.setUnitQty(lineDto.getUnitQty());
                invCountLine.setUnitDiffQty(lineDto.getUnitQty().subtract(invCountLine.getSnapshotUnitQty()));
            });
            invCountLineRepository.batchUpdateOptional(lineList,InvCountLine.FIELD_UNIT_QTY,InvCountLine.FIELD_UNIT_DIFF_QTY);
        } else {
            invCountHeader.setStatus(status.get());
            invCountHeader.setErrorMsg(errorMessage.substring(0, errorMessage.length() - 1));
        }
        return invCountHeader;
    }

    @Override
    public InvCountInfoDTO submitCheck(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();
        String[] validStatuses = {
                Constants.STATUS_PROCESSING,
                Constants.STATUS_INCOUNTING,
                Constants.STATUS_REJECTED,
                Constants.STATUS_WITHDRAWN
        };

        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            StringBuilder errorMessage = new StringBuilder();
            //Status Validation [S]
            if (!Arrays.asList(validStatuses).contains(invCountHeaderDTO.getCountStatus())) {
                errorMessage.append("The operation is allowed only when the status is in counting, processing, rejected, withdrawn!,");
            }
            //Status Validation [E]

            //Supervisor Validation [S]
            boolean isSupervisor = Optional.ofNullable(invCountHeaderDTO.getSupervisorIds())
                    .map(ids -> Arrays.stream(ids.split(","))
                            .map(Long::parseLong)
                            .anyMatch(id -> id.equals(DetailsHelper.getUserDetails().getUserId())))
                    .orElse(false);
            if (!isSupervisor) {
                errorMessage.append("Only Supervisor can submit document !,");
            }
            //Supervisor Validation [E]

            //Data Integrity Validation [S]
            Map<Long, InvCountLine> invCountLineMap = invCountHeaderDTO.getCountOrderLineList().stream()
                    .map(InvCountLineDTO::getCountLineId)
                    .distinct()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            id -> invCountLineRepository.selectByPrimary(id)
                    ));
            for (InvCountLineDTO invCountLineDTO : invCountHeaderDTO.getCountOrderLineList()) {
                //Qty Validation [S]
                if (invCountLineDTO.getUnitQty() == null
                        || invCountLineDTO.getSnapshotUnitQty() == null
                        || invCountLineDTO.getUnitDiffQty() == null) {
                    errorMessage.append("There are data rows (").append(invCountLineDTO.getLineNumber()).append(") with empty count quantity. Please check the data !,");
                }
                //Qty Validation [E]
                //check for empty Qty[S]
                InvCountLine dbLine = invCountLineMap.get(invCountLineDTO.getCountLineId());
                if (dbLine != null && dbLine.getUnitDiffQty().compareTo(BigDecimal.ZERO) < 0 &&
                        StringUtils.isBlank(invCountHeaderDTO.getReason())) {
                    errorMessage.append("There is a quantity difference, and the reason must not be empty!,");
                }
                //check for empty Qty[E]

            }
            //Data Integrity Validation [E]
            //Return Error [S]
            if (errorMessage.length() > 0) {
                invCountInfoDTO.setTotalErrorMsg(errorMessage.substring(0, errorMessage.length() - 1));
                errorList.add(invCountHeaderDTO);
            } else {
                successList.add(invCountHeaderDTO);
            }
            //Return Error [E]
        }
        invCountInfoDTO.setErrorList(errorList);
        invCountInfoDTO.setSuccessList(successList);
        return invCountInfoDTO;
    }

    @Override
    public List<InvCountHeaderDTO> submit(List<InvCountHeaderDTO> invCountHeaders) {
        //collect department ID
        // Collect department IDs from the header's line list
        Set<Long> departmentIds = invCountHeaders.stream()
                .map(InvCountHeaderDTO::getDepartmentId)
                .collect(Collectors.toSet());

        // Retrieve department data and map it by department ID
        Map<Long, IamDepartment> iamDepartmentMap = iamDepartmentRepository
                .selectByIds(departmentIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .stream()
                .collect(Collectors.toMap(IamDepartment::getDepartmentId, Function.identity()));

        // get Workflow flag from profile client
        String workflowFlag = profileClient.getProfileValueByOptions(BaseConstants.DEFAULT_TENANT_ID, null, null, Constants.CONF_MAIN_WORK_FLOW);

        for (InvCountHeaderDTO invCountHeaderDTO : invCountHeaders) {
            if ("1".equals(workflowFlag)) {
                handleWorkflow(invCountHeaderDTO, iamDepartmentMap);
            } else {
                updateDocumentStatusToConfirmed(invCountHeaderDTO);
            }

        }
        return invCountHeaders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<InvCountHeaderDTO> orderSubmit(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO infoDTO = new InvCountInfoDTO();
        //SAVE
        infoDTO = this.manualSaveCheck(invCountHeaders);
        if (StringUtil.isNotEmpty(infoDTO.getTotalErrorMsg())) {
            throw new CommonException(infoDTO.getTotalErrorMsg());
        }
        self().manualSave(invCountHeaders);

        //SUBMIT
        infoDTO = this.submitCheck(invCountHeaders);
        if (StringUtil.isNotEmpty(infoDTO.getTotalErrorMsg())) {
            throw new CommonException(infoDTO.getTotalErrorMsg());
        }
        self().submit(invCountHeaders);
        return invCountHeaders;
    }

    @Override
    public List<InvCountHeaderDTO> countingOrderReportDs(Long organizationId, InvCountHeaderDTO invCountHeader) {
        //region latest code [S]
        List<InvCountHeaderDTO> invCountHeaderDTOList = invCountHeaderRepository.selectReport(invCountHeader);

        //Get all material [S]
        Map<Long, InvMaterial> invMaterialMap = getMaterialMap(invCountHeaderDTOList);
        //Get all material [E]

        //Get all batch [S]
        Map<Long, InvBatch> invBatchMap = getBatchMap(invCountHeaderDTOList);
        //Get all batch [E]

        //Get all Line [S]
        Map<Long, List<InvCountLineDTO>> countLineMap = getLineMap(invCountHeaderDTOList);
        //Get all Line [S]

        List<InvCountHeaderDTO> filteredHeaders = invCountHeaderDTOList.parallelStream()
                .map(header -> {
                    header.setTenantAdminFlag(Boolean.TRUE.equals(getTenantData().getTenantAdminFlag())); //Set tenant admin flag[S.E]
                    //Material Snapshot
                    header.setSnapshotMaterialList(Arrays.stream(header.getSnapshotMaterialIds().split(","))
                            .map(Long::parseLong) // Convert String to Long
                            .map(invMaterialMap::get) // Get InvMaterial from the map
                            .collect(Collectors.toList())); // Collect as a list

                    //Batch Snapshot
                    header.setSnapshotBatchList(Arrays.stream(header.getSnapshotBatchIds().split(","))
                            .map(Long::parseLong) // Convert String to Long
                            .map(invBatchMap::get) // Get InvMaterial from the map
                            .collect(Collectors.toList()));

                    //Line
                    header.setCountOrderLineList(countLineMap.get(header.getCountHeaderId()));

                    //history
                    List<RunTaskHistory> approveHistory = getApproveHistory(organizationId, header.getCountNumber());
                    header.setHistoryList(approveHistory);

                    Map<Long, InvMaterial> materialMap = new ConcurrentHashMap<>();
                    Map<Long, InvBatch> batchMap = new ConcurrentHashMap<>();
                    materialMap = Optional.ofNullable(header.getSnapshotMaterialList())
                            .orElse(Collections.emptyList())
                            .stream().collect(Collectors.toMap(InvMaterial::getMaterialId, Function.identity()));
                    batchMap = Optional.ofNullable(header.getSnapshotBatchList())
                            .orElse(Collections.emptyList())
                            .stream().collect(Collectors.toMap(InvBatch::getBatchId, Function.identity()));

                    header.setMaterialListString(materialMap.values().stream()
                            .map(InvMaterial::getMaterialCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(",")));

                    header.setBatchListString(batchMap.values().stream()
                            .map(InvBatch::getBatchCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(",")));
                    //endregion

                    String counterNames = header.getCounterList() != null ?
                            header.getCounterList().stream()
                                    .map(UserDTO::getRealName)
                                    .collect(Collectors.joining(","))
                            : "";

                    String supervisorNames = header.getSupervisorList() != null ?
                            header.getSupervisorList().stream()
                                    .map(UserDTO::getRealName)
                                    .collect(Collectors.joining(","))
                            : "";

                    header.setCounterNameListString(counterNames);
                    header.setSupervisorNameListString(supervisorNames);

                    // Process Counter Names for Line Items
                    if (header.getCountOrderLineList() != null) {
                        header.getCountOrderLineList().parallelStream().forEach(lineDTO -> {
                            String lineCounterNames = lineDTO.getCounterList() != null ?
                                    lineDTO.getCounterList().stream()
                                            .map(UserDTO::getRealName)
                                            .collect(Collectors.joining(","))
                                    : "";
                            lineDTO.setCounterName(lineCounterNames);
                        });
                    }
                    return header;
                })
                .collect(Collectors.toList());
        //endregion latest code [E]
        //requery data [E]
        return filteredHeaders;
    }

    @Override
    public InvCountHeaderDTO callBack(WorkFlowEventDTO workFlowEventDTO) {
        InvCountHeaderDTO invCountHeaderDTO = new InvCountHeaderDTO();
        String currentSupervisor = DetailsHelper.getUserDetails().getUserId().toString(); //get current user id for supervisor [S.E]
        //update current header
        //Set Count Number [S]
        InvCountHeader invCountHeader = new InvCountHeader();
        invCountHeader.setCountNumber(workFlowEventDTO.getBusinessKey());
        //Set Count Number [E]
        //Query Current Count Header [S]
        InvCountHeader invCountHeader1 = invCountHeaderRepository.selectOne(invCountHeader);
        //Query Current Count Header [E]
        //Set Updated Column [S]
        if (StringUtil.isNotEmpty(workFlowEventDTO.getDocStatus())) {
            invCountHeader1.setCountStatus(workFlowEventDTO.getDocStatus());
        }
        if (workFlowEventDTO.getApprovedTime() != null){
            invCountHeader1.setApprovedTime(workFlowEventDTO.getApprovedTime());
        }
        invCountHeader1.setWorkflowId(workFlowEventDTO.getWorkflowId());
        invCountHeader1.setSupervisorIds(currentSupervisor);
        //Set Updated Column [E]
        //Do Update [S]
        if (invCountHeaderRepository.updateOptional(invCountHeader1, InvCountHeader.FIELD_COUNT_STATUS, InvCountHeader.FIELD_APPROVED_TIME, InvCountHeader.FIELD_WORKFLOW_ID, InvCountHeader.FIELD_SUPERVISOR_IDS) > 0) {
            invCountHeaderDTO = invCountHeaderRepository.selectByPrimary(invCountHeader.getCountHeaderId()); //convert to header DTO [S.E]
        }
        //Do Update [E]
        return invCountHeaderDTO;
    }

    @Override
    public InvCountHeaderDTO manualCount(InvCountHeaderDTO invCountHeaderDTO) {
        for (InvCountLineDTO line : invCountHeaderDTO.getCountOrderLineList()) {
            //check if unit is not empty [S]
            if (line.getUnitQty() != null) {
                line.setUnitDiffQty(line.getUnitQty().subtract(line.getSnapshotUnitQty())); //count diff [S.E]
                line.setCounterIds(DetailsHelper.getUserDetails().getUserId().toString()); //change line counter to current counter [S.E]
            }
            //check if unit is not empty [E]
        }
        return invCountHeaderDTO;
    }

    //region Helper Method
    //get history list
    private List<RunTaskHistory> getApproveHistory(Long organizationId, String bussinessKey) {
        List<RunTaskHistory> runTaskHistory = new ArrayList<>();
        try {
            runTaskHistory = workflowClient.approveHistoryByFlowKey(organizationId, Constants.WORKFLOW_KEY, bussinessKey);
        } catch (Exception e) {
            return runTaskHistory;
        }
        return runTaskHistory;
    }

    //get current tenant data
    private UserVO getTenantData() {
        ResponseEntity<String> stringResponseEntity = iamRemoteService.selectSelf();
        try {
            UserVO userVO = objectMapper.readValue(stringResponseEntity.getBody(), UserVO.class);
            return userVO;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, InvCountExtra> initializeInvExtraMap(InvCountHeaderDTO invCountHeaderDTO) {
        List<InvCountExtra> invExtra = extraRepository.select(new InvCountExtra().setSourceId(invCountHeaderDTO.getCountHeaderId()));
        Map<String, InvCountExtra> invExtraMap = invExtra.stream()
                .collect(Collectors.toMap(InvCountExtra::getProgramKey, extra -> extra));

        invExtraMap.putIfAbsent("wms_sync_status", createNewInvCountExtra(invCountHeaderDTO, "wms_sync_status"));
        invExtraMap.putIfAbsent("wms_sync_error_message", createNewInvCountExtra(invCountHeaderDTO, "wms_sync_error_message"));

        return invExtraMap;
    }

    private InvCountExtra createNewInvCountExtra(InvCountHeaderDTO headerDTO, String programKey) {
        return new InvCountExtra()
                .setSourceId(headerDTO.getCountHeaderId())
                .setTenantId(headerDTO.getTenantId())
                .setEnabledFlag(1)
                .setProgramKey(programKey);
    }

    private void processWmsSync(InvCountHeaderDTO invCountHeaderDTO, Map<String, InvCountExtra> invExtraMap, StringBuilder errorMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            invCountHeaderDTO.setEmployeeNumber(getTenantData().getEmployeeNum());
            String payload = objectMapper.writeValueAsString(invCountHeaderDTO);

            log.info("Calling WMS interface with payload: {}", payload);
            ResponsePayloadDTO responsePayloadDTO = utils.invokeInterface(payload, "HZERO", "FEXAM_WMS", "fexam-wms-api.thirdAddCounting", null, MediaType.APPLICATION_JSON);

            Map<String, String> response = objectMapper.readValue(responsePayloadDTO.getPayload(), Map.class);
            handleWmsResponse(response, responsePayloadDTO.getPayload(), invExtraMap, errorMessage);
        } catch (Exception e) {
            setFailedStatus(invExtraMap, e.getMessage(), errorMessage);
        }
    }

    private void handleWmsResponse(Map<String, String> response, String payload, Map<String, InvCountExtra> invExtraMap, StringBuilder errorMessage) {
        if ("S".equals(response.get("returnStatus"))) {
            invExtraMap.get("wms_sync_status").setProgramValue("SUCCESS");
            invExtraMap.get("wms_sync_error_message").setProgramValue("");
        } else {
            String errorMsg = response.getOrDefault("returnMsg", response.get("message"));
            invExtraMap.get("wms_sync_status").setProgramValue("FAILED");
            invExtraMap.get("wms_sync_error_message").setProgramValue(errorMsg);
            errorMessage.append("Response Exception: ").append(errorMsg).append(",");
        }
    }

    private void setFailedStatus(Map<String, InvCountExtra> invExtraMap, String errorMsg, StringBuilder errorMessage) {
        invExtraMap.get("wms_sync_status").setProgramValue("FAILED");
        invExtraMap.get("wms_sync_error_message").setProgramValue(errorMsg);
        errorMessage.append("Response Exception: ").append(errorMsg).append(",");
    }

    private void setSkippedStatus(Map<String, InvCountExtra> invExtraMap) {
        invExtraMap.get("wms_sync_status").setProgramValue("SKIP");
        invExtraMap.get("wms_sync_error_message").setProgramValue("SKIPPED");
    }

    private void saveOrUpdateInvExtra(Map<String, InvCountExtra> invExtraMap, InvCountHeaderDTO invCountHeaderDTO) {
        List<InvCountExtra> invExtraList = new ArrayList<>(invExtraMap.values());
        invCountExtraService.saveData(invExtraList);
    }

    private String trimTrailingComma(StringBuilder errorMessage) {
        return errorMessage.length() > 0 ? errorMessage.substring(0, errorMessage.length() - 1) : "";
    }

    private void handleWorkflow(InvCountHeaderDTO headerDTO, Map<Long, IamDepartment> departmentMap) {
        // Prepare Workflow Variables
        String starter = Constants.WORKFLOW_DEFAULT_DIMENSION.equals("USER")
                ? DetailsHelper.getUserDetails().getUserId().toString()
                : DetailsHelper.getUserDetails().getUsername();

        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("departmentCode", departmentMap.get(headerDTO.getDepartmentId())
                .getDepartmentCode());

        // Start Workflow
        RunInstance workFlowStart = workflowClient.startInstanceByFlowKey(
                headerDTO.getTenantId(), Constants.WORKFLOW_KEY, headerDTO.getCountNumber(),
                Constants.WORKFLOW_DEFAULT_DIMENSION, starter, variableMap
        );

        log.info("Workflow started with status {}", workFlowStart.getStatus());

        // Update Header Repository
//        InvCountHeader invCountHeader = new InvCountHeader();
//        BeanUtils.copyProperties(headerDTO, invCountHeader);
//        invCountHeaderRepository.updateOptional(invCountHeader, InvCountHeader.FIELD_REASON);
    }

    private void updateDocumentStatusToConfirmed(InvCountHeaderDTO headerDTO) {
        // Update Status
        headerDTO.setCountStatus(Constants.STATUS_CONFIRMED);

        // Update Header Repository
        InvCountHeader invCountHeader = new InvCountHeader();
        BeanUtils.copyProperties(headerDTO, invCountHeader);
        invCountHeaderRepository.updateOptional(
                invCountHeader, InvCountHeader.FIELD_COUNT_STATUS
        );
    }

    private Map<Long, InvMaterial> getMaterialMap(List<InvCountHeaderDTO> invCountHeaderDTOList) {
        Set<Long> materialIds = invCountHeaderDTOList.stream()
                .flatMap(header -> Arrays.stream(header.getSnapshotMaterialIds().split(","))
                        .map(Long::parseLong))
                .collect(Collectors.toSet());

        return invMaterialRepository
                .selectByIds(materialIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .stream()
                .collect(Collectors.toMap(InvMaterial::getMaterialId, Function.identity()));
    }

    private Map<Long, InvBatch> getBatchMap(List<InvCountHeaderDTO> invCountHeaderDTOList) {
        Set<Long> batchIds = invCountHeaderDTOList.stream()
                .flatMap(header -> Arrays.stream(header.getSnapshotBatchIds().split(","))
                        .map(Long::parseLong))
                .collect(Collectors.toSet());

        return invBatchRepository
                .selectByIds(batchIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .stream()
                .collect(Collectors.toMap(InvBatch::getBatchId, Function.identity()));
    }

    private Map<Long, List<InvCountLineDTO>> getLineMap(List<InvCountHeaderDTO> invCountHeaderDTOList) {

        Set<Long> headerIds = invCountHeaderDTOList.stream().map(InvCountHeaderDTO::getCountHeaderId).collect(Collectors.toSet());
        // The key is the header ID
        // The value is the list of InvCountLineDTO
        return headerIds.stream()
                .collect(Collectors.toMap(
                        id -> id, // The key is the header ID
                        id -> {
                            InvCountLineDTO invCountLine = (InvCountLineDTO) new InvCountLineDTO().setCountHeaderId(id);
                            return invCountLineRepository.selectLineReport(invCountLine); // The value is the list of InvCountLineDTO
                        }
                ));
    }

    @ProcessCacheValue
    private List<InvCountHeaderDTO> getCounters(List<InvCountHeaderDTO> invCountHeaderDTOList) {
        invCountHeaderDTOList.forEach(invCountHeaderDTO -> {
            List<UserDTO> counterList = Arrays.stream(invCountHeaderDTO.getCounterIds().split(","))
                    .map(Long::parseLong)
                    .map(userId -> {
                        UserDTO userDTO = new UserDTO();
                        userDTO.setUserId(userId);
                        return userDTO;
                    })
                    .collect(Collectors.toList());
            invCountHeaderDTO.setCounterList(counterList);
        });


        return invCountHeaderDTOList;
    }
    //endregion Helper Method
}

