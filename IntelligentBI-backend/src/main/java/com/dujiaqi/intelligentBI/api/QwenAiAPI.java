package com.dujiaqi.intelligentBI.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.dujiaqi.intelligentBI.common.ErrorCode;
import com.dujiaqi.intelligentBI.exception.BusinessException;
import com.dujiaqi.intelligentBI.model.dto.api.CreateChatCompletionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QwenAiAPI {
    public CreateChatCompletionResponse doChat(String apiKey , String url , String model, String message){
        if (StringUtils.isBlank(apiKey)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有apiKey");
        }
        if (StringUtils.isBlank(url)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有url");
        }
        if (StringUtils.isBlank(model)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有model");
        }
        if (StringUtils.isBlank(message)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有提问的内容");
        }

        // 构造请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> queryMessages = new ArrayList<>();
        Map<String, String> queryMessage = new HashMap<>();
        queryMessage.put("role", "user");
        queryMessage.put("content", message);
        queryMessages.add(queryMessage);

        requestBody.put("messages", queryMessages);

        String json = JSONUtil.toJsonStr(requestBody);

        String result = HttpRequest.post(url)
                .header("Authorization" , "Bearer " + apiKey)
                .body(json)
                .execute()
                .body();
        return JSONUtil.toBean(result, CreateChatCompletionResponse.class);
    }

}
