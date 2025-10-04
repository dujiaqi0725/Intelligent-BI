package com.dujiaqi.intelligentBI.model.vo;

import lombok.Data;

@Data
public class BiResponse {

    /**
     * 生成的图表
     */
    private String genChart;

    /**
     * 生成的结论
     */
    private String genResult;


    /**
     * 生成的图表id
     */
    private Long chartId;

}
