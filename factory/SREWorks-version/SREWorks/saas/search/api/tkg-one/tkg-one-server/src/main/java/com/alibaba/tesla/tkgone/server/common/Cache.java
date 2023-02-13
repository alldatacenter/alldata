package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * 统一的内存缓存
 */
public class Cache {

    public static Map<String, List<ConfigDto>> allConfigDto = new Hashtable<>();

}
