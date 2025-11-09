package com.khai.llmloop.llmloop.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Concept extends Context {
    private String conceptSummary;
}
