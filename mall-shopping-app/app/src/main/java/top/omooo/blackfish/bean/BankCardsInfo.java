package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/3/24.
 */

public class BankCardsInfo {
    private String logoUrl; //图标Url
    private String name;    //名字
    private String abbr;    //简称

    public BankCardsInfo(String logoUrl, String name, String abbr) {
        this.logoUrl = logoUrl;
        this.name = name;
        this.abbr = abbr;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbr() {
        return abbr;
    }

    public void setAbbr(String abbr) {
        this.abbr = abbr;
    }
}
