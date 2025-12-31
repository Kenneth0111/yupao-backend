package com.example.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -1739478411426452342L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;

}
