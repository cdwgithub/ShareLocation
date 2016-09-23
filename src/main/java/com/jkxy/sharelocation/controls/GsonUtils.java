package com.jkxy.sharelocation.controls;

import com.google.gson.Gson;

/**
 * Created by X on 2016/5/14.
 */
public class GsonUtils {
    // 将JSON数据解析生成指定的类
    public static <T> T jsonToUserMessage(String jsonResult, Class<T> clz) {
        Gson gson = new Gson();
        T t = gson.fromJson(jsonResult, clz);
        return t;
    }

    // 将一个javaBean生成对应的Json数据
    public static String userMessageToJson(Object obj) {
        Gson gson = new Gson();
        String json = gson.toJson(obj);
        return json;
    }
}
