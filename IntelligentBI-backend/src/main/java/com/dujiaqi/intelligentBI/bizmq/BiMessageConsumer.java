package com.dujiaqi.intelligentBI.bizmq;

import com.dujiaqi.intelligentBI.api.QwenAiAPI;
import com.dujiaqi.intelligentBI.common.ErrorCode;
import com.dujiaqi.intelligentBI.exception.BusinessException;
import com.dujiaqi.intelligentBI.exception.ThrowUtils;
import com.dujiaqi.intelligentBI.model.entity.Chart;
import com.dujiaqi.intelligentBI.service.ChartService;
import com.dujiaqi.intelligentBI.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private QwenAiAPI qwenAiAPI;

    @Value("${qwen.api}")
    private String apiKey;

    @Value("${qwen.url}")
    private String url;

    @Value("${qwen.model}")
    private String model;

    // 指定程序监听的消息队列和确认机制
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME} , ackMode = "MANUAL")
    @SneakyThrows
    public void receiveMessage(String message , Channel channel ,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){

        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR , "消息为空");
        }
        Long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);

        if (chart == null) {
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR , "图表为空");
        }

        // 先修改任务状态为 "执行中"。等待执行成功后，修改为"已完成"，保存执行结果；执行失败后，状态修改为"失败"，记录失败信息
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean isSuccessUpdate = chartService.updateById(updateChart);
        if (!isSuccessUpdate) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图表执行中状态失败");
            return;
        }
        //调用 AI
        String answer = qwenAiAPI.doChat(apiKey, url, model, buildUserInput(chart));
        String[] splits = answer.split("【【【【【");
        if (splits.length < 3){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"AI 生成失败");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setStatus("succeed");
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        boolean isSuccess = chartService.updateById(updateChartResult);
        if (!isSuccess) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(),"更新图表成功状态失败");
        }

        // 消息确认
        log.info("receiveMessage message = {}",message);
        channel.basicAck(deliveryTag,false);
    }

    private String buildUserInput(Chart chart){
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String chartData = chart.getChartData();

        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象json代码， 合理地将数据进行可视化，不要生成任何多余\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";

        //处理用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append(prompt).append("\n");
        userInput.append("分析需求:").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        // 压缩后的数据
        String csvData = chartData;
        userInput.append("原始数据:").append(csvData).append("\n");

        return userInput.toString();
    }

    private void handleChartUpdateError(long chartId,String execMessage){
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean isSuccessUpdate = chartService.updateById(updateChart);
        ThrowUtils.throwIf(!isSuccessUpdate, ErrorCode.OPERATION_ERROR,"更新图表为失败状态失败");
    }

}
