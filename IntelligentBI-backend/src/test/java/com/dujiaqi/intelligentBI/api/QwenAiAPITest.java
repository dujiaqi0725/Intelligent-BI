package com.dujiaqi.intelligentBI.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QwenAiAPITest {


    @Resource
    private QwenAiAPI qwenAiAPI;

    @Test
    void doChatStream() {
        // 调用流式方法，实时打印每段内容
        qwenAiAPI.doChatStream(
                "sk-jjzkmtekstrefcqffveieehlwntolzfidgwgsamxfsvgxeth",
                "https://api.siliconflow.cn/v1/chat/completions",
                "Qwen/Qwen3-235B-A22B-Instruct-2507",
                "请逐字输出“测试流式输出”",
                segment -> {
                    System.out.print(segment);
                    System.out.flush();
                } // 实时输出每一段内容
        );
    }
}