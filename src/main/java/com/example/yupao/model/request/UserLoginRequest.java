package com.example.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 7774981960374364415L;

    private String userAccount;

    private String userPassword;
}
