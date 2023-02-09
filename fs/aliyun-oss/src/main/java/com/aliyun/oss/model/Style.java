package com.aliyun.oss.model;

import java.util.Date;

public class Style {
    private String styleName;
    private String style;
    private Date creationDate;
    private Date lastModifyTime;

    public String GetStyleName() {
        return this.styleName;
    }

    public String GetStyle() {
        return this.style;
    }

    public Date GetCreationDate() {
        return this.creationDate;
    }

    public Date GetLastModifyTime() {
        return this.lastModifyTime;
    }

    public void SetStyleName(String styleName) {
        this.styleName = styleName;
    }

    public void SetStyle(String style) {
        this.style = style;
    }

    public void SetCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void SetLastModifyTime(Date lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }
}
