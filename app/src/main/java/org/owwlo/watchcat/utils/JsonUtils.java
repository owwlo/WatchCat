package org.owwlo.watchcat.utils;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Type;

public class JsonUtils {
    public static String toJson(Object data) {
        return JSON.toJSONString(data);
    }

    public static <T> T parseJson(String json, Type type) {
        return JSON.parseObject(json, type);
    }
}
