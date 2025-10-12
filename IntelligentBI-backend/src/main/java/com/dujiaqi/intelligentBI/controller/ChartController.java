package com.dujiaqi.intelligentBI.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dujiaqi.intelligentBI.annotation.AuthCheck;
import com.dujiaqi.intelligentBI.api.QwenAiAPI;
import com.dujiaqi.intelligentBI.bizmq.BiMessageProducer;
import com.dujiaqi.intelligentBI.common.BaseResponse;
import com.dujiaqi.intelligentBI.common.DeleteRequest;
import com.dujiaqi.intelligentBI.common.ErrorCode;
import com.dujiaqi.intelligentBI.common.ResultUtils;
import com.dujiaqi.intelligentBI.constant.UserConstant;
import com.dujiaqi.intelligentBI.exception.BusinessException;
import com.dujiaqi.intelligentBI.exception.ThrowUtils;
import com.dujiaqi.intelligentBI.manager.RedisLimiterManager;
import com.dujiaqi.intelligentBI.model.dto.api.CreateChatCompletionResponse;
import com.dujiaqi.intelligentBI.model.dto.chart.*;
import com.dujiaqi.intelligentBI.model.entity.Chart;
import com.dujiaqi.intelligentBI.model.entity.User;
import com.dujiaqi.intelligentBI.model.vo.BiResponse;
import com.dujiaqi.intelligentBI.service.ChartService;
import com.dujiaqi.intelligentBI.service.UserService;
import com.dujiaqi.intelligentBI.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.weaver.ast.Var;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private QwenAiAPI qwenAiAPI;

    @Value("${qwen.api}")
    private String apiKey;

    @Value("${qwen.url}")
    private String url;

    @Value("${qwen.model}")
    private String model;

    // region 增删改查

    /**
     * 创建图表
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chartAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        // 数据校验
        chartService.validChart(chart, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除图表
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = chartService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图表（仅管理员可用）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        // 数据校验
        chartService.validChart(chart, false);
        // 判断是否存在
        long id = chartUpdateRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = chartService.updateById(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图表（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Chart chart = chartService.getById(id);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(chartService.getChart(chart, request));
    }

    /**
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 查询数据库
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前登录用户创建的图表列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(chartQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        // 获取封装类
        return ResultUtils.success(chartService.getChartPage(chartPage, request));
    }

    /**
     * 编辑图表（给用户使用）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        // 数据校验
        chartService.validChart(chart, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = chartEditRequest.getId();
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = chartService.updateById(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 数据分析 （同步）
     *
     * @param multipartFile excel文件
     * @param genChartByAiRequest 请求参数
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genCharByAi(@RequestPart("file") MultipartFile multipartFile,
                                                GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "图表名称不能为空");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        //校验文件后缀
        final List<String> validFileSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg");
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.SYSTEM_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

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
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据:").append(csvData).append("\n");

        String answer = qwenAiAPI.doChat(apiKey, url, model, userInput.toString());
        String[] splits = answer.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(userGoal);
        chart.setChartData(csvData);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setChartType(chartType);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 数据分析 （异步）
     *
     * @param multipartFile excel文件
     * @param genChartByAiRequest 请求参数
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genCharByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                            GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal) , ErrorCode.PARAMS_ERROR , "分析目标不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(name) , ErrorCode.PARAMS_ERROR , "图表名称不能为空");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB , ErrorCode.PARAMS_ERROR , "文件超过 1M");
        //校验文件后缀
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls ");
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix) , ErrorCode.SYSTEM_ERROR , "文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

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
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据:").append(csvData).append("\n");

        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(userGoal);
        chart.setChartData(csvData);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setChartType(chartType);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");

        CompletableFuture.runAsync(() -> {
            // 先修改任务状态为 "执行中"。等待执行成功后，修改为"已完成"，保存执行结果；执行失败后，状态修改为"失败"，记录失败信息
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean isSuccessUpdate = chartService.updateById(updateChart);
            if (!isSuccessUpdate) {
                handleChartUpdateError(chart.getId(),"更新图表执行中状态失败");
                return;
            }
            //调用 AI
            String answer = qwenAiAPI.doChat(apiKey, url, model, userInput.toString());
            String[] splits = answer.split("【【【【【");
            if (splits.length < 3){
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
                handleChartUpdateError(chart.getId(),"更新图表成功状态失败");
            }
        },threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 数据分析 （异步消息队列）
     *
     * @param multipartFile excel文件
     * @param genChartByAiRequest 请求参数
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genCharByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                     GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal) , ErrorCode.PARAMS_ERROR , "分析目标不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(name) , ErrorCode.PARAMS_ERROR , "图表名称不能为空");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB , ErrorCode.PARAMS_ERROR , "文件超过 1M");
        //校验文件后缀
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls ");
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix) , ErrorCode.SYSTEM_ERROR , "文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

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
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据:").append(csvData).append("\n");

        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(userGoal);
        chart.setChartData(csvData);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setChartType(chartType);
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");
        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(long chartId,String execMessage){
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean isSuccessUpdate = chartService.updateById(updateChart);
        ThrowUtils.throwIf(!isSuccessUpdate,ErrorCode.OPERATION_ERROR,"更新图表为失败状态失败");
    }

}
