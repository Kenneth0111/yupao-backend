package com.example.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = -7653838511843470527L;

    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;
}
