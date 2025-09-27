package com.dujiaqi.intelligentBI.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dujiaqi.intelligentBI.constant.CommonConstant;
import com.dujiaqi.intelligentBI.mapper.ChartMapper;
import com.dujiaqi.intelligentBI.model.dto.chart.ChartQueryRequest;
import com.dujiaqi.intelligentBI.model.entity.Chart;
import com.dujiaqi.intelligentBI.model.entity.User;
import com.dujiaqi.intelligentBI.model.vo.UserVO;
import com.dujiaqi.intelligentBI.service.ChartService;
import com.dujiaqi.intelligentBI.service.UserService;
import com.dujiaqi.intelligentBI.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图表服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param chart
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validChart(Chart chart, boolean add) {
    }

    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        Long userId = chartQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 模糊查询
        // 精确查询
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(goal), "goal", goal);
        queryWrapper.eq(ObjectUtils.isNotEmpty(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取图表封装
     *
     * @param chart
     * @param request
     * @return
     */
    @Override
    public Chart getChart(Chart chart, HttpServletRequest request) {

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = chart.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        chart.setUserId(userVO.getId());
        // 2. 已登录，获取用户点赞、收藏状态
        long chartId = chart.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        // endregion

        return chart;
    }

    /**
     * 分页获取图表封装
     *
     * @param chartPage
     * @param request
     * @return
     */
    @Override
    public Page<Chart> getChartPage(Page<Chart> chartPage, HttpServletRequest request) {
        List<Chart> chartList = chartPage.getRecords();

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = chartList.stream().map(Chart::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> chartIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> chartIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> chartIdSet = chartList.stream().map(Chart::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
        }
        // 填充信息
        chartList.forEach(chart -> {
            Long userId = chart.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            chart.setUserId(userService.getUserVO(user).getId());
        });
        // endregion

        chartPage.setRecords(chartList);
        return chartPage;
    }

}
