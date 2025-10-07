package com.dujiaqi.intelligentBI.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dujiaqi.intelligentBI.annotation.AuthCheck;
import com.dujiaqi.intelligentBI.api.QwenAiAPI;
import com.dujiaqi.intelligentBI.common.BaseResponse;
import com.dujiaqi.intelligentBI.common.DeleteRequest;
import com.dujiaqi.intelligentBI.common.ErrorCode;
import com.dujiaqi.intelligentBI.common.ResultUtils;
import com.dujiaqi.intelligentBI.constant.UserConstant;
import com.dujiaqi.intelligentBI.exception.BusinessException;
import com.dujiaqi.intelligentBI.exception.ThrowUtils;
import com.dujiaqi.intelligentBI.manager.RedisLimiterManager;
import com.dujiaqi.intelligentBI.model.dto.chart.*;
import com.dujiaqi.intelligentBI.model.entity.Chart;
import com.dujiaqi.intelligentBI.model.entity.User;
import com.dujiaqi.intelligentBI.model.vo.BiResponse;
import com.dujiaqi.intelligentBI.service.ChartService;
import com.dujiaqi.intelligentBI.service.UserService;
import com.dujiaqi.intelligentBI.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 队列测试
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
@Profile({"dev","local"})
public class QueueController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name){
        CompletableFuture.runAsync(() -> {
            System.out.println("任务执行中:" + name + ",执行人:" + Thread.currentThread().getName());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },threadPoolExecutor);
    }

    @GetMapping("/get")
    public String get(){
        Map<String,Object> map = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度",size);
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数",taskCount);
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数",completedTaskCount);
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("正在工作的线程数",activeCount);
        return JSONUtil.toJsonStr(map);
    }

}
