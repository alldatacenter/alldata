/**
 * util function
 * 
 */

const UtilFn = {
    // 获取location search参数，返回一个search对象
    getLocationSearchObj: function(qstring) {
        let splitUrl = qstring.split("?");
        let strUrl = (splitUrl.length > 1) ? decodeURIComponent(splitUrl[1]).split("&") : [];
        let str = "";
        let obj = {};
        for (let i = 0, l = strUrl.length; i < l; i++) {
            str = strUrl[i].split("=");
            obj[str[0]] = str[1];
        }
        return Array.prototype.sort.call(obj);
    },

    // 判断是否空对象
    isEmptyObject: function(e) {
        var t;  
        for (t in e)  
            return !1;  
        return !0  
    },

	// 设置cookie
    setCookie: function(name, value, days) {
        days = days || 0;
        let expires = "";
        if (days != 0) { //设置cookie过期时间  
            let date = new Date();
            let ms = days * 24 * 60 * 60 * 1000;
            date.setTime(date.getTime() + ms);
            expires = "; expires=" + date.toGMTString();
        }
        if (days == Infinity) { // 设置最大过期时间
            expires = "; expires=Fri, 31 Dec 9999 23:59:59 GMT";
        }
        document.cookie = name + "=" + value + expires + "; path=/";
    },

    // 获取cookie
    getCookie: function(name) {
        let nameEQ = name + "=";
        let ca = document.cookie.split(';'); //把cookie分割成组  
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i]; //取得字符串  
            while (c.charAt(0) == ' ') { //判断一下字符串有没有前导空格  
                c = c.substring(1, c.length); //有的话，从第二位开始取  
            }
            if (c.indexOf(nameEQ) == 0) { //如果含有我们要的name  
                return c.substring(nameEQ.length, c.length);
            }
        }
        return false;
    },

    // 清除cookies
    clearCookie: function(name) {
        this.setCookie(name, "", -1);
    },

    // 绑定多个函数到onresize上
    addEventOnResize: function (callback) {
        let originFn = window.onresize;
        window.onresize = function () {
            originFn && originFn();
            callback();
        }
    },
    // 深度克隆
    cloneObj: function(obj) {
        let newObj = obj instanceof Array ? [] : {};
        for(let i in obj) {
            let item = obj[i];
            newObj[i] = typeof item === 'object' ? this.cloneObj(item) : item;
        }
        return newObj;
    }
};

export default UtilFn;