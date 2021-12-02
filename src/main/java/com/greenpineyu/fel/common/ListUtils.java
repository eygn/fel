package com.greenpineyu.fel.common;

import java.util.List;

/**
 * @author byg
 * @date 2021-12-02 13:47:55
 */
public class ListUtils {

    /**
     * @param list
     * @return
     */
    public static List<Integer> convert(List<String> list) {
        return list.stream().map(str -> com.greenpineyu.fel.common.NumberUtil.toInteger(str)).collect(java.util.stream.Collectors.toList());
    }

}
