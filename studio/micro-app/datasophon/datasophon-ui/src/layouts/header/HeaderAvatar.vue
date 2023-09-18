<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-07-11 15:33:32
 * @FilePath: \ddh-ui\src\layouts\header\HeaderAvatar.vue
-->
<template>
  <a-dropdown>
    <div class="header-avatar" style="cursor: pointer">
      <!-- <a-avatar class="avatar" size="small" shape="circle" src="../../assets/img/logo3.png"/> -->
      <img width="32" class="mgr6" src="@/assets/img/avatar.svg" />
      <span class="name">{{ user.username }}</span>
    </div>
    <a-menu :class="['avatar-menu']" slot="overlay">
      <a-menu-item @click="viewUserInfo">
        <a-icon type="user" />
        <span>个人中心</span>
      </a-menu-item>
      <a-menu-item v-if="isCluster === 'isCluster'" @click="toCluster">
        <svg-icon icon-class="colony"></svg-icon>
        <span style="margin-left: 8px">集群管理</span>
      </a-menu-item>
      <a-menu-divider />
      <a-menu-item @click="logout">
        <a-icon style="margin-right: 8px" type="poweroff" />
        <span>退出登录</span>
      </a-menu-item>
    </a-menu>
  </a-dropdown>
</template>

<script>
import { mapGetters, mapState, mapMutations } from "vuex";
import { logout } from "@/services/user";
import UserInfo from "./UserInfo.vue";
export default {
  name: "HeaderAvatar",
  computed: {
    ...mapGetters("account", ["user"]),
    ...mapGetters("setting", ["isCluster"]),
    // isCluster () {
    //   const isCluster = localStorage.getItem('isCluster')
    //   return isCluster
    // }
  },
  methods: {
    ...mapMutations("setting", ["setIsCluster", "setMenuData"]),
    viewUserInfo() {
      let width = 400;
      let title = "个人中心";
      let content = <UserInfo />;
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    logout() {
      this.$axiosPost(global.API.loginOut, {}).then((res) => {
        logout();
        localStorage.removeItem("isCluster");
        this.setIsCluster("");
        this.$router.push("/login");
      });
    },
    toCluster() {
      localStorage.removeItem("isCluster");
      this.setIsCluster("");
      this.$router.push("/colony-manage/colony-list");
      // localStorage.removeItem('menuData')
    },
  },
};
</script>

<style lang="less">
.header-avatar {
  display: inline-flex;
  .avatar,
  .name {
    align-self: center;
    color: #fff;
  }
  .avatar {
    margin-right: 8px;
  }
  .name {
    font-weight: 500;
  }
}
.avatar-menu {
  width: 150px;
}
</style>
