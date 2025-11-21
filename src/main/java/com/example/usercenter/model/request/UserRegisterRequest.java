package com.example.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author 张博洋
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 6881330052174877354L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;

    private String planetCode;

}
