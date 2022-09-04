package com.alibaba.tesla.authproxy.web.input;

/**
 * @author tandong
 * @Description:TODO
 * @date 2019/3/21 11:43
 */
public class TeslaUserListRequest {

    int page = 1;

    int size = 10;

    String loginName;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if(size > 500){
            size = 500;
        }
        this.size = size;

    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }
}
