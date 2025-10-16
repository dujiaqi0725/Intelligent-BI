package com.dujiaqi.intelligentBI.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 流式聊天完成响应模型
 */
@Data
public class StreamChatCompletionResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
    private List<Choice> choices;
    private Usage usage;

    /**
     * 选择项内部类
     */
    @Data
    public static class Choice {
        private Integer index;
        private Delta delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 增量内容内部类（流式响应特有的delta字段）
     */
    @Data
    public static class Delta {
        private String role;
        private String content;
        @JsonProperty("reasoning_content")
        private String reasoningContent;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    /**
     * 工具调用内部类（与原有结构保持一致）
     */
    @Data
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;
    }

    /**
     * 函数调用内部类
     */
    @Data
    public static class Function {
        private String name;
        private String arguments;
    }

    /**
     * 用量统计内部类
     */
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
    