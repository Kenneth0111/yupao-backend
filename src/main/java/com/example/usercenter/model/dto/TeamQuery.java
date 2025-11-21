package com.example.usercenter.model.dto;

import com.example.usercenter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Date;

/**
 * 队伍查询封装类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * id列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词（同时对队伍名称和描述进行搜索）
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 创建人id
     */
    private Long userId;

    /**
     * 0-公开,1-私有,2-加密
     */
    private Integer status;

}
