package com.example.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamDeleteRequest implements Serializable {

    private static final long serialVersionUID = 2160223726876885648L;

    private Long id;
}
