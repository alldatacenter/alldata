/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 获取url的参数
 * @name $.getUrlParam
 * @function
 * @param {String} [name] 不传值则返回所有参数组成的object
 * @param {String} [url] 需要解析的链接，不填默认是location.href
 * @returns {String|Object}
 * @example
 * $.getUrlParam('name'); //返回name对应的参数值
 * $.getUrlParam(); //返回所有参数组成的object
 */
(function ($) {
    $.extend({
        "getUrlParam": function (name, url) {
            var str = (url || location.href),
                index = str.indexOf('?'),
                n, arr, returnOobj = {};
            if (index != -1) {
                str = str.substring(index + 1);
                arr = str.split('#')[0].split('&');
            } else {
                arr = [];
            }
            for (n = 0; n < arr.length; ++n) {
                arr[n] = arr[n].split('=');
                returnOobj[arr[n][0]] = returnOobj[arr[n][0].toLowerCase()] = arr[n][1];
            }
            if (name) {
                return returnOobj[name] || '';
            } else {
                return returnOobj;
            }
        }
    });
})(jQuery);

/**
 * cookie操作模块
 * @name $.cookie
 * @function
 * @param {String} key
 * @param {String} [value]
 * @param {{expires:Number|'forever', domain:String, path:String , inCommon : boolean , secure :
 *     boolean}} [options]
 * @returns {String}
 * @example
 * $.cookie(key); //读取cookie
 * $.cookie(key, null); //删除cookie
 * $.cookie(key, value, {"expires":forever,"domain":"duo.qq.com","path":"/" , inCommon : false});
 *     //设置cookie
 */
(function ($) {
    $.extend({
        "cookie": function (key, value, options) {
            // #lizard forgives
            var cookiex = $.cookie,
                options = options || {},
                ret,
                result,
                decode = options.raw ? function (s) {
                    return s;
                } : decodeURIComponent,
                getRet = function (tkey) {
                    var tret = '';
                    if (result = new RegExp('(?:^|; )' + encodeURIComponent(tkey) + '=([^;]*)').exec(document.cookie)) {
                        try {
                            tret = decode(result[1]);
                        } catch (e) {
                            tret = result[1];
                        }
                    }
                    return tret;
                };

            // key and value given, set cookie...
            if (arguments.length > 1 && (value === null || typeof value !== "object")) {

                if (value === null) {


                    //删除forever里面的东西,只改key和expires，在最后才写cookie
                    var forever = getRet("duo_common_forever"),
                        inCommon = false,
                        oldKey = key,
                        oldDomain = options.domain,
                        oldPath = options.path;

                    if (forever != '') {
                        var foreverList = forever.split('&');
                        for (var i = 0; i < foreverList.length; i++) {
                            if (foreverList[i].split('=')[0] == key) {
                                foreverList.splice(i, 1);
                                key = "duo_common_forever";
                                value = foreverList.join("&");
                                options.expires = new Date(0xfffffffffff);
                                options.domain = ".qq.com";
                                options.path = "/";
                                inCommon = true;
                                break;
                            }
                        }
                    }

                    //删除session里面的东西,只改key和expires，在最后才写cookie
                    if (!inCommon) {
                        var session = getRet("duo_common_session");
                        if (session != '') {
                            var sessionList = session.split('&');
                            for (var i = 0; i < sessionList.length; i++) {
                                if (sessionList[i].split('=')[0] == key) {
                                    sessionList.splice(i, 1);
                                    key = "duo_common_session";
                                    value = sessionList.join("&");
                                    options.expires = null;
                                    options.domain = ".qq.com";
                                    options.path = "/";
                                    inCommon = true;
                                    break;
                                }
                            }
                        }
                    }

                    //如果不在公用cookie里面就直接在最后删除原生cookie就行了，如果在公用cookie里面就改变
                    if (!inCommon) {
                        value = '';
                        options.expires = new Date(0);
                    } else {
                        //删除原生cookie,带直接写cookie操作
                        document.cookie = [
                            encodeURIComponent(oldKey),
                            '=',
                            "",
                            '; expires=' + (new Date(0)).toGMTString(),
                            oldPath ? '; path=' + oldPath : '; path=/',
                            oldDomain ? '; domain=' + oldDomain : '; domain=.qq.com',
                            options.secure ? '; secure' : ''
                        ].join('');
                    }
                } else {
                    //写cookie
                    if (typeof options.expires === 'number') {
                        //计时cookie，以天计
                        var days = options.expires,
                            t = options.expires = new Date();
                        t.setDate(t.getDate() + days);
                    } else if (typeof options.expires === 'string' && options.expires
                        != 'forever') {
                        //计时cookie ,根据开发者输入的尾缀定单位
                        var t = parseInt(options.expires),
                            suffix = options.expires[options.expires.length - 1],
                            now = new Date();
                        if (suffix == "s") {
                            now.setSeconds(now.getSeconds() + t);
                            options.expires = now;
                        } else if (suffix == "m") {
                            now.setMinutes(now.getMinutes() + t);
                            options.expires = now;
                        } else if (suffix == "h") {
                            now.setHours(now.getHours() + t);
                            options.expires = now;
                        } else if (suffix == "d") {
                            now.setDate(now.getDate() + t)
                            options.expires = now;
                        } else if (suffix == "M") {
                            now.setMonth(now.getMonth() + t);
                            options.expires = now;
                        }
                    } else if (options.expires == 'forever') {
                        //永久cookie
                        options.expires = new Date(0xfffffffffff);
                        if (options.inCommon) {
                            //如果使用公用cookie，强制使用domain : .qq.com这个和 path : /
                            options.domain = ".qq.com";
                            options.path = "/";
                            var forever = cookiex("duo_common_forever");
                            if (forever != '') {

                                var retList = forever.split('&'),
                                    inForever = false;

                                for (var i = 0; i < retList.length; i++) {
                                    if (retList[i].split('=')[0] == key) {
                                        retList[i] =
                                            key + '=' + encodeURIComponent(String(value));
                                        inForever = true;
                                        break;
                                    }
                                }

                                if (!inForever) {
                                    value =
                                        forever + "&" + key + '=' + encodeURIComponent(
                                            String(value));
                                } else {
                                    value = retList.join('&');
                                }

                            } else {
                                value = key + '=' + encodeURIComponent(String(value));
                            }
                            key = "duo_common_forever";
                        }
                    } else if ((typeof (options.expires) === "object") && (options.expires instanceof Date)) {
                        //用户自己传入Date对象
                    } else {
                        //浏览器进程cookie
                        options.expires = null;
                        if (options.inCommon) {
                            //如果使用公用cookie，强制使用domain : .qq.com这个和 path : /
                            options.domain = ".qq.com";
                            options.path = "/";
                            var session = cookiex("duo_common_session");
                            if (session != '') {

                                var retList = session.split('&'),
                                    inSession = false;

                                for (var i = 0; i < retList.length; i++) {
                                    if (retList[i].split('=')[0] == key) {
                                        retList[i] =
                                            key + '=' + encodeURIComponent(String(value));
                                        inSession = true;
                                        break;
                                    }
                                }

                                if (!inSession) {
                                    value =
                                        session + "&" + key + '=' + encodeURIComponent(
                                            String(value));
                                } else {
                                    value = retList.join('&');
                                }
                            } else {
                                value = key + '=' + encodeURIComponent(String(value));
                            }
                            key = "duo_common_session";
                        }
                    }

                }

                //执行操作
                return (document.cookie = [
                    encodeURIComponent(key), '=',
                    options.raw ? String(value) : encodeURIComponent(String(value)),
                    options.expires ? '; expires=' + options.expires.toGMTString() : '', // use
                                                                                         // expires
                                                                                         // attribute,
                                                                                         // max-age
                                                                                         // is
                                                                                         // not
                                                                                         // supported
                                                                                         // by
                                                                                         // IE
                    options.path ? '; path=' + options.path : '; path=/',
                    options.domain ? '; domain=' + options.domain : '; domain=.qq.com',
                    options.secure ? '; secure' : ''
                ].join(''));
            }

            // key and possibly options given, get cookie...
            options = value || {};
            ret = getRet(key);

            //查找永久公用cookie
            if (ret == '' && key != 'duo_common_forever') {
                ret = getRet("duo_common_forever");
                if (ret != '') {
                    var retList = ret.split('&');
                    for (var i = 0; i < retList.length; i++) {
                        if (retList[i].split('=')[0] == key) {
                            ret = decodeURIComponent(retList[i].split('=')[1]);
                            return ret;
                        }
                    }
                }
                ret = '';
            }

            //查找浏览器进程公用cookie
            if (ret === '' && key != 'duo_common_session') {
                ret = getRet("duo_common_session");
                if (ret !== '') {
                    var retList = ret.split('&');
                    for (var i = 0; i < retList.length; i++) {
                        if (retList[i].split('=')[0] == key) {
                            ret = decodeURIComponent(retList[i].split('=')[1]);
                            return ret;
                        }
                    }
                }
                ret = '';
            }

            return ret;
        }
    });
})(jQuery);