<template>
  <div class="wrapper">
    <BaseHeader></BaseHeader>
    <div class="wrapper-head">
      <div class="head-left">
        <div class="left-tips">订单提交成功，请尽快付款！</div>
        <div class="left-tips-time">请您尽快完成支付，否则订单会被自动取消</div>
      </div>
      <div class="head-right">
        <div>应付金额 <span class="price">{{$route.query.price | unitPrice}}</span>元</div>
      </div>
    </div>
    <div class="content">
      <div class="pay-way">{{params.paymentMethod === 'ALIPAY' ? '支付宝支付' : '微信支付'}}</div>
      <div class="qrcode">
        <div style="width:200px;height:200px;border:1px solid #eee;">
          <vue-qr :text="qrcode" :margin="0" colorDark="#000" colorLight="#fff" :size="200"></vue-qr>
        </div>
        <div class="intro">
          <Icon type="md-qr-scanner" /> 请使用{{params.paymentMethod === 'ALIPAY' ? '支付宝' : '微信'}}扫码付款
        </div>
      </div>
      <div class="btn-div">
        <p class="mb_10">支付成功后自动跳转，如未跳转请点击按钮手动跳转。。。</p>
        <div>
          <Button @click="handlePay">重新支付</Button>
          <Button type="success" @click="$router.push('/payDone')">支付成功</Button>
        </div>
      </div>
      <a @click="$router.back()">选择其他支付方式></a>
    </div>
    <BaseFooter></BaseFooter>
  </div>
</template>
<script>
import vueQr from 'vue-qr';
import { payCallback, pay } from '@/api/pay.js';
export default {
  components: { vueQr },
  data () {
    return {
      qrcode: '', // 二维码
      params: this.$route.query, // 参数
      interval: null, // 定时器
      num: 0 // 商品数
    };
  },
  methods: {
    // 获取支付二维码
    handlePay () {
      const params = this.$route.query;
      pay(params).then(res => {
        if (res.success) {
          this.qrcode = res.result;
          this.num = 0;
          this.interval = setInterval(this.callback, 5000);
        } else {
          this.$Message.error(res.message)
        }
      });
    },
    callback () { // 支付回调接口
      this.num++;
      if (this.num >= 7) {
        clearInterval(this.interval);
        this.interval = null;
      }
      let params = JSON.parse(JSON.stringify(this.$route.query));
      delete params.paymentMethod;
      delete params.paymentClient;
      payCallback(params).then(res => {
        if (res.result) {
          clearInterval(this.interval);
          this.interval = null;
          this.$router.push({path: '/payDone', query: {orderType: this.$route.query.orderType}});
        }
      });
    }
  },
  mounted () {
    this.handlePay();
  }
};
</script>
<style scoped lang="scss">
.head-left {
  font-weight: bold;
}
.left-tips {
  font-size: 21px;
}

.left-tips-time {
  font-size: 16px;
}
.wrapper-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  line-height: 1.75;
}
.wrapper-head,
.content {
  padding: 20px 40px;
  width: 1000px;
  margin: 20px auto;
}
.wrapper {
  width: 100%;
  height: 100%;
}
.content {
  background-color: #fff;
  box-shadow: 0 6px 10px #ddd;
  position: relative;
  display: flex;
  .pay-way {
    font-weight: bold;
    font-size: 18px;
  }

  .qrcode {
    margin: 30px 0 0 70px;
  }

  .intro {
    background-color: #ff7674;
    height: 40px;
    line-height: 40px;
    margin-top: 10px;
    color: #fff;
    font-size: 16px;
    text-align: center;
  }

  .btn-div {
    margin:120px 0 0 30px;
  }

  a {
    position: absolute;
    right: 20px;
    top: 20px;
  }
}
.price {
  font-size: 18px;
  font-weight: bold;
  color: $theme_color;
}
.head-right {
  font-weight: bold;
}
</style>
