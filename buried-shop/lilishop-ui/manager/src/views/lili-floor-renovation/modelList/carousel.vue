<template>
  <div class="model-carousel">
    <div class="nav-body clearfix">
      <!-- 侧边导航 -->
      <div class="nav-side">分类占位区</div>
      <div class="nav-content setup-content">
        <!-- 轮播图 -->
        <Carousel autoplay>
          <CarouselItem v-for="(item, index) in data.options.list" :key="index">
            <div style="overflow: hidden">
              <img :src="item.img" width="790" height="340" />
            </div>
          </CarouselItem>
        </Carousel>
        <div class="setup-box">
          <div>
            <Button size="small" @click.stop="handleSelectModel">编辑</Button>
          </div>
        </div>
      </div>
      <div class="nav-right">
        <div class="person-msg">
          <img :src="userInfo.face" v-if="userInfo.face" alt />
          <Avatar icon="ios-person" class="mb_10" v-else size="80" />
          <div>
            Hi，{{ userInfo.nickName || "欢迎来到管理后台" | secrecyMobile }}
          </div>
          <div v-if="userInfo.id">
            <Button type="error" shape="circle">会员中心</Button>
          </div>
          <div v-else>
            <Button type="error" shape="circle">请登录</Button>
          </div>
        </div>
        <div class="shop-msg">
          <div>
            <span>常见问题</span>
            <ul class="article-list">
              <li
                class="ellipsis"
                :alt="article.title"
                v-for="(article, index) in articleList"
                :key="index"
              >
                {{ article.title }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <Modal
      v-model="showModal"
      title="快捷导航"
      draggable
      width="800"
      :z-index="100"
      :mask-closable="false"
    >
      <div class="modal-tab-bar">
        <Button type="primary" size="small" @click="handleAdd">添加轮播</Button>
        &nbsp;
        <span class="ml_10">图片尺寸:{{ data.size }}</span>
        <div style="color: #999" class="fz_12">点击缩略图替换图片</div>
        <table cellspacing="0">
          <thead>
            <tr>
              <th width="250">所选图片</th>
              <th width="250">链接地址</th>
              <!-- <th width="150">排序</th> -->
              <th width="250">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in data.options.list" :key="index">
              <td>
                <img
                  style="cursor: pointer"
                  :src="item.img"
                  @click="handleSelectImg(item)"
                  width="200"
                  height="100"
                  alt=""
                />
              </td>
              <td>
                <Input
                  class="outsideUrl"
                  v-model="item.url"
                  :disabled="!!item.type && item.type !== 'link'"
                />
              </td>
              <!-- <td><Input v-model="item.sort"/></td> -->
              <td>
                <Button
                  type="primary"
                  size="small"
                  @click="handleSelectImg(item)"
                  >选择图片</Button
                >&nbsp;
                <Button type="info" size="small" @click="handleSelectLink(item)"
                  >选择链接</Button
                >&nbsp;
                <Button
                  type="error"
                  ghost
                  size="small"
                  @click="handleDel(index)"
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
import ossManage from "@/views/sys/oss-manage/ossManage";
export default {
  name: "modelCarousel",
  props: ["data"],
  components: {
    ossManage,
  },
  data() {
    return {
      showModal: false, // modal显隐
      selected: null, // 已选数据
      picModelFlag: false, // 选择图片modal
      userInfo: {},
      articleList: [
        { title: "促销计算规则" },
        { title: "商家申请开店" },
        { title: "商家账号注册" },
        { title: "促销计算规则" },
      ],
    };
  },

  methods: {
    handleSelectModel() {
      // 编辑模块
      this.showModal = true;
    },
    // 添加轮播图
    handleAdd() {
      this.data.options.list.push({ img: "", url: "", type: "" });
      this.$forceUpdate();
    },
    handleSelectLink(item) {
      // 选择链接
      this.$refs.liliDialog.open("link");
      this.selected = item;
    },
    callbackSelected(item) {
      // 选择图片回调
      this.picModelFlag = false;
      this.selected.img = item.url;
    },
    handleDel(index) {
      // 删除图片
      this.data.options.list.splice(index, 1);
    },
    selectedLink(val) {
      // 选择链接回调
      this.selected.url = this.$options.filters.formatLinkType(val);
      this.selected.type =
        val.___type === "other" && val.url === "" ? "link" : "other";
    },
    // 选择图片
    handleSelectImg(item) {
      this.selected = item;
      this.$refs.ossManage.selectImage = true;
      this.picModelFlag = true;
    },
  },
};
</script>

<style scoped lang="scss">
@import "./setup-box.scss";
.model-carousel {
  width: 1200px;
  height: 340px;
  overflow: hidden;
}

.nav-item li {
  float: left;
  font-size: 16px;
  font-weight: bold;
  margin-left: 30px;
}
.nav-item a {
  text-decoration: none;
  color: #555555;
}
.nav-item a:hover {
  color: $theme_color;
}
/*大的导航信息，包含导航，幻灯片等*/
.nav-body {
  width: 1200px;
  height: 340px;
  margin: 0px auto;
}
.nav-side {
  height: 100%;
  width: 200px;
  padding: 0px;
  color: #fff;
  float: left;
  background-color: #6e6568;
  line-height: 340px;
  text-align: center;
}

/*导航内容*/
.nav-content {
  width: 800px;
  height: 340px;
  overflow: hidden;
  float: left;
  position: relative;
}
.nav-right {
  float: left;
  width: 200px;
  .person-msg {
    display: flex;
    align-items: center;
    flex-direction: column;
    margin: 20px auto;

    button {
      height: 25px !important;
      margin-top: 10px;
    }

    .ivu-btn-default {
      color: $theme_color;
      border-color: $theme_color;
    }

    img {
      margin-bottom: 10px;
      width: 80px;
      height: 80px;
      border-radius: 50%;
    }
  }
  .shop-msg {
    div {
      width: 100%;
      margin: 10px 27px;
      span {
        cursor: pointer;
        text-align: center;
        font-weight: bold;
        margin-left: 5px;
      }
      span:nth-child(1) {
        color: $theme_color;
        margin-left: 0;
      }
      span:nth-child(2) {
        font-weight: normal;
      }
      span:nth-child(3):hover {
        color: $theme_color;
      }
    }

    ul {
      margin: 0 30px;
      li {
        cursor: pointer;
        margin: 5px 0;
        color: #999395;
        &:hover {
          color: $theme_color;
        }
      }
    }
  }
}
</style>
