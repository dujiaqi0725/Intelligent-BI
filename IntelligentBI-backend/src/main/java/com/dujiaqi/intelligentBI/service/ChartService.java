package com.dujiaqi.intelligentBI.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dujiaqi.intelligentBI.model.dto.chart.ChartQueryRequest;
import com.dujiaqi.intelligentBI.model.entity.Chart;

import javax.servlet.http.HttpServletRequest;

/**
 * 图表服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface ChartService extends IService<Chart> {

    /**
     * 校验数据
     *
     * @param chart
     * @param add 对创建的数据进行校验
     */
    void validChart(Chart chart, boolean add);

    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);
    
    /**
     * 获取图表封装
     *
     * @param chart
     * @param request
     * @return
     */
    Chart getChart(Chart chart, HttpServletRequest request);

    /**
     * 分页获取图表封装
     *
     * @param chartPage
     * @param request
     * @return
     */
    Page<Chart> getChartPage(Page<Chart> chartPage, HttpServletRequest request);
}
