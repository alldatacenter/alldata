

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
      actionList: {
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
      return this.$store.state.app.pageOpenedList;
    }
  },
  methods: {
    itemTitle(item) {
      if (typeof item.title == "object") {
        return this.$t(item.title.i18n);
      } else {
        return item.title;
      }
    },
    closePage(event, name) {
      let pageOpenedList = this.$store.state.app.pageOpenedList;
      let lastPageObj = pageOpenedList[0];
      if (this.currentPageName == name) {
        let len = pageOpenedList.length;
        for (let i = 1; i < len; i++) {
          if (pageOpenedList[i].name == name) {
            if (i < len - 1) {
              lastPageObj = pageOpenedList[i + 1];
            } else {
              lastPageObj = pageOpenedList[i - 1];
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
      pageOpenedList = this.$store.state.app.pageOpenedList;
      localStorage.pageOpenedList = JSON.stringify(pageOpenedList);
      if (this.currentPageName == name) {
        this.linkTo(lastPageObj);
      }
    },
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
        if ( this.$refs.scrollCon.offsetWidth - 100 < this.$refs.scrollBody.offsetWidth ) {
          if ( this.tagBodyLeft <  -( this.$refs.scrollBody.offsetWidth - this.$refs.scrollCon.offsetWidth + 100 ) ) {
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
    contextMenu (item, e) {
      this.visible = true
      const offsetLeft = this.$el.getBoundingClientRect().left
      this.contextMenuLeft = e.clientX - offsetLeft + 10
      this.contextMenuTop = e.clientY - 64
    },
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

<style lang="scss">
@import "@/views/main.scss";
.contextmenu {
  position: absolute;
  margin: 0;
  padding: 5px 0;
  background: #fff;
  z-index: 11000;
  list-style-type: none;
  border-radius: 4px;
  box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, .1);
  li {
    margin: 0;
    padding: 5px 15px;
    cursor: pointer;
    &:hover {
      background: #eee;
    }
  }
}
.ivu-tag-primary, .ivu-tag-primary.ivu-tag-dot .ivu-tag-dot-inner{
  background: $theme_color;
}
</style>
