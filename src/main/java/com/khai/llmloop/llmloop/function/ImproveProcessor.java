package com.khai.llmloop.llmloop.function;

import com.khai.llmloop.llmloop.entity.Context;

@FunctionalInterface
public interface ImproveProcessor {
    Context apply(Context currentItem, String improvedResult);
}
