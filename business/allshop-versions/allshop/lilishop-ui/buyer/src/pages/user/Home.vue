<template>
  <div>
    <BaseHeader></BaseHeader>

    <div class="container width_1200">
      <Layout class="layoutAll">
        <Sider class="side-bar" ref="side" :collapsed-width="78">
          <Menu
            class="side-menu"
            theme="light"
            width="auto"
            :active-name="$route.name"
            :open-names="['订单中心', '会员中心', '账户中心']"
            @on-select="onSelect"
          >
            <div class="user-icon">
              <div class="user-img">
                <img
                  :src="userInfo.face"
                  style="width: 100%; height: 100%"
                  v-if="userInfo.face"
                  alt
                />
                <Avatar icon="ios-person" class="mb_10" v-else size="96" />
              </div>
              <p>{{ userInfo.nickName }}</p>
            </div>

            <!--   循环导航栏       -->
            <Submenu
              v-for="(menu, index) in menuList"
              :key="index"
              :name="menu.title"
            >
              <template slot="title" v-if="menu.display">
                <Icon type="location"></Icon>
                <span>{{ menu.title }}</span>
              </template>
              <MenuItem
                v-for="(chlidren, i) in menu.menus"
                :key="i"
                :name="chlidren.path"
                >{{ chlidren.title }}</MenuItem
              >
            </Submenu>
          </Menu>
        </Sider>
        <Layout class="layout ml_10">
          <Content class="content">
            <transition mode="out-in">
              <router-view></router-view>
            </transition>
          </Content>
        </Layout>
      </Layout>
    </div>
  </div>
</template>

<script>
import menuList from "./menu";
import Storage from "@/plugins/storage.js";

export default {
  name: "Home",
  data() {
    return {
      menuList, // 会员中心左侧列表
    };
  },
  computed: {
    userInfo() {
      // 用户信息
      if (Storage.getItem("userInfo")) {
        return JSON.parse(Storage.getItem("userInfo"));
      } else {
        return {};
      }
    },
  },

  methods: {
    // 每次点击左侧bar的callback
    onSelect(name) {
      this.$router.push({ name: name });
    },
  },
};
</script>

<style scoped lang="scss">
.content {
  padding: 15px 50px;
}

.header {
  @include background_color($light_background_color);
}

.side-menu,
.side-bar,
.content {
  @include white_background_color();
  @include title_color($light_title_color);
}

.side-bar {
  min-height: 600px;
  height: auto;
}

.layoutAll {
  min-height: 1200px;
  @include background_color($light_background_color);
}

.container {
  margin: 0 auto;
  padding: 20px 0;
}

.side-bar a {
  @include title_color($light_title_color);
}

.user-icon {
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.user-img {
  margin-bottom: 15px;
  width: 96px;
  height: 96px;
  border-radius: 48px;
  overflow: hidden;
}

.layout-footer-center {
  padding: 0px 15px;

  padding-bottom: 15px;
  text-align: center;
}
</style>
