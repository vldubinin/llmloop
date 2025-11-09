package com.khai.llmloop.llmloop.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Context {
    private int id;
    private String sourceContext;
}
