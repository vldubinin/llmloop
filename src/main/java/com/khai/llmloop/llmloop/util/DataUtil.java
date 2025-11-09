package com.khai.llmloop.llmloop.util;

import com.khai.llmloop.llmloop.entity.Context;

import java.util.List;

public class DataUtil {
    public static <DataType extends Context> DataType findById(List<DataType> dataList, int id) {
        return dataList.stream().filter(item -> item.getId() == id).findFirst().orElse(null);
    }


    public static <DataType extends Context> List<DataType> initIds(List<DataType> contexts) {
        for (int i = 0; i < contexts.size(); i++) {
            contexts.get(i).setId(i);
        }
        return contexts;
    }

    public static <DataType extends Context> List<DataType> cleanContexts(List<DataType> contexts) {
        for (DataType context : contexts) {
            context.setSourceContext(null);
        }
        return contexts;
    }
}
