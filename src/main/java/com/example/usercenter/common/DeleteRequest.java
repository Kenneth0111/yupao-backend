package com.example.usercenter.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求
 */

@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 7771479899016038750L;

    private Long id;
}
