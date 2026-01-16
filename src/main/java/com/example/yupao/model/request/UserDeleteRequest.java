package com.example.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1472557763205638109L;

    private Long id;
}
