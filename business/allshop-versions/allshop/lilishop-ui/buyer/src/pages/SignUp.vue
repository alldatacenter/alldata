<template>
  <div class="sign-up" @click='$refs.verify.show = false'>
    <div style="height:50px;"></div>
    <div class="logo-box">
      <img
        width="150"
        :src="$store.state.logoImg"
        @click="$router.push('/')"
      />
      <div>注册</div>
    </div>
    <div class="login-container">
        <!-- 注册 -->
        <Form
          ref="formRegist"
          :model="formRegist"
          :rules="ruleInline"
          style="width:300px;"
        >
          <FormItem prop="username">
            <i-input
              type="text"
              v-model="formRegist.username"
              clearable
              placeholder="用户名"
            >
              <Icon type="md-person" slot="prepend"></Icon>
            </i-input>
          </FormItem>
          <FormItem prop="password">
            <i-input
              type="password"
              v-model="formRegist.password"
              clearable
              placeholder="请输入大于6位密码"
            >
              <Icon type="md-lock" slot="prepend"> </Icon>
            </i-input>
          </FormItem>
          <FormItem prop="mobilePhone">
            <i-input
              type="text"
              v-model="formRegist.mobilePhone"
              clearable
              placeholder="手机号"
            >
              <Icon type="md-phone-portrait" slot="prepend"></Icon>
            </i-input>
          </FormItem>
          <FormItem prop="code">
            <i-input
              type="text"
              v-model="formRegist.code"
              clearable
              placeholder="手机验证码"
            >
              <Icon
                type="ios-text-outline"
                style="font-weight: bold"
                slot="prepend"
              />
              <Button slot="append" @click="sendCode">{{ codeMsg }}</Button>
            </i-input>
          </FormItem>
          <FormItem>
            <Button @click="verifyBtnClick" long :type="verifyStatus?'success':'default'">{{verifyStatus?'验证通过':'点击完成安全验证'}}</Button>
          </FormItem>
          <FormItem>
            <Button type="error" size="large" @click="handleRegist" long
              >注册</Button
            >
          </FormItem>
          <FormItem><span class="color999 ml_20">点击注册，表示您同意《<router-link to="/article?id=1371992704333905920" target="_blank">商城用户协议</router-link>》</span></FormItem>
        </Form>
        <!-- 拼图验证码 -->
        <Verify
          ref="verify"
          class="verify-con"
          :verifyType="verifyType"
          @change="verifyChange"
        ></Verify>
        <div class="login-btn">已有账号？<a @click="$router.push('login')">立即登录</a></div>
    </div>
    <div class="foot">
      <Row type="flex" justify="space-around" class="help">
        <router-link to="/article" class="item" target="_blank">帮助</router-link>
        <router-link to="/article?id=1371779927900160000" class="item" target="_blank">隐私</router-link>
        <router-link to="/article?id=1371992704333905920" class="item" target="_blank">条款</router-link>
      </Row>
      <Row type="flex" justify="center" class="copyright">
        Copyright © {{year}} - Present
        <a href="https://pickmall.cn" target="_blank" style="margin: 0 5px">{{config.title}}</a>
        版权所有
      </Row>
    </div>
  </div>
</template>

<script>

import * as RegExp from '@/plugins/RegExp.js';
import { md5 } from '@/plugins/md5.js';
import * as apiLogin from '@/api/login.js';
import { sendSms } from '@/api/common.js';
import Verify from '@/components/verify';
export default {
  name: 'SignUp',
  components: { Verify },
  data () {
    return {
      config:require('@/config'),
      year: new Date().getFullYear(),
      formRegist: {
        // 注册表单
        mobilePhone: '',
        code: '',
        username: '',
        password: ''
      },
      ruleInline: {
        // 验证规则
        username: [{ required: true, message: '请输入用户名' }],
        password: [
          { required: true, message: '请输入密码' },
          { type: 'string', min: 6, message: '密码不能少于6位' }
        ],
        mobilePhone: [
          { required: true, message: '请输入手机号码' },
          {
            pattern: RegExp.mobile,
            trigger: 'blur',
            message: '请输入正确的手机号'
          }
        ],
        code: [{ required: true, message: '请输入手机验证码' }]
      },
      verifyStatus: false, // 是否验证通过
      verifyType: 'REGISTER', // 验证状态
      codeMsg: '发送验证码', // 提示文字
      interval: '', // 定时器
      time: 60 // 倒计时
    };
  },
  methods: {
    // 注册
    handleRegist () {
      this.$refs.formRegist.validate((valid) => {
        if (valid) {
          let data = JSON.parse(JSON.stringify(this.formRegist));
          data.password = md5(data.password);
          apiLogin.regist(data).then((res) => {
            if (res.success) {
              this.$Message.success('注册成功!');
              this.$router.push('login');
            } else {
              this.$Message.warning(res.message);
            }
          });
        } else {}
      });
    },
    // 发送短信验证码
    sendCode () {
      if (this.time === 60) {
        if (this.formRegist.mobilePhone === '') {
          this.$Message.warning('请先填写手机号');
          return;
        }
        if (!this.verifyStatus) {
          this.$Message.warning('请先完成安全验证');
          return;
        }
        let params = {
          mobile: this.formRegist.mobilePhone,
          verificationEnums: 'REGISTER'
        };
        sendSms(params).then(res => {
          if (res.success) {
            this.$Message.success('验证码发送成功');
            let that = this;
            this.interval = setInterval(() => {
              that.time--;
              if (that.time === 0) {
                that.time = 60;
                that.codeMsg = '重新发送';
                that.verifyStatus = false;
                clearInterval(that.interval);
              } else {
                that.codeMsg = that.time;
              }
            }, 1000);
          } else {
            this.$Message.warning(res.message);
          }
        });
      }
    },
    // 图片验证码成功回调
    verifyChange (con) {
      if (!con.status) return;
      this.$refs.verify.show = false;
      this.verifyStatus = true;
    },
    // 打开图片验证码
    verifyBtnClick () {
      if (!this.verifyStatus) {
        this.$refs.verify.init();
      }
    }
  },
  mounted () {
    this.$refs.formRegist.resetFields();
    document.querySelector('.sign-up').style.height = window.innerHeight + 'px'
  }
};
</script>
<style scoped lang="scss">
.logo-box {
  width: 600px;
  height: 80px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  img {
    width: 150px;
    cursor: pointer;
  }
  div {
    font-size: 20px;
    margin-top: 10px;
  }
}

.login-container {
  border-top: 2px solid $theme_color;
  position: relative;
  margin: 0 auto;
  width: 600px;
  background-color: #fff;
  padding: 20px 150px;
  .login-btn{
    position: absolute;
    right: 20px;
    top: -45px;
  }
}

.verify-con{
  position: absolute;
  left: 140px;
  top: 80px;
  z-index: 10;
}

.other-login {
  margin: 0 auto;
  .ivu-icon {
    font-size: 24px;
  }
}
.regist {
  display: flex;
  justify-content: flex-end;
  margin-top: -10px;
  span {
    margin-left: 10px;
    &:hover {
      cursor: pointer;
      color: $theme_color;
    }
  }
}
.foot {
  position: fixed;
  bottom: 4vh;
  width: 368px;
  left: calc(50% - 184px);
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  .help {
    margin: 0 auto;
    margin-bottom: 1vh;
    width: 60%;
    .item {
      color: rgba(0, 0, 0, 0.45);
    }
    :hover {
      color: rgba(0, 0, 0, 0.65);
    }
  }
}
</style>
