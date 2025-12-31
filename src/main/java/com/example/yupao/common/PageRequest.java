package com.example.yupao.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -207471148513391766L;
    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 当前页码
     */
    protected int pageNum = 1;
}
