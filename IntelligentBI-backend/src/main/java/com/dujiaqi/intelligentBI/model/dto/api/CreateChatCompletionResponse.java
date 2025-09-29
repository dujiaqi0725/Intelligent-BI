package com.dujiaqi.intelligentBI.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateChatCompletionResponse {
    private String id;
    private List<Choice> choices;
    private Usage usage;
    private Long created;
    private String model;
    private String object;

    @Data
    public static class Choice {
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        @JsonProperty("reasoning_content")
        private String reasoningContent;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Data
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;
    }

    @Data
    public static class Function {
        private String name;
        private String arguments;
    }

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