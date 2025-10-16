package com.dujiaqi.intelligentBI.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dujiaqi.intelligentBI.common.ErrorCode;
import com.dujiaqi.intelligentBI.exception.BusinessException;
import com.dujiaqi.intelligentBI.model.dto.api.CreateChatCompletionResponse;
import com.dujiaqi.intelligentBI.model.dto.api.StreamChatCompletionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class QwenAiAPI {
    public String doChat(String apiKey , String url , String model, String message){
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
        CreateChatCompletionResponse createChatCompletionResponse = JSONUtil.toBean(result, CreateChatCompletionResponse.class);
        return createChatCompletionResponse.getChoices().get(0).getMessage().getContent();
    }

    /**
     * 通义千问流式输出实现
     * @param apiKey API密钥
     * @param url API地址
     * @param model 模型名称
     * @param message 用户消息
     * @param consumer 回调函数，用于处理每一段输出
     */
    public void doChatStream(String apiKey, String url, String model, String message, Consumer<String> consumer) {
        // 参数校验
        if (StringUtils.isAnyBlank(apiKey, url, model, message)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 构建请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true); // 开启流式输出

        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        String jsonBody = JSONUtil.toJsonStr(requestBody);

        // 发送请求并处理流式响应
        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .body(jsonBody)
                .execute()) {

            // 检查响应状态
            if (response.getStatus() != 200) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "API请求失败，状态码：" + response.getStatus() + "，响应：" + response.body());
            }

            // 处理流式响应
            try (InputStream inputStream = response.bodyStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                // 逐行读取SSE事件
                while ((line = reader.readLine()) != null) {
                    // 过滤空行或结束标记[DONE]
                    if (StringUtils.isBlank(line) || line.equals("data: [DONE]")) {
                        continue;
                    }

                    // 去除 "data: " 前缀（流式响应的标准格式）
                    String jsonStr = line.startsWith("data: ")
                            ? line.substring("data: ".length()).trim()
                            : line.trim();

                    StreamChatCompletionResponse streamChatCompletionResponse = JSONUtil.toBean(jsonStr, StreamChatCompletionResponse.class);
                    String result = streamChatCompletionResponse.getChoices().get(0).getDelta().getContent();
                    if (StringUtils.isNotBlank(result)) {
                        System.out.println(result);
                    }
                }
            }
        } catch (IOException e) {
            log.error("流式请求异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式请求处理失败");
        }
    }

}
