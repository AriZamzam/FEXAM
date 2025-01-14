package com.hand.demo.app.service;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import com.hand.demo.api.dto.WorkFlowEventDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.core.base.AopProxy;

import java.util.List;

/**
 * (InvCountHeader)应用服务
 *
 * @author Zamzam
 * @since 2024-12-17 10:23:01
 */
public interface InvCountHeaderService extends AopProxy<InvCountHeaderService> {

    /**
     * 查询数据
     *
     * @param pageRequest     分页参数
     * @param invCountHeaders 查询条件
     * @return 返回值
     */
    Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeaderDTO invCountHeaders);
    InvCountHeaderDTO selectDetail(Long countHeaderId);

    /**
     * 保存数据
     *
     * @param invCountHeaders 数据
     */
    List<InvCountHeaderDTO> orderSave(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Saves the provided inventory count header data manually.
     *
     * @param invCountHeaders the list of inventory count headers to be saved
     * @return the saved list of inventory count headers
     */
    List<InvCountHeaderDTO> manualSave(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Performs a pre-save check on the provided inventory count header data.
     *
     * @param invCountHeaders the list of inventory count headers to check
     * @return the result of the pre-save check, including any validation messages or errors
     */
    InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Validates and removes the specified inventory count headers.
     *
     * @param invCountHeaders the list of inventory count headers to validate and remove
     * @return the result of the operation, including validation and removal details
     */
    InvCountInfoDTO checkAndRemove(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Executes operations related to ordering inventory counts.
     *
     * @param invCountHeaders the list of inventory count headers to process
     * @return the result of the order execution, including any errors or warnings
     */
    InvCountInfoDTO orderExecution(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Checks preconditions for executing inventory count operations.
     *
     * @param invCountHeaders the list of inventory count headers to check
     * @return the result of the execution check, including any validation details
     */
    InvCountInfoDTO executeCheck(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Executes the inventory count operation for the given headers.
     *
     * @param invCountHeaders the list of inventory count headers to execute
     * @return the updated list of inventory count headers after execution
     */
    List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Synchronizes inventory counts with the Warehouse Management System (WMS).
     *
     * @param invCountHeaders the list of inventory count headers to synchronize
     * @return the result of the synchronization process
     */
    InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Synchronizes individual count results with external systems.
     *
     * @param invCountHeader the inventory count header to synchronize
     * @return the updated inventory count header after synchronization
     */
    InvCountHeaderDTO countResultSync(InvCountHeaderDTO invCountHeader);
    /**
     * Verifies if the inventory counts can be submitted.
     *
     * @param invCountHeaders the list of inventory count headers to check
     * @return the result of the submission check, including any validation details
     */
    InvCountInfoDTO submitCheck(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Submits the provided inventory count headers.
     *
     * @param invCountHeaders the list of inventory count headers to submit
     * @return the submitted list of inventory count headers
     */
    List<InvCountHeaderDTO> submit(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Submits inventory counts in an ordered sequence.
     *
     * @param invCountHeaders the list of inventory count headers to submit in order
     * @return the submitted list of inventory count headers
     */
    List<InvCountHeaderDTO> orderSubmit(List<InvCountHeaderDTO> invCountHeaders);
    /**
     * Generates a report for counting orders within a specific organization.
     *
     * @param organizationId the ID of the organization
     * @param invCountHeader the inventory count header criteria for the report
     * @return the list of inventory count headers that match the report criteria
     */
    List<InvCountHeaderDTO> countingOrderReportDs(Long organizationId, InvCountHeaderDTO invCountHeader);
    /**
     * Handles callback events from a workflow.
     *
     * @param workFlowEventDTO the workflow event data
     * @return the updated inventory count header after processing the callback
     */
    InvCountHeaderDTO callBack(WorkFlowEventDTO workFlowEventDTO);
    /**
     * Performs a manual inventory count operation.
     *
     * @param invCountHeaderDTO the inventory count header data for manual counting
     * @return the updated inventory count header after the manual count
     */
    InvCountHeaderDTO manualCount(InvCountHeaderDTO invCountHeaderDTO);
}

