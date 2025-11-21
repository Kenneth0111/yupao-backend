package com.example.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户包装类（脱敏）
 */

@Data
public class UserVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 简介
     */
    private String profile;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 标签列表 json
     */
    private String tags;

    /**
     *  状态 0-正常
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户角色 0 - 普通用户 1 - 管理员
     */
    private Integer userRole;

    /**
     * 星球编号
     */
    private String planetCode;

    private static final long serialVersionUID = 1L;
}
