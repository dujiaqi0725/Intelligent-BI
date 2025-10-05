package com.dujiaqi.intelligentBI.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dujiaqi.intelligentBI.model.entity.Chart;

import java.util.List;
import java.util.Map;

/**
* @author Lenovo
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2025-09-26 16:48:43
* @Entity com.dujiaqi.intelligentBI.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {


    List<Map<String,Object>> queryChartData(String querySql);

}




