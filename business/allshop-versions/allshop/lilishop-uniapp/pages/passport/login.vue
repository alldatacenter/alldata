<template>
  <div class="wrapper">
    <div v-if="!wechatLogin">
      <u-navbar :is-back="showBack" :border-bottom="false"></u-navbar>
      <div>
        <div class="title">{{ loginTitleWay[current].title }}</div>
        <div :class="current == 1 ? 'desc-light' : 'desc'">
          {{ loginTitleWay[current].desc
          }}<span v-if="current == 1">{{ mobile | secrecyMobile }}</span>
        </div>
      </div>
      <!-- 手机号 -->
      <div v-show="!enableUserPwdBox">
        <div v-show="current == 0">
          <u-input
            :custom-style="inputStyle"
            :placeholder-style="placeholderStyle"
            placeholder="请输入手机号 (11位)"
            class="mobile"
            focus
            v-model="mobile"
            type="number"
            maxlength="11"
          />
          <div
            :class="!enableFetchCode ? 'disable' : 'fetch'"
            @click="fetchCode"
            class="btn"
          >
            获取验证码
          </div>
        </div>
        <!-- 输入验证码 -->
        <div v-show="current == 1" class="box-code">
          <verifyCode
            type="bottom"
            @confirm="submit"
            boxActiveColor="#D8D8D8"
            v-model="code"
            isFocus
            boxNormalColor="#D8D8D8"
            cursorColor="#D8D8D8"
          />

          <div class="fetch-btn">
            <u-verification-code
              change-text="验证码已发送（x）"
              end-text="重新获取验证码"
              unique-key="page-login"
              :seconds="seconds"
              @end="end"
              @start="start"
              ref="uCode"
              @change="codeChange"
            ></u-verification-code>
            <span @tap="fetchCode" :style="{ color: codeColor }">
              {{ tips }}</span
            >
          </div>
        </div>
      </div>

      <!-- 帐号密码登录 -->
      <div v-show="enableUserPwdBox">
        <u-input
          :custom-style="inputStyle"
          :placeholder-style="placeholderStyle"
          placeholder="请输入用户名"
          class="mobile"
          focus
          v-model="userData.username"
        />
        <u-input
          :custom-style="inputStyle"
          :placeholder-style="placeholderStyle"
          placeholder="请输入密码"
          class="mobile"
          focus
          v-model="userData.password"
          type="password"
        />

        <div
          :class="!enableUserBtnColor ? 'disable' : 'fetch'"
          @click="passwordLogin"
          class="btn"
        >
          帐号密码登录
        </div>
      </div>

      <div class="flex" v-show="current != 1">
        <u-checkbox-group :icon-size="24" width="45rpx">
          <u-checkbox
            shape="circle"
            v-model="enablePrivacy"
            active-color="#FF5E00"
          ></u-checkbox>
        </u-checkbox-group>
        <div class="tips">
          未注册的手机号验证后将自动创建用户账号，登录即代表您已同意<span
            @click="navigateToPrivacy('privacy')"
            >《隐私协议》</span>
             <span @click="navigateToPrivacys('user')">
            《用户协议》
            </span>
        </div>
      </div>

      <div
        v-if="current != 1"
        class="user-password-tips"
        @click="enableUserPwdBox = !enableUserPwdBox"
      >
        {{ !enableUserPwdBox ? "帐号密码" : "手机号" }}登录
      </div>

      <!-- 循环出当前可使用的第三方登录模式 -->
      <div class="flex login-list">
        <div
          v-if="item.code"
          :style="{ background: item.color }"
          class="login-item"
          v-for="(item, index) in loginList"
          :key="index"
        >
          <u-icon
            v-if="item.title != 'APPLE'"
            color="#fff"
            size="42"
            :name="item.icon"
            @click="navigateLogin(item)"
          >
          </u-icon>
          <u-image
            v-else
            src="/static/appleidButton@2x.png"
            :lazy-load="false"
            @click="navigateLogin(item)"
            width="80"
            height="80"
          />
        </div>
      </div>
      <myVerification
        v-if="codeFlag"
        @send="verification"
        class="verification"
        ref="verification"
        business="LOGIN"
      />
    </div>
    <view v-else>
      <wechatH5Login />
    </view>
  </div>
</template>

<script>
import { openIdLogin, loginCallback } from "@/api/connect.js";
import api from "@/config/api.js";
import { sendMobile, smsLogin, userLogin } from "@/api/login";
import myVerification from "@/components/verification/verification.vue"; //验证码模块
import uuid from "@/utils/uuid.modified.js"; // uuid
import verifyCode from "@/components/verify-code/verify-code";
import { getUserInfo } from "@/api/members";
import { whetherNavigate } from "@/utils/Foundation"; //登录跳转
import storage from "@/utils/storage.js"; //缓存
import wechatH5Login from "./wechatH5Login.vue";
import { webConnect } from "@/api/connect.js";
import { md5 } from "@/utils/md5.js";

export default {
  components: { myVerification, verifyCode, wechatH5Login },

  data() {
    return {
      uuid,
      wechatLogin: false, //是否加载微信公众号登录
      flage: false, //是否验证码验证
      codeFlag: true, //验证开关，用于是否展示验证码
      tips: "",
      enableUserPwdBox: false, //帐号密码登录
      current: 0,
      codeColor: "#999", //按钮验证码颜色
      lightColor: this.$lightColor,
      seconds: 60, //默认验证码等待时间
      loginTitleWay: [
        {
          title: "欢迎登录",
          desc: "登录后更精彩，美好生活即将开始",
        },
        {
          title: "请输入验证码",
          desc: "已经发送验证码至",
        },
      ],
      userData: {
        username: "",
        password: "",
      },
      showBack: false,
      enableFetchCode: false,
      enableUserBtnColor:false,
      enablePrivacy: false, //隐私政策
      mobile: "", //手机号
      code: "", //验证码
      inputStyle: {
        height: "104rpx",
        "border-bottom": "1rpx solid #D8D8D8",
        "letter-spacing": "1rpx",
        "font-size": "40rpx",
        "line-height": "40rpx",
        color: "#333",
      },
      placeholderStyle: "font-size: 32rpx;line-height: 32rpx;color: #999999;",
      loginList: [
        //第三方登录集合
        {
          icon: "weixin-fill",
          color: "#00a327",
          title: "微信",
          code: "WECHAT",
        },
        {
          icon: "qq-fill",
          color: "#38ace9",
          title: "QQ",
          code: "QQ",
        },
        {
          icon: "apple-fill",
          color: "#000000",
          title: "Apple",
          code: "APPLE",
        },
      ],
    };
  },
  onShow() {
		
		// 只要是app登录的全部清除内容
		// #ifdef APP-PLUS 
		storage.setAccessToken("");
		storage.setRefreshToken("");
		storage.setUserInfo({});
		// #endif
		
		
    //#ifdef H5
    let isWXBrowser = /micromessenger/i.test(navigator.userAgent);
    if (isWXBrowser) {
      webConnect("WECHAT").then((res) => {
        let data = res.data;
        if (data.success) {
          window.location = data.result;
        }
      });
    }
    //#endif
  },

  mounted() {
    // #ifndef APP-PLUS
    //判断是否微信浏览器
    var ua = window.navigator.userAgent.toLowerCase();
    if (ua.match(/MicroMessenger/i) == "micromessenger") {
      this.wechatLogin = true;
      return;
    }
    // #endif
    /**
     * 条件编译判断当前客户端类型
     */
    //#ifdef H5
    this.clientType = "H5";
    //#endif

    //#ifdef APP-PLUS
    this.clientType = "APP";
    /**如果是app 加载支持的登录方式*/
    let _this = this;
    uni.getProvider({
      service: "oauth",
      success: (result) => {
        _this.loginList = result.provider.map((value) => {
          //展示title
          let title = "";
          //系统code
          let code = "";
          //颜色
          let color = "#8b8b8b";
          //图标
          let icon = "";
          //uni 联合登录 code
          let appcode = "";
          switch (value) {
            case "weixin":
              icon = "weixin-circle-fill";
              color = "#00a327";
              title = "微信";
              code = "WECHAT";
              break;
            case "qq":
              icon = "qq-circle-fill";
              color = "#38ace9";
              title = "QQ";
              code = "QQ";
              break;
            case "apple":
              icon = "apple-fill";
              color = "#000000";
              title = "Apple";
              code = "APPLE";
              break;
          }
          return {
            title: title,
            code: code,
            color: color,
            icon: icon,
            appcode: value,
          };
        });
      },
      fail: (error) => {
        uni.showToast({
          title: "获取登录通道失败" + error,
          duration: 2000,
          icon: "none",
        });
      },
    });
    //#endif

    //特殊平台，登录方式需要过滤
    // #ifdef H5
    this.methodFilter(["QQ"]);
    // #endif

    //微信小程序，只支持微信登录
    // #ifdef MP-WEIXIN
    this.methodFilter(["WECHAT"]);
    // #endif
  },
  watch: {
    current(val) {
      val ? (this.showBack = true) : (this.showBack = false);
    },
    userData:{
      handler(val){
        if(this.userData.username && this.userData.password) {
          this.enableUserBtnColor = true;
        }else{
            this.enableUserBtnColor = false;
        }
      },
      deep:true,

    },
    mobile: {
      handler(val) {
        if (val.length == 11) {
          this.enableFetchCode = true;
        }
      },
    },

    async flage(val) {
      if (val) {
        if (this.$refs.uCode.canGetCode) {
          if (this.enableUserPwdBox) {
            this.submitUserLogin();
            return;
            // 执行登录
          } else {
            // 向后端请求验证码
            uni.showLoading({});
            let res = await sendMobile(this.mobile);
            uni.hideLoading();
            // 这里此提示会被this.start()方法中的提示覆盖
            if (res.data.success) {
              this.current = 1;
              this.$refs.uCode.start();
            } else {
              uni.showToast({
                title: res.data.message,
                duration: 2000,
                icon: "none",
              });
              this.flage = false;
              this.$refs.verification.getCode();
            }
          }
        } else {
          !this.enableUserPwdBox ? this.$u.toast("请倒计时结束后再发送") : "";
        }
      } else {
        this.$refs.verification.hide();
      }
    },
  },
  onLoad(options) {
    if (options && options.state) {
      this.stateLogin(options.state);
    }
  },
  methods: {
    //联合信息返回登录
    stateLogin(state) {
      loginCallback(state).then((res) => {
        let data = res.data;
        if (data.success) {
          storage.setAccessToken(data.result.accessToken);
          storage.setRefreshToken(data.result.refreshToken);
          // 登录成功
          uni.showToast({
            title: "登录成功!",
            icon: "none",
          });
          getUserInfo().then((user) => {
            storage.setUserInfo(user.data.result);
            storage.setHasLogin(true);
          });
          getCurrentPages().length > 1
            ? uni.navigateBack({
                delta: getCurrentPages().length - 2,
              })
            : uni.switchTab({
                url: "/pages/tabbar/home/index",
              });
        }
      });
    },
    /** 根据参数显示登录模块 */
    methodFilter(code) {
      let way = [];
      this.loginList.forEach((item) => {
        if (code.length != 0) {
          code.forEach((val) => {
            if (item.code == val) {
              way.push(item);
            }
          });
        } else {
          uni.showToast({
            title: "配置有误请联系管理员",
            duration: 2000,
            icon: "none",
          });
        }
      });
      this.loginList = way;
    },
    //非h5 获取openid
    async nonH5OpenId(item) {
      let _this = this;
      //获取各个openid
      await uni.login({
        provider: item.appcode,
        // #ifdef MP-ALIPAY
        scopes: "auth_user", //支付宝小程序需设置授权类型
        // #endif
        success: function (res) {
          uni.setStorageSync("type", item.code);
          //微信小程序意外的其它方式直接在storage中写入openid
          // #ifndef MP-WEIXIN
          uni.setStorageSync("openid", res.authResult.openid);
          // #endif
        },
        fail(e) {
          uni.showToast({
            title: "第三方登录暂不可用！",
            icon: "none",
            duration: 3000,
          });
        },
        complete(e) {
          //获取用户信息
          uni.getUserInfo({
            provider: item.appcode,
            success: function (infoRes) {
              //写入用户信息
              uni.setStorageSync("nickname", infoRes.userInfo.nickName);
              uni.setStorageSync("avatar", infoRes.userInfo.avatarUrl);

              // #ifdef MP-WEIXIN
              //微信小程序获取openid 需要特殊处理 如需获取openid请参考uni-id: https://uniapp.dcloud.net.cn/uniCloud/uni-id
              _this.weixinMPOpenID(res).then((res) => {
                //这里需要先行获得openid，再使用openid登录，小程序登录需要两步，所以这里特殊编译
                _this.goOpenidLogin("WECHAT_MP");
              });
              // #endif

              // #ifndef MP-WEIXIN
              _this.goOpenidLogin("APP");
              //#endif
            },
          });
        },
      });
    },
    //openid 登录
    goOpenidLogin(clientType) {
      // 获取准备好的参数，进行登录系统
      let params = {
        uuid: uni.getStorageSync("openid"), //联合登陆id
        source: uni.getStorageSync("type"), //联合登陆类型
        nickname: uni.getStorageSync("nickname"), // 昵称
        avatar: uni.getStorageSync("avatar"), // 头像
        uniAccessToken: uni.getStorageSync("uni_access_token"), //第三方token
      };
      openIdLogin(params, clientType).then((res) => {
        if (!res.data.success) {
          let errormessage = "第三方登录暂不可用";
          uni.showToast({
            // title: '未绑定第三方账号',
            title: errormessage,
            icon: "none",
            duration: 3000,
          });
          return;
        } else {
          storage.setAccessToken(res.data.result.accessToken);
          storage.setRefreshToken(res.data.result.refreshToken);
          // 登录成功
          uni.showToast({
            title: "第三方登录成功!",
            icon: "none",
          });
          // 执行登录
          getUserInfo().then((user) => {
            if (user.data.success) {
              /**
               * 个人信息存储到缓存userInfo中
               */
              storage.setUserInfo(user.data.result);
              storage.setHasLogin(true);

              /**
               * 计算出当前router路径
               * 1.如果跳转的链接为登录页面或跳转的链接为空页面。则会重新跳转到首页
               * 2.都不满足返回跳转页面
               */
             if (user.data.result.mobile) {
                whetherNavigate();
              } else {
                uni.navigateTo({
                  url: "/pages/passport/bindUserPhone",
                });
              }
            } else {
              uni.switchTab({
                url: "/pages/tabbar/home/index",
              });
            }
          });
        }
      });
    },
    //微信小程序获取openid
    async weixinMPOpenID(res) {
      await miniProgramLogin(res.code).then((res) => {
        uni.setStorageSync("openid", res.data);
      });
    },
    /**跳转到登录页面 */
    navigateLogin(connectLogin) {
      // #ifdef H5
      let code = connectLogin.code;
      let buyer = api.buyer;
      window.open(
        buyer + `/passport/connect/connect/login/web/` + code,
        "_self"
      );
      // #endif
      // #ifdef APP-PLUS
      this.nonH5OpenId(connectLogin);
      // #endif
    },

    // 提交
    submit() {
      /**
       * 清空当前账号信息
       */
      storage.setHasLogin(false);
      storage.setAccessToken("");
      storage.setRefreshToken("");
      storage.setUserInfo({});
      /**
       * 执行登录
       */
      smsLogin({ mobile: this.mobile, code: this.code }, this.clientType).then(
        (res) => {
          this.getUserInfoMethods(res);
        }
      );
    },

    // 登录成功之后获取用户信息
    getUserInfoMethods(res) {
      console.log(res);
      if (res.data.success) {
        storage.setAccessToken(res.data.result.accessToken);
        storage.setRefreshToken(res.data.result.refreshToken);

        /**
         * 登录成功后获取个人信息
         */
        getUserInfo().then((user) => {
          if (user.data.success) {
            /**
             * 个人信息存储到缓存userInfo中
             */
            storage.setUserInfo(user.data.result);
            storage.setHasLogin(true);
            // 登录成功
            uni.showToast({
              title: "登录成功!",
              icon: "none",
            });

           
                whetherNavigate();
              
          } else {
            uni.switchTab({
              url: "/pages/tabbar/home/index",
            });
          }
        });
      }
    },

    // 验证码验证
    verification(val) {
      this.flage = val == this.$store.state.verificationKey ? true : false;
    },
    // 跳转
    navigateToPrivacy(val) {
      uni.navigateTo({
        url: "/pages/mine/help/tips?type=" + val,
      });
      console.log(val)
    },
    navigateToPrivacys(val){
      uni.navigateTo({
        url:"/pages/mine/help/tips?type="+ val,
      })
    },
    // 点击获取验证码
    start() {
      this.codeColor = "#999";
      this.$u.toast("验证码已发送");
      this.flage = false;
      this.codeFlag = false;

      this.$refs.verification.hide();
    },
    /**点击验证码*/
    codeChange(text) {
      this.tips = text;
    },
    /** 结束验证码后执行 */
    end() {
      this.codeColor = this.lightColor;
      this.codeFlag = true;
      console.log(this.codeColor);
    },

    passwordLogin() {
      if (!this.enablePrivacy) {
        uni.showToast({
          title: "请同意用户隐私",
          duration: 2000,
          icon: "none",
        });
        return false;
      }

      if (!this.userData.username) {
        uni.showToast({
          title: "请填写用户名",
          duration: 2000,
          icon: "none",
        });
        return false;
      }

      if (!this.userData.password) {
        uni.showToast({
          title: "请填写密码",
          duration: 2000,
          icon: "none",
        });
        return false;
      }

      if (!this.flage) {
        this.$refs.verification.error(); //发送

        return false;
      }
    },

    // 提交用户登录
    async submitUserLogin() {
      const params = JSON.parse(JSON.stringify(this.userData));
      params.password = md5(params.password);
      try {
        let res = await userLogin(params);
        if (res.data.success) {
          console.log("zhixing ")
          this.getUserInfoMethods(res);
        } else {
          this.$refs.verification.getCode();
          this.flage = false;
        }
      } catch (error) {
        this.$refs.verification.getCode();
      }
    },

    // 发送验证码
    fetchCode() {
      if (!this.enablePrivacy) {
        uni.showToast({
          title: "请同意用户隐私",
          duration: 2000,
          icon: "none",
        });
        return false;
      }

      if (!this.$u.test.mobile(this.mobile)) {
        uni.showToast({
          title: "请填写正确手机号",
          duration: 2000,
          icon: "none",
        });
        return false;
      }
      if (this.tips == "重新获取验证码") {
        uni.showLoading({
          title: "加载中",
        });
        if (!this.codeFlag) {
          let timer = setInterval(() => {
            if (this.$refs.verification) {
              this.$refs.verification.error(); //发送
            }
            clearInterval(timer);
          }, 100);
        }
        uni.hideLoading();
      }
      if (!this.flage) {
        this.$refs.verification.error(); //发送

        return false;
      }
    },
  },
};
</script>
<style>
page {
  background: #fff;
}
</style>
<style lang="scss" scoped>
.wrapper {
  padding: 0 80rpx;
}
.title {
  padding-top: calc(104rpx);
  font-style: normal;
  line-height: 1;
  font-weight: 500;
  font-size: 56rpx;
  color: #333;
}
.box-code {
  margin-top: 78rpx;
}
.desc,
.desc-light {
  font-size: 32rpx;
  line-height: 32rpx;
  color: #333333;
  margin-top: 40rpx;
}
.desc {
  color: #333;
}
.desc-light {
  color: #999999;
  > span {
    color: #333;
    margin-left: 8rpx;
  }
}
.mobile {
  margin-top: 80rpx;
}
.disable {
  background: linear-gradient(90deg, #ffdcba 2.21%, #ffcfb2 99.86%);
}
.fetch {
  background: linear-gradient(57.72deg, #ff8a19 18.14%, #ff5e00 98.44%);
}
.btn {
  border-radius: 100px;
  width: 590rpx;
  margin-top: 97rpx;
  height: 80rpx;
  font-size: 30rpx;
  line-height: 80rpx;
  text-align: center;
  color: #ffffff;
}
.tips {
  font-size: 12px;
  line-height: 20px;
  margin-top: 32rpx;
  width: 546rpx;
  > span {
    color: $light-color;
  }
}
.fetch-btn {
  width: 370rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: #f2f2f2;
  border-radius: 100rpx;
  font-size: 28rpx;
  color: #999;

  margin: 71rpx auto 0 auto;
}

.login-list {
  display: flex;
  width: 590rpx;
  position: absolute;
  bottom: 20px;
  align-items: center;
  justify-content: center;
}

.login-item {
  width: 80rpx;
  border-radius: 10rpx;
  height: 80rpx;
  display: flex;
  justify-content: center;
  align-items: center;

  margin: 0 20rpx;
}

.user-password-tips {
  text-align: center;
  color: $main-color;
  margin: 20px 0;
}
</style>
