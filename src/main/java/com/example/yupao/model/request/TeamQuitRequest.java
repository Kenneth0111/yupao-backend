package com.example.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -5263235092835854952L;

    /**
     * id
     */
    private Long id;
}
