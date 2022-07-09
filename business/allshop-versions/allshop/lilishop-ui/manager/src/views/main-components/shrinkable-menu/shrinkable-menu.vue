<template>
  <div class="ivu-shrinkable-menu">
    <!-- 一级菜单 -->
    <Menu ref="sideMenu" width="80px" theme="dark"  :active-name="currNav" @on-select="selectNav">
      <MenuItem v-for="(item, i) in navList" :key="i" :name="item.name">
        {{item.title}}
      </MenuItem>
    </Menu>
    <!-- 二级菜单 -->
    <Menu
      ref="childrenMenu"
      :active-name="$route.name"
      width="100px"
      @on-select="changeMenu"
    >
      <template v-for="item in menuList">
        <MenuGroup :title="item.title" :key="item.id" style="padding-left:0;">
          <MenuItem :name="menu.name" v-for="menu in item.children" :key="menu.name">
            {{menu.title}}
          </MenuItem>
        </MenuGroup>

      </template>
    </Menu>
  </div>
</template>

<script>
import util from "@/libs/util.js";

export default {
  name: "shrinkableMenu",
  computed: {
    menuList() {
      return this.$store.state.app.menuList;
    },
    navList() {
      return this.$store.state.app.navList;
    },
    currNav() {
      return this.$store.state.app.currNav;
    }
  },
  watch: {
    // 监听路由变化
    $route: {
      handler: function (val, oldVal) {
        if (val.meta.firstRouterName && val.meta.firstRouterName !== this.currNav) {
          this.selectNav(val.meta.firstRouterName)
        }
      }
    } 
  },
  methods: {
    changeMenu(name) { //二级路由点击
      this.$router.push({
        name: name
      });
    },
    selectNav(name) { // 一级路由点击事件
      this.$store.commit("setCurrNav", name);
      this.setStore("currNav", name);
      util.initRouter(this);
      this.$nextTick(()=>{
        this.$refs.childrenMenu.updateActiveName()
      })
    },
  }
};
</script>
<style lang="scss" scoped>
.ivu-shrinkable-menu{
    height: calc(100% - 60px);
    width: 180px;
    display: flex;
}

.ivu-btn-text:hover {
    background-color: rgba(255,255,255,.2) !important;
}
.ivu-menu-dark.ivu-menu-vertical .ivu-menu-item-active:not(.ivu-menu-submenu), .ivu-menu-dark.ivu-menu-vertical .ivu-menu-submenu-title-active:not(.ivu-menu-submenu){
    background-color: #fff;
    &:hover{
        background-color: #fff;
    }
}
.ivu-menu-vertical{
  overflow-y: auto;
}
.ivu-menu-dark.ivu-menu-vertical .ivu-menu-item-active:not(.ivu-menu-submenu), .ivu-menu-dark.ivu-menu-vertical .ivu-menu-submenu-title-active:not(.ivu-menu-submenu){
    color: $theme_color;
}
/deep/.ivu-menu-vertical .ivu-menu-item-group-title {
    height: 40px;
    line-height: 40px;
    padding-left: 20px;
}
</style>
