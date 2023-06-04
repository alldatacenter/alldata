<template>
  <div>
    <common-layout>
      <div class="top">
        <div class="header">
          <!-- <img src="@/assets/img/brand.png" alt /> -->
          <!-- <span class="logo-name">DataSophon</span> -->
        </div>
      </div>
      <div class="login">
        <div class="login-title">账号登录</div>
        <div class="project-name">DataSophon</div>
        <a-form @submit="onSubmit" :form="form">
          <a-alert type="error" :closable="true" v-show="error" :message="error" showIcon style="margin-bottom: 24px;" />
          <a-form-item>
            <a-input class="login-input" size="large" placeholder="输入用户名" v-decorator="['name', {rules: [{ required: true, message: '请输入用户名', whitespace: true}]}]"></a-input>
          </a-form-item>
          <a-form-item>
            <a-input class="login-input" size="large" placeholder="输入密码" type="password" v-decorator="['password', {rules: [{ required: true, message: '请输入密码', whitespace: true}]}]"></a-input>
          </a-form-item>
          <a-form-item>
            <a-button :loading="logging" style="width: 400px;height: 50px;border-radius: 4px;margin-top: 47px;font-size: 18px" size="large" htmlType="submit" type="primary">登录</a-button>
          </a-form-item>
        </a-form>
      </div>
      <page-footer style="position: absolute;bottom:0;left:0;right:0" :link-list="footerLinks" :copyright="copyright"></page-footer>
    </common-layout>
  </div>
</template>

<script>
import PageFooter from "@/layouts/footer/PageFooter";

import CommonLayout from "@/layouts/CommonLayout";
import { login, getRoutesConfig } from "@/services/user";
import { setAuthorization } from "@/utils/request";
import { loadRoutes } from "@/utils/routerUtil";
import { mapMutations } from "vuex";
import { mapState } from "vuex";

export default {
  name: "Login",
  components: { CommonLayout, PageFooter },
  data() {
    return {
      logging: false,
      error: "",
      form: this.$form.createForm(this),
    };
  },
  computed: {
    systemName() {
      return this.$store.state.setting.systemName;
    },
    ...mapState("setting", ["footerLinks", "copyright"]),
  },
  methods: {
    ...mapMutations("account", ["setUser", "setPermissions", "setRoles"]),
    onSubmit(e) {
      e.preventDefault();
      this.form.validateFields((err) => {
        if (!err) {
          this.logging = true;
          const username = this.form.getFieldValue("name");
          const password = this.form.getFieldValue("password");
          this.$axiosPost(global.API.login, { username, password }).then(
            (res) => this.afterLogin(res)
          );
        }
      });
    },
    afterLogin(res) {
      this.logging = false;
      const loginRes = res.data;
      if (res.code === 200) {
        setAuthorization({ sessionId: loginRes.sessionId });
        this.setUser(res.userInfo);
        loadRoutes()
        this.$store.commit('setting/setIsCluster', '')
        this.$router.push("/colony-manage/colony-list");
        this.$message.success("登录成功", 3);
      }
    },
  },
};
</script>

<style lang="less" scoped>
.common-layout {
  .top {
    text-align: center;
    .header {
      padding: 0 0 0 40px;
      height: 44px;
      line-height: 44px;
      display: flex;
      justify-items: center;
      align-items: center;
      a {
        text-decoration: none;
      }
      .logo {
        height: 44px;
        vertical-align: top;
        margin-right: 16px;
      }
      .logo-name {
        font-size: 24px;
        color: #333333;
        letter-spacing: 0;
        text-align: center;
        font-weight: 600;
        padding-left: 10px;
      }
      .title {
        font-size: 33px;
        color: @title-color;
        // font-family: "Myriad Pro", "Helvetica Neue", Arial, Helvetica,
          // sans-serif;
        font-weight: 600;
        position: relative;
        top: 2px;
      }
    }
    .desc {
      font-size: 14px;
      color: @text-color-second;
      margin-top: 12px;
      margin-bottom: 40px;
    }
  }
  .login {
    text-align: center;
    position: absolute;
    right: 200px;
    width: 490px;
    height: 600px;
    // height: 644px;
    background: #ffffff;
    margin: 70px 0 0 0;
    box-shadow: 0px 2px 10px 0px rgba(0, 0, 0, 0.1);
    border-radius: 4px;
    .login-title {
      margin: 54px 0 0 40px;
      width: 76px;
      height: 50px;
      font-size: 18px;
      color: #333333;
      letter-spacing: 0;
      line-height: 50px;
      font-weight: 600;
      border-bottom: 2px solid #bcc0c8;
    }
    .project-name {
      font-size: 26px;
      color: #333330;
      letter-spacing: 0;
      text-align: center;
      font-weight: 600;
      margin: 55px 0;
    }
    .login-input {
      width: 400px;
      height: 50px;
      border: 1px solid rgba(196, 204, 219, 1);
      border-radius: 4px;
    }
  }
}
</style>
