package com.zhang.autotouch.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataUtil {
    public static Map<String, String> map = new HashMap<>();
    public static List<String> list = new ArrayList<>();

    public static void createTestData() {
        map.put("请填写姓名", "夏超");
        map.put("请填写证件号", "421126198602162638");
        map.put("请填写年龄", "36");
        map.put("请填写居住地址", "江苏省南京市");
        map.put("请填写手机号", "13151555688");


        list.add("夏超");
        list.add("421126198602162638");
        list.add("36");
        list.add("江苏省南京市");
        list.add("13151555688");

    }


    public static Set<String> getKeySet() {
        if (map.isEmpty()) {
            return null;
        }
        return map.keySet();
    }

    public static void remove(String key) {
        map.remove(key);
    }

    public static String get(String key) {
        if (map.isEmpty()) {
            return null;
        }
        return map.get(key);
    }

    public static String getNextValue() {
        if (!list.isEmpty()) {
            String value = list.remove(0);
            return value;
        }
        return "";
    }

}
