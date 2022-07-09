/**
 * 微信小程序支付
 * 此处针对于微信小程序开发的支付插件
 * 第一次支付成功后会跳出订阅的消息 如果用户拒绝或同意都会跳转到支付成功页面
 * 如果点击订阅 会将状态写进缓存 之后不再提醒。
 * 
 * @param {sn,price}
 */

import { initiatePay } from "@/api/trade";
import { getWeChatMpMessage } from "@/api/message.js";
class LiLiWXPay {
  constructor(...payList) {
    this.data = payList[0];
    console.log(payList);
    // 调用支付
    this.pay = () => {
      uni.showLoading({
        title: "加载中",
      });

      let submitData = {
        sn: this.data.sn,
        orderType: this.data.orderType || "TRADE",
        clientType: "WECHAT_MP",
      };
      const paymentMethod = "WECHAT";
      const paymentClient = "MP";
      // 调用支付
      initiatePay(paymentMethod, paymentClient, submitData).then((res) => {
        let response = res.data.result;
        uni.hideLoading();
        uni.requestPayment({
          provider: "wxpay",
          appid: response.appid,
          timeStamp: response.timeStamp,
          nonceStr: response.nonceStr,
          package: response.package,
          signType: response.signType,
          paySign: response.paySign,
          success: (e) => {
            uni.showToast({
              icon: "none",
              title: "支付成功!",
            });
            sendMessage(payList[0].price);
          },
          fail: (e) => {
            this.exception = e;
            // 支付异常或支付失败之后跳转到订单页面
            uni.showModal({
              content: "支付失败,如果您已支付，请勿反复支付",
              showCancel: false,
              success: () => {
                uni.redirectTo({
                  url: "/pages/order/myOrder?status=0",
                });
              },
            });
          },
        });
      });
    };
  }
}

function sendMessage(price) {
  //判断用户是否已经进行了订阅
  if (!uni.getStorageSync("acceptSubscribeMessage")) {
    //订阅消息
    getWeChatMpMessage().then((res) => {
      var message = res.data.result;
      var templateid = message.map((item) => item.code);
      uni.requestSubscribeMessage({
        tmplIds: templateid,
        success: (res) => {
          for (let key in res) {
            // 表示用户拒绝订阅该信息
            if (res[key] == "reject") {
              this.checked = false;
            } else {
              uni.setStorageSync("acceptSubscribeMessage", res);
            }
          }
        },
        fail: (res) => {
          uni.removeStorageSync("acceptSubscribeMessage");
          this.checked = false;
        },
        complete: () => {
          /**
           * 已经支付成功
           */
          uni.redirectTo({
            url:
              "/pages/cart/payment/success?paymentMethod=WECHAT" +
              "&payPrice=" +
              price,
          });
        },
      });
    });
  } else {
    /**
     * 已经支付成功
     */
    uni.redirectTo({
      url:
        "/pages/cart/payment/success?paymentMethod=WECHAT" +
        "&payPrice=" +
        price,
    });
  }
}

export default LiLiWXPay;
