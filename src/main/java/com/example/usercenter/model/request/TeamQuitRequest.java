package com.example.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = 5012030020849223485L;
    /**
     * id
     */
    private Long id;
}
