package com.platform.mall.service.mobile;

import java.util.Map;

public interface LitemallSystemConfigService {

    Map<String, String> queryAll();

    Map<String, String> listMail();

    Map<String, String> listWx();

    Map<String, String> listOrder();

    Map<String, String> listExpress();

    void updateConfig(Map<String, String> data);

    void addConfig(String key, String value);
}
