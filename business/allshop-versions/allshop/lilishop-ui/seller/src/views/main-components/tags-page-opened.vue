<style lang="scss" scoped>
@import "../main.scss";
</style>

<template>

  <div
    ref="scrollCon"
    @DOMMouseScroll="handlescroll"
    @mousewheel="handlescroll"
    class="tags-outer-scroll-con"
  >
    <ul v-show="visible" :style="{left: contextMenuLeft + 'px', top: contextMenuTop + 'px'}" class="contextmenu">
      <li v-for="(item, key) of actionList" @click="handleTagsOption(key)" :key="key">{{item}}</li>
    </ul>
    <div ref="scrollBody" class="tags-inner-scroll-body" :style="{left: tagBodyLeft + 'px'}">
      <transition-group name="taglist-moving-animation">
        <Tag
          type="dot"
          v-for="item in pageTagsList"
          ref="tagsPageOpened"
          :key="item.name"
          :name="item.name"
          @on-close="closePage"
          @click.native="linkTo(item)"
          :closable="item.name=='home_index'?false:true"
          :color="item.children?(item.children[0].name==currentPageName?'primary':'default'):(item.name==currentPageName?'primary':'default')"
           @contextmenu.prevent.native="contextMenu(item, $event)"
        >{{ itemTitle(item) }}</Tag>
      </transition-group>
    </div>
  </div>
</template>

<script>
export default {
  name: "tagsPageOpened",
  data() {
    return {
      currentPageName: this.$route.name, // 当前路由名称
      tagBodyLeft: 0, // 标签左偏移量
      visible: false, // 显示操作按钮
      contextMenuLeft: 0, // 内容左偏移量
      contextMenuTop: 0, // 内容上偏移量
      actionList: { // 右键菜单
        others: '关闭其他',
        clearAll: '关闭所有'
      },
      refsTag: [], // 所有已打开标签
      tagsCount: 1 // 标签数量
    };
  },
  props: {
    pageTagsList: Array,
    beforePush: {
      type: Function,
      default: item => {
        return true;
      }
    }
  },
  computed: {
    title() {
      return this.$store.state.app.currentTitle;
    },
    tagsList() {
      return this.$store.state.app.storeOpenedList;
    }
  },
  methods: {
    // 格式化标签名
    itemTitle(item) {
      if (typeof item.title == "object") {
        return item.title;
      } else {
        return item.title;
      }
    },
    // 关闭页面
    closePage(event, name) {
      let storeOpenedList = this.$store.state.app.storeOpenedList;
      let lastPageObj = storeOpenedList[0];
      if (this.currentPageName == name) {
        let len = storeOpenedList.length;
        for (let i = 1; i < len; i++) {
          if (storeOpenedList[i].name == name) {
            if (i < len - 1) {
              lastPageObj = storeOpenedList[i + 1];
            } else {
              lastPageObj = storeOpenedList[i - 1];
            }
            break;
          }
        }
      } else {
        let tagWidth = event.target.parentNode.offsetWidth;
        this.tagBodyLeft = Math.min(this.tagBodyLeft + tagWidth, 0);
      }
      this.$store.commit("removeTag", name);
      this.$store.commit("closePage", name);
      storeOpenedList = this.$store.state.app.storeOpenedList;
      localStorage.storeOpenedList = JSON.stringify(storeOpenedList);
      if (this.currentPageName == name) {
        this.linkTo(lastPageObj);
      }
    },
    // 跳转
    linkTo(item) {
      if (this.$route.name == item.name) {
        return;
      }
      let routerObj = {};
      routerObj.name = item.name;
      if (item.argu) {
        routerObj.params = item.argu;
      }
      if (item.query) {
        routerObj.query = item.query;
      }
      if (this.beforePush(item)) {
        this.$router.push(routerObj);
      }
    },
    // 页签栏滚动
    handlescroll(e) {
      var type = e.type;
      let delta = 0;
      if (type == "DOMMouseScroll" || type == "mousewheel") {
        delta = e.wheelDelta ? e.wheelDelta : -(e.detail || 0) * 40;
      }
      let left = 0;
      if (delta > 0) {
        left = Math.min(0, this.tagBodyLeft + delta);
      } else {
        if (
          this.$refs.scrollCon.offsetWidth - 100 <
          this.$refs.scrollBody.offsetWidth
        ) {
          if (
            this.tagBodyLeft <
            -(
              this.$refs.scrollBody.offsetWidth -
              this.$refs.scrollCon.offsetWidth +
              100
            )
          ) {
            left = this.tagBodyLeft;
          } else {
            left = Math.max(
              this.tagBodyLeft + delta,
              this.$refs.scrollCon.offsetWidth -
                this.$refs.scrollBody.offsetWidth -
                100
            );
          }
        } else {
          this.tagBodyLeft = 0;
        }
      }
      this.tagBodyLeft = left;
    },
    // 标签右键操作
    handleTagsOption(type) {
      if (type == "clearAll") {
        this.$store.commit("clearAllTags");
        this.$router.push({
          name: "home_index"
        });
      } else {
        this.$store.commit("clearOtherTags", this);
      }
      this.tagBodyLeft = 0;
    },
    // 标签栏滚动
    moveToView(tag) {
      if (tag.offsetLeft < -this.tagBodyLeft) {
        // 标签在可视区域左侧
        this.tagBodyLeft = -tag.offsetLeft + 10;
      } else if (
        tag.offsetLeft + 10 > -this.tagBodyLeft &&
        tag.offsetLeft + tag.offsetWidth <
          -this.tagBodyLeft + this.$refs.scrollCon.offsetWidth - 100
      ) {
        // 标签在可视区域
        this.tagBodyLeft = Math.min(
          0,
          this.$refs.scrollCon.offsetWidth -
            100 -
            tag.offsetWidth -
            tag.offsetLeft -
            20
        );
      } else {
        // 标签在可视区域右侧
        this.tagBodyLeft = -(
          tag.offsetLeft -
          (this.$refs.scrollCon.offsetWidth - 100 - tag.offsetWidth) +
          20
        );
      }
    },
    // 显示操作按钮
    contextMenu (item, e) {
      this.visible = true
      const offsetLeft = this.$el.getBoundingClientRect().left
      this.contextMenuLeft = e.clientX - offsetLeft + 10
      this.contextMenuTop = e.clientY - 64
    },
    // 关闭右侧菜单
    closeMenu () {
      this.visible = false
    }
  },
  mounted() {
    this.refsTag = this.$refs.tagsPageOpened;
    setTimeout(() => {
      this.refsTag.forEach((item, index) => {
        if (this.$route.name == item.name) {
          let tag = this.refsTag[index].$el;
          this.moveToView(tag);
        }
      });
    }, 1); // 这里不设定时器就会有偏移bug
    this.tagsCount = this.tagsList.length;
  },
  watch: {
    $route(to) {
      this.currentPageName = to.name;
      this.$nextTick(() => {
        this.refsTag.forEach((item, index) => {
          if (to.name == item.name) {
            let tag = this.refsTag[index].$el;
            this.moveToView(tag);
          }
        });
      });
      this.tagsCount = this.tagsList.length;
    },
    visible (value) {
      if (value) {
        document.body.addEventListener('click', this.closeMenu)
      } else {
        document.body.removeEventListener('click', this.closeMenu)
      }
    }
  }
};
</script>
<style lang="scss" scoped>
.contextmenu {
  position: absolute;
  margin: 0;
  padding: 5px 0;
  background: #fff;
  z-index: 11000;
  list-style-type: none;
  border-radius: 4px;
   box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);
  li {
    margin: 0;
    padding: 5px 15px;
    cursor: pointer;
    &:hover {
      background: rgba($color: $theme_color, $alpha: .1);
    }
  }
}

.ivu-tag-primary{
  /deep/ .ivu-tag-dot-inner{
    background: $theme_color !important;
  }
}

</style>
