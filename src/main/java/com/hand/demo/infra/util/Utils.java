package com.hand.demo.infra.util;

import io.choerodon.mybatis.util.StringUtil;
import io.seata.common.util.StringUtils;
import org.hzero.boot.interfaces.sdk.dto.RequestPayloadDTO;
import org.hzero.boot.interfaces.sdk.dto.ResponsePayloadDTO;
import org.hzero.boot.interfaces.sdk.invoke.InterfaceInvokeSdk;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils
 */
@Component
public class Utils {

    @Autowired
    private InterfaceInvokeSdk interfaceInvokeSdk;

    private Utils() {
    }


    public ResponsePayloadDTO invokeInterface(String jsonString, String namespace, String serverCode, String interfaceCode, String accessToken, MediaType mediaType) {
        //set payload
        RequestPayloadDTO requestPayloadDTO = new RequestPayloadDTO();
        requestPayloadDTO.setPayload(jsonString);

        //set header
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("Authorization", "bearer " + (StringUtils.isBlank(accessToken) ? TokenUtils.getToken() : accessToken));
        paramMap.put("Accept-Encoding", "gzip,deflate");
        paramMap.put("H-Invoke-Source-Type", "USECASE");
        requestPayloadDTO.setHeaderParamMap(paramMap);

        //set path param
        Map<String, String> pathParamMap = new HashMap<>();
        pathParamMap.put("organizationId", BaseConstants.DEFAULT_TENANT_ID.toString());
        requestPayloadDTO.setPathVariableMap(pathParamMap);

        //set media type
        requestPayloadDTO.setMediaType(mediaType.toString());

        return interfaceInvokeSdk.invoke(namespace, serverCode, interfaceCode, requestPayloadDTO);
    }
}
