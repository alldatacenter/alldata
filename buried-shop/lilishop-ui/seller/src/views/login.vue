<template>
  <div class="login" @click="$refs.verify.show = false">
    <Row type="flex" @keydown.enter.native="submitLogin">
      <Col style="width: 368px">
      <Header />
      <Row style="flex-direction: column;">
        <Form ref="usernameLoginForm" :model="form" :rules="rules" class="form">
          <FormItem prop="username">
            <Input v-model="form.username" prefix="ios-contact" size="large" clearable placeholder="请输入用户名"
              autocomplete="off" />
          </FormItem>
          <FormItem prop="password">
            <Input type="password" v-model="form.password" prefix="ios-lock" size="large" password placeholder="请输入密码"
              autocomplete="off" />
          </FormItem>
        </Form>

        <Row>
          <div class="login-btn" type="primary" size="large" :loading="loading" @click="submitLogin" long>
            <span v-if="!loading">登录</span>
            <span v-else>登录中</span>
          </div>
        </Row>

      </Row>
      <Footer />
      <!-- 拼图验证码 -->
      <verify ref="verify" class="verify-con" verifyType="LOGIN" @change="verifyChange"></verify>
      </Col>
    </Row>


  </div>
</template>

<script>
import { login, userMsg } from "@/api/index";
import Cookies from "js-cookie";
import Header from "@/views/main-components/header";
import Footer from "@/views/main-components/footer";
import util from "@/libs/util.js";
import verify from "@/views/my-components/verify";
export default {
  components: {
    Header,
    Footer,
    verify,
  },
  data() {
    return {
      saveLogin: true, // 保存登录状态
      loading: false, // 加载状态
      form: {
        // 表单数据
        username: "",
        password: "",
        mobile: "",
        code: "",
      },
      rules: {
        // 验证规则
        username: [
          {
            required: true,
            message: "账号不能为空",
            trigger: "blur",
          },
        ],
        password: [
          {
            required: true,
            message: "密码不能为空",
            trigger: "blur",
          },
        ],
      },
    };
  },
  methods: {
    afterLogin(res) {
      let accessToken = res.result.accessToken;
      this.setStore("accessToken", accessToken);
      this.setStore("refreshToken", res.result.refreshToken);

      // 获取用户信息
      userMsg().then((res) => {
        if (res.success) {
          this.setStore("saveLogin", this.saveLogin);
          if (this.saveLogin) {
            // 保存7天
            Cookies.set("userInfoSeller", JSON.stringify(res.result), {
              expires: 7,
            });
          } else {
            Cookies.set("userInfoSeller", JSON.stringify(res.result));
          }
          this.$store.commit("setAvatarPath", res.result.storeLogo);
          // 加载菜单
          util.initRouter(this);
          this.$router.push({
            name: "home_index",
          });
        } else {
          this.loading = false;
        }
      });
    },
    submitLogin() {
      // 登录提交
      this.$refs.usernameLoginForm.validate((valid) => {
        if (valid) {
          this.$refs.verify.init();
        }
      });
    },
    verifyChange(con) {
      // 拼图验证码回显
      if (!con.status) return;

      this.loading = true;

      let fd = new FormData();
      fd.append('username',this.form.username)
      fd.append('password',this.md5(this.form.password))
      login(fd)
        .then((res) => {
          this.loading = false;
          if (res && res.success) {
            this.afterLogin(res);
          }
        })
        .catch(() => {
          this.loading = false;
        });
      this.$refs.verify.show = false;
    },
  },
};
</script>
<style lang="scss" scoped>

.login {
  height: 100%;
  background: url("../assets/background.png") no-repeat;
  background-size: 100%;
  background-position-y: bottom;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;


  .verify-con {
    position: absolute;
    top: 126px;
    z-index: 10;
    left: 20px;
  }
  .form {
    padding-top: 1vh;
  }

  .login-btn {
    background: linear-gradient(135deg, $theme_color 0%, $warning_color 100%);
    height: 40px;
    cursor: pointer;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    color: #fff;
    width: 100%;
    text-align: center;
    transition: 0.35s;
  }
  .login-btn:hover {
    opacity: .9;
    border-radius: 10px;
  }
}

</style>
