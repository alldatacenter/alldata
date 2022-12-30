String.prototype.startWith=function(str){
    if(str==null||str==""||this.length==0||str.length>this.length)
      return false;
    if(this.substr(0,str.length)==str)
      return true;
    else
      return false;
    return true;
};

var provinceMap = {
    "上海":["上海市", "cn-sh"],
    "云南": ["云南省", "cn-yn"], 
    "内蒙古": ["内蒙古自治区", "cn-nm"], 
    // "台湾": ["台湾省", "tw-tw"], 
    "吉林": ["吉林省", "cn-jl"], 
    "四川": ["四川省", "cn-sc"], 
    "天津": ["天津市", "cn-tj"], 
    "宁夏": ["宁夏回族自治区", "cn-nx"], 
    "安徽": ["安徽省", "cn-ah"],
    "山东": ["山东省", "cn-sd"],
    "山西": ["山西省", "cn-sx"],
    "陕西": ["陕西省", "cn-sa"],
    "广东": ["广东省", "cn-gd"],
    "广西": ["广西省", "cn-gx"],
    "新疆": ["新疆维吾尔自治区", "cn-xj"],
    "江苏": ["江苏省", "cn-js"],
    "江西": ["江西省", "cn-jx"],
    "河北": ["河北省", "cn-hb"],
    "河南": ["河南省", "cn-he"],
    "浙江": ["浙江省", "cn-zj"],
    "海南": ["海南省", "cn-ha"],
    "湖北": ["湖北省", "cn-hu"],
    "湖南": ["湖南省", "cn-hn"],
    "甘肃": ["甘肃省", "cn-gs"],
    "福建": ["福建省", "cn-fj"],
    "西藏": ["西藏自治区", "cn-xz"],
    "贵州": ["贵州省", "cn-gz"],
    "辽宁": ["辽宁省", "cn-ln"],
    "重庆": ["重庆市", "cn-cq"],
    "青海": ["青海省", "cn-qh"],
    //"香港": ["香港特别行政区", ""],
    "黑龙江": ["黑龙江省", "cn-hl"],
    //"澳门": ["澳门特别行政区", ""],
    "北京": ["北京市", "cn-bj"]
};

var convertProvince = function(province) {
    for (var p in provinceMap) {
        if (province.startWith(p)) {
            return provinceMap[p];
        }
    }
    return null;
};
