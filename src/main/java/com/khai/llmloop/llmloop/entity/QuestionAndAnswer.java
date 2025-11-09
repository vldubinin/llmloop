package com.khai.llmloop.llmloop.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionAndAnswer extends Context {
    private String question;
    private String answer;
}
