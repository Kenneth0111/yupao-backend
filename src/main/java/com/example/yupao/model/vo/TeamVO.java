package com.example.yupao.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeamVO implements Serializable {

    private static final long serialVersionUID = 4625414451962320667L;

    /**
     * id
     */
    private Long id;

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
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人id
     */
    private Long userId;

    /**
     * 0-公开, 1-加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人用户信息
     */
    private UserVO creatorUser;

    /**
     * 是否已加入队伍
     */
    private Boolean hasJoined;

    /**
     * 已加入的用户数
     */
    private Integer memberCount;

}
