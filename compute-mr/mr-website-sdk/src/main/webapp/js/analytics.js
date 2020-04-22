(function () {
  var CookieUtil = {
    //get the name/value pair to browser cookie
    get: function (name) {
      var cookieName = encodeURIComponent(name) + "=",
          cookieStart = document.cookie.indexOf(cookieName),
          cookieValue = null;

      if (cookieStart > -1) {
        var cookieEnd = document.cookie.indexOf(";", cookieStart);
        if (cookieEnd == -1) {
          cookieEnd = document.cookie.length;
        }
        cookieValue = decodeURIComponent(
            document.cookie.substring(cookieStart + cookieName.length,
                cookieEnd));
      }
      return cookieValue;
    },
    //set the name/value pair to browser cookie
    set: function (name, value, expires, path, domain, secure) {
      var cookieText = encodeURIComponent(name) + "=" + encodeURIComponent(
          value);
      if (expires) {
        //set expires time
        var expiresTime = new Date();
        expiresTime.setTime(expires);
        cookieText += ";expires=" + expiresTime.toUTCString();
      }

      if (path) {
        cookieText += ";path=" + path;
      }

      if (domain) {
        cookieText += ";domain=" + domain;
      }

      if (secure) {
        cookieText += ";secure=" + secure;
      }

      document.cookie = cookieText;

    },

    setExt: function (name, value) {
      this.set(name, value, new Date().getTime() + 315360000000, "/");
    }
  };

  var tracker = {
    clientConfig: {
      serverUrl: "http://192.168.52.156/BfImg.gif",
      sessionTimeout: 360, //6mins
      maxWaitTime: 3600, //1hour
      ver: "1"
    },

    cookieExpiresTime: 315360000000, //cookie expire time 10 year

    columns: {
      //发送到服务器的列名称
      eventName: "en",
      version: "ver",
      platform: "pl",
      sdk: "sdk",
      uuid: "u_ud",
      memberId: "u_mid",
      sessionId: "u_sd",
      clientTime: "c_time",
      language: "l",
      userAgent: "b_iev",
      resolution: "b_rst",
      currentUrl: "p_url",
      referrerUrl: "p_ref",
      title: "tt",
      orderId: "oid",
      orderName: "on",
      currencyAmount: "cua",
      currencyType: "cut",
      paymentType: "pt",

      category: "ca",
      action: "ac",
      kv: "kv_",
      duration: "du",
    },

    keys: {
      pageView: "e_pv",
      chargeRequestEvent: "e_crt",
      launch: "e_l",
      eventDurationEvent: "e_e",
      sid: "bftrack_sid",
      uuid: "bftrack_uuid",
      mid: "bftrack_mid",
      preVisitTime: "bftrack_previsit"
    },

    getEventKeys: function () {
      //返回全部event的名称数组
      return [
        this.keys.pageView,
        this.keys.chargeRequestEvent,
        this.keys.launch,
        this.key.eventDurationEvent
      ]

    },

    getSid: function () {
      return CookieUtil.get(this.keys.sid);
    },

    setSid: function (sid) {
      if (sid) {
        CookieUtil.setExt(this.keys.sid, sid);
      }
    },

    getUuid: function () {
      return CookieUtil.get(this.keys.uuid);
    },

    setUuid: function (uuid) {
      if (uuid) {
        CookieUtil.setExt(this.keys.uuid, uuid);
      }
    },

    getMemberId: function () {
      return CookieUtil.get(this.keys.mid);
    },

    setMemberId: function (memberId) {
      if (memberId) {
        CookieUtil.setExt(this.keys.mid, memberId);
      }
    },

    startSession: function () {
      //加载js就触发的事件
      if (this.getSid()) {
        if (this.isSessionTimeout()) {
          //会话过期
          this.createNewSession();
        } else {
          this.updatePreVisitTime(new Date().getTime());
        }
      } else {
        this.createNewSession();
      }

      this.onPageView();
    },
    preCallApi: function () {
      if (this.isSessionTimeout()){
        this.startSession();
      }else{
        this.updatePreVisitTime(new Date().getTime())
      }
      return true;
    },
    onPageView: function () {
      //触发page view事件
      if (this.preCallApi()) {
        var pageViewEvent = {}
        pageViewEvent[this.columns.eventName] = this.keys.pageView;
        pageViewEvent[this.columns.currentUrl] = window.location.href;
        pageViewEvent[this.columns.referrerUrl] = document.referrer;
        pageViewEvent[this.columns.title] = document.title;
        this.setCommonColumns(pageViewEvent);
        this.sendDataToServer(this.parseParam(pageViewEvent));
        this.updatePreVisitTime(new Date().getTime());
      }
    },
    onChargeRequest: function (orderId, name, currencyAmount, currencyType,
        paymentType) {
      if (this.preCallApi()) {
        if (!orderId || !currencyType || !paymentType) {
          this.log("orderId, currencyType and paymentType not null");
          return;
        }

        if (typeof(currencyAmount) == "number") {
          var chargeRequestEvent = {}
          chargeRequestEvent[this.columns.eventName] = this.keys.chargeRequestEvent;
          chargeRequestEvent[this.columns.orderId] = orderId;
          chargeRequestEvent[this.columns.orderName] = name;
          chargeRequestEvent[this.columns.currencyAmount] = currencyAmount;
          chargeRequestEvent[this.columns.currencyType] = currencyType;
          chargeRequestEvent[this.columns.paymentType] = paymentType;
          this.setCommonColumns(chargeRequestEvent);
          this.sendDataToServer(this.parseParam(chargeRequestEvent));
          this.updatePreVisitTime(new Date().getTime());
        } else {
          this.log("currencyAmount must be number ")
          return;
        }
      }
    },
    onEventDuration: function (category, action, map, duration) {
      //触发event事件
      if (this.preCallApi()) {
        if (category && action) {
          var event = {}
          event[this.columns.eventName] = this.keys.eventDurationEvent;
          event[this.columns.category] = category;
          event[this.columns.action] = action;
          if (map) {
            for (var k in map) {
              if (k && map[k]) {
                event[this.columns.kv + k] = map[k];
              }
            }
          }
          if (duration) {
            event[this.columns.duration] = duration;
          }
          this.setCommonColumns(event);
          this.sendDataToServer(this.parseParam(event));
          this.updatePreVisitTime(new Date().getTime());
        }
        else {
          this.log("category and action not null")
        }
      }

    },
    sendDataToServer: function (data) {
      //data是String
      var that = this;
      var i2 = new Image(1, 1);
      i2.onerror = function () {
        //重试
      };
      i2.src = this.clientConfig.serverUrl + "?" + data;
    },

    setCommonColumns: function (data) {
      data[this.columns.version] = this.clientConfig.ver;
      data[this.columns.platform] = "website";
      data[this.columns.sdk] = "js";
      data[this.columns.uuid] = this.getUuid();
      data[this.columns.sessionId] = this.getSid();
      data[this.columns.memberId] = this.getMemberId();
      data[this.columns.clientTime] = new Date().getTime();
      data[this.columns.language] = window.navigator.language;
      data[this.columns.userAgent] = window.navigator.userAgent;
      data[this.columns.resolution] = screen.width * screen.height;

    },
    onLaunch: function () {
      var launch = {};
      launch[this.columns.eventName] = this.keys.launch;//设置事件名称
      this.setCommonColumns(launch);
      this.sendDataToServer(this.parseParam(launch));
    },

    parseParam: function (data) {
      var params = "";
      for (var e in data) {
        if (e && data) {
          params += encodeURIComponent(e) + "=" + encodeURIComponent(data[e])
              + "&";
        }
      }
      if (params) {
        return params.substring(0, params.length - 1);
      } else {
        return params;
      }

    },

    generateId: function () {
      var CHARS = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split(
          '');
      var chars = CHARS, uuid = new Array(36), rnd = 0, r;
      for (var i = 0; i < 36; i++) {
        if (i == 8 || i == 13 || i == 18 || i == 23) {
          uuid[i] = '-';
        } else if (i == 14) {
          uuid[i] = '4';
        } else {
          if (rnd <= 0x02) {
            rnd = 0x2000000 + (Math.random() * 0x1000000) | 0;
          }
          r = rnd & 0xf;
          rnd = rnd >> 4;
          uuid[i] = chars[(i == 19) ? (r & 0x3) | 0x8 : r];
        }
      }
      return uuid.join('');
    },

    //创建新会话，如果是第一次会话，创建lunch事件
    createNewSession: function () {
      var sid = this.generateId();
      var time = new Date().getTime();
      this.setSid(sid);
      this.updatePreVisitTime(time);
      if (!this.getUuid()) {
        //uuid不存在
        var uuid = this.generateId();
        this.setUuid(uuid);
        this.onLaunch();
      }
    },

    isSessionTimeout: function () {
      var time = new Date().getTime();
      var preTime = CookieUtil.get(this.keys.preVisitTime);

      if (preTime) {
        return time - preTime > this.clientConfig.sessionTimeout * 1000;
      }
      return true;
    },

    updatePreVisitTime: function (time) {
      CookieUtil.setExt(this.keys.preVisitTime, time);
    },

    /**
     * 打印log
     * @param msg
     */
    log: function (msg) {
      console.log(msg);
    }

  };

  //对外暴露的方法名称
  window.__AE__ = {
    startSession: function () {
      tracker.startSession();
    },
    onPageView: function () {
      tracker.onPageView();
    },

    onChargeRequest: function (orderId, name, currencyAmount, currencyType,
      paymentType) {
      tracker.onChargeRequest(orderId, name, currencyAmount, currencyType,
          paymentType);
    },

    onEventDuration: function (category, action, map, duration) {
      tracker.onEventDuration(category, action, map, duration)
    },

    setMemberId: function (mid) {
      tracker.setMemberId(mid);
    }


  };

  var autoLoad = function () {

    var _aelog_ = _aelog_ || window._aelog_ || [];
    var memberId = null;
    for (var i=0; i<_aelog_.length; i++){
      _aelog_[i][0] === "memberId" && (memberId = _aelog_[i][1]);
    }
    memberId && __AE__.setMemberId(memberId);
    __AE__.startSession();
  }

  autoLoad();

})();