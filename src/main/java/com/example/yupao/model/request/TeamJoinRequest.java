package com.example.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamJoinRequest implements Serializable {


    private static final long serialVersionUID = -7832171150332429845L;

    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;
}
