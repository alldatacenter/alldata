package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;
import java.util.List;

/**
 * 修改语言的返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class TeslaUserLangListResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private List<String> langs;

    public List<String> getLangs() {
        return langs;
    }

    public void setLangs(List<String> langs) {
        this.langs = langs;
    }

}
