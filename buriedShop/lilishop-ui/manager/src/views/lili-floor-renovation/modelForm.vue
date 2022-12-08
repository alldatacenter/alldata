<template>
  <div class="model-form">
    <div class="model-content">
      <!-- 头部广告，登录信息，不需要拖拽 -->
      <div
        class="top-fixed-advert"
        :style="{ backgroundColor: topAdvert.bgColor }"
      >
        <img :src="topAdvert.img" width="1200" height="80" alt="" />
        <div class="setup-box">
          <Button size="small" @click.stop="handleModel('topAdvert')"
            >编辑</Button
          >
        </div>
      </div>
      <div class="header-con">
        <div></div>
        <ul class="detail">
          <li>立即注册</li>
          <li>请登录</li>
          <li>我的订单</li>
          <li>我的足迹</li>
          <li><Icon size="18" type="ios-cart-outline"></Icon>购物车</li>
          <li>店铺入驻</li>
        </ul>
      </div>
      <div class="search-con">
        <img :src="require('@/assets/logo.png')" class="logo" alt="" />
        <div class="search">
          <i-input size="large" placeholder="输入你想查找的商品">
            <Button slot="append">搜索</Button>
          </i-input>
        </div>
      </div>
      <div class="nav-con">
        <div class="all-categories">全部商品分类</div>
        <ul class="nav-item">
          <li v-for="(item, index) in navList.list" :key="index">
            <a href="#">{{ item.name }}</a>
          </li>
        </ul>
        <div class="setup-box">
          <Button size="small" @click.stop="handleModel('quickNav')"
            >编辑</Button
          >
        </div>
      </div>
      <!-- 装修主体 -->
      <div>
        <draggable
          class="model-form-list"
          v-model="data.list"
          v-bind="{ group: 'model', ghostClass: 'ghost' }"
          @end="handleMoveEnd"
          @add="handleModelAdd"
        >
          <template v-for="(element, index) in data.list">
            <model-form-item
              v-if="element && element.key"
              :key="element.key"
              :element="element"
              :index="index"
              :data="data"
            ></model-form-item>
          </template>
        </draggable>
      </div>
    </div>
    <Modal
      v-model="showModal"
      title="顶部广告"
      draggable
      width="800"
      :z-index="100"
      :mask-closable="false"
    >
      <!-- 顶部广告 -->
      <div class="modal-top-advert">
        <div>
          <img
            class="show-image"
            width="600"
            height="40"
            :src="topAdvert.img"
            alt
          />
        </div>
        <div class="tips">
          建议尺寸：<span>{{ topAdvert.size }}</span>
        </div>
        <div>
          图片链接：<Input
            class="outsideUrl"
            v-model="topAdvert.url"
            :disabled="!!topAdvert.type && topAdvert.type !== 'link'"
            placeholder="https://"
          /><Button size="small" type="primary" @click="handleSelectLink"
            >选择链接</Button
          >
        </div>
        <div>
          选择图片：<Button size="small" type="primary" @click="handleSelectImg"
            >选择图片</Button
          >&nbsp;
        </div>
        <div>选择背景色：<ColorPicker v-model="topAdvert.bgColor" /></div>
      </div>
    </Modal>
    <Modal
      v-model="showModalNav"
      title="快捷导航"
      draggable
      width="800"
      :z-index="100"
      :mask-closable="false"
    >
      <!-- 分类tab栏 -->
      <div class="modal-tab-bar">
        <Button type="primary" size="small" @click="handleAddNav"
          >添加分类</Button
        >
        <table cellspacing="0">
          <thead>
            <tr>
              <th width="250">分类名称</th>
              <th width="250">链接地址</th>
              <!-- <th width="150">排序</th> -->
              <th width="250">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in navList.list" :key="index">
              <td><Input v-model="item.name" /></td>
              <td>
                <Input
                  v-model="item.url"
                  :disabled="!!item.type && item.type !== 'link'"
                />
              </td>
              <!-- <td><Input v-model="item.sort"/></td> -->
              <td>
                <Button
                  type="primary"
                  size="small"
                  @click="handleSelectLink(item, index)"
                  >选择链接</Button
                >&nbsp;
                <Button type="error" size="small" @click="handleDelNav(index)"
                  >删除</Button
                >
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </Modal>
    <!-- 选择商品。链接 -->
    <liliDialog ref="liliDialog" @selectedLink="selectedLink"></liliDialog>
    <!-- 选择图片 -->
    <Modal width="1200px" v-model="picModelFlag" footer-hide>
      <ossManage
        @callback="callbackSelected"
        :isComponent="true"
        ref="ossManage"
      />
    </Modal>
  </div>
</template>
<script>
import Draggable from "vuedraggable";
import ModelFormItem from "./modelFormItem.vue";
import ossManage from "@/views/sys/oss-manage/ossManage";
export default {
  name: "modelForm",
  components: {
    Draggable,
    ModelFormItem,
    ossManage,
  },
  props: ["data"],
  data() {
    return {
      picModelFlag: false, // 选择图片模态框
      showModal: false, // 顶部广告模态框
      showModalNav: false, // 分类nav模态框
      selectedNav: null, //当前已选nav
      // 模拟搜索框下方数据
      promotionTags: [
        "买2免1",
        "领200神券",
        "199减100",
        "母婴5折抢",
        "充100送20",
      ], // 热词数据
      topAdvert: {
        // 头部广告图数据
        type: "topAdvert",
        img: "",
        url: "",
        bgColor: "#de000d",
        size: "1200*80",
      },
      navList: {
        // 分类nav数据
        type: "navBar",
        list: [
          { name: "秒杀", url: "" },
          { name: "闪购", url: "" },
          { name: "优惠券", url: "" },
          { name: "拍卖", url: "" },
          { name: "服装城", url: "" },
        ],
      },
    };
  },
  mounted() {
    document.body.ondrop = function (event) {
      let isFirefox = navigator.userAgent.toLowerCase().indexOf("firefox") > -1;
      if (isFirefox) {
        event.preventDefault();
        event.stopPropagation();
      }
    };
  },
  methods: {
    handleSelectLink(item, index) {
      // 调起选择链接弹窗
      if (item) this.selectedNav = item;
      this.$refs.liliDialog.open("link");
      console.log(item);
    },
    // 已选链接
    selectedLink(val) {
      if (this.showModalNav) {
        this.selectedNav.url = this.$options.filters.formatLinkType(val);
        this.selectedNav.type =
          val.___type === "other" && val.url === "" ? "link" : "other";
      } else {
        this.topAdvert.url = this.$options.filters.formatLinkType(val);
        this.topAdvert.type =
          val.___type === "other" && val.url === "" ? "link" : "other";
      }
    },
    handleDelNav(index) {
      // 删除导航
      this.navList.list.splice(index, 1);
    },
    handleAddNav() {
      // 添加导航
      this.navList.list.push({ name: "", url: "" });
    },
    // 拖动结束回调
    handleMoveEnd({ newIndex, oldIndex }) {
      console.log("index", newIndex, oldIndex);
    },
    // 修改顶部广告
    handleModel(type) {
      if (type == "topAdvert") {
        this.showModal = true;
      } else {
        this.showModalNav = true;
      }
    },
    // 选择图片
    handleSelectImg() {
      this.$refs.ossManage.selectImage = true;
      this.picModelFlag = true;
    },
    callbackSelected(item) {
      // 选择图片回调
      this.picModelFlag = false;
      this.topAdvert.img = item.url;
    },
    handleModelAdd(evt) {
      // 拖拽，添加模块
      const newIndex = evt.newIndex;

      // 为拖拽到容器的元素添加唯一 key
      this.data.list[newIndex] = JSON.parse(
        JSON.stringify(this.data.list[newIndex])
      );
      const key =
        Date.parse(new Date()) + "_" + Math.ceil(Math.random() * 99999);
      this.$set(this.data.list, newIndex, {
        ...this.data.list[newIndex],
        options: {
          ...this.data.list[newIndex].options,
        },
        key,
        // 绑定键值
        model: this.data.list[newIndex].type + "_" + key,
      });
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./modelList/setup-box.scss";
.model-form {
  width: 1500px;
}
.model-content {
  width: 1200px;
  margin: 0 auto;
  background: #fff;
  min-height: 1200px;
}
.model-form-list {
  min-height: 500px;
}
/**  顶部广告，头部，搜索框 start */
.top-fixed-advert {
  display: flex;
  width: 1500px;
  margin-left: -150px;
  background: $theme_color;
  justify-content: center;
}

.header-con {
  display: flex;
  justify-content: space-between;
  height: 35px;
  padding: 0 15px;
  line-height: 35px;
  color: #999;
  font-weight: bold;
  div,
  li {
    &:hover {
      color: $theme_color;
      cursor: pointer;
    }
  }
  .detail {
    display: flex;
    > li {
      margin-left: 10px;
      &::after {
        content: "|";
        padding-left: 10px;
      }
      &:last-child::after {
        content: "";
        padding-left: 0;
      }
      &:hover::after {
        color: #999;
      }
    }
  }
}
/** 搜索框 */
.search-con {
  padding-top: 15px;
  margin: 0px auto;
  margin-bottom: 10px;
  width: 1200px;
  position: relative;
  .logo {
    position: absolute;
    top: 10px;
    left: 10px;
    width: 150px;
    height: 50px;
  }
  .search {
    width: 460px;
    margin: 0 auto;
    /deep/ .ivu-input.ivu-input-large {
      border: 2px solid $theme_color;
      font-size: 12px;
      height: 34px;
      &:focus {
        box-shadow: none;
      }
    }
    /deep/ .ivu-input-group-append {
      border: 1px solid $theme_color;
      border-left: none;
      height: 30px;
      background-color: $theme_color;
      color: #ffffff;
      button {
        font-size: 14px;
        font-weight: 600;
        line-height: 1;
      }
    }
  }
}
/** 商品分类 */
.nav-con {
  width: 1200px;
  height: 40px;
  background: #eee;
  display: flex;
  .all-categories {
    width: 200px;
    line-height: 40px;
    color: #fff;
    background-color: $theme_color;
    text-align: center;
    font-size: 16px;
  }
  .nav-item {
    width: 1000px;
    height: 40px;
    line-height: 40px;
    overflow: hidden;
    list-style: none;
    background-color: #eee;
    display: flex;
    li {
      font-size: 16px;
      font-weight: bold;
      margin-left: 20px;
      a {
        color: rgb(89, 88, 88);
        font-size: 15px;
        &:hover {
          color: $theme_color;
        }
      }
    }
  }
}
/**  顶部广告，头部，搜索框 end */

.top-fixed-advert,
.nav-con {
  position: relative;
  &:hover {
    .setup-box {
      display: block;
    }
  }
}
/** 装修模态框 内部样式start */
.modal-top-advert {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  > * {
    margin-bottom: 10px;
  }
}
</style>
