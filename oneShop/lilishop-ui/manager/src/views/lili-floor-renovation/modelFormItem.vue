<template>
  <div class="model-item" v-if="element && element.key">
    <!-- 轮播图模块，包括个人信息，快捷导航模块 -->
    <template v-if="element.type == 'carousel'">
      <model-carousel :data="element"></model-carousel>
    </template>
    <!-- 轮播图模块，100%宽度，无个人信息栏 -->
    <template v-if="element.type == 'carousel1'">
      <model-carousel1 class="mb_20" :data="element"></model-carousel1>
    </template>
    <!-- 轮播图模块，包括个人信息，两个轮播模块 -->
    <template v-if="element.type == 'carousel2'">
      <model-carousel2 class="mb_20" :data="element"></model-carousel2>
    </template>
    <!-- 热门广告 -->
    <template v-if="element.type == 'hotAdvert'">
      <div class="setup-content">
        <img
          style="display: block"
          :src="element.options.list[0].img"
          @click="$router.push(element.options.list[0].url)"
          width="1200"
          alt=""
        />
        <div class="setup-box">
          <div>
            <Button
              size="small"
              @click.stop="handleSelectModel(element.options.list[0])"
              >编辑</Button
            >
          </div>
        </div>
      </div>
      <ul class="advert-list">
        <template v-for="(item, index) in element.options.list">
          <li
            v-if="index !== 0"
            @click="$router.push(item.url)"
            class="setup-content"
            :key="index"
          >
            <img :src="item.img" width="230" height="190" alt="" />
            <div class="setup-box">
              <div>
                <Button size="small" @click.stop="handleSelectModel(item)"
                  >编辑</Button
                >
              </div>
            </div>
          </li>
        </template>
      </ul>
    </template>
    <!-- 限时秒杀 待完善 -->
    <template v-if="element.type == 'seckill'">
      <seckill :data="element"></seckill>
    </template>
    <!-- 折扣广告 -->
    <template v-if="element.type == 'discountAdvert'">
      <div
        class="discountAdvert"
        :style="{
          'background-image':
            'url(' + require('@/assets/nav/decorate.png') + ')',
        }"
      >
        <div>
          <div
            v-for="(item, index) in element.options.classification"
            :key="index"
            class="setup-content"
          >
            <img :src="item.img" width="190" height="210" alt="" />
            <div class="setup-box">
              <div>
                <Button size="small" @click.stop="handleSelectModel(item)"
                  >编辑</Button
                >
              </div>
            </div>
          </div>
        </div>
        <div>
          <div
            v-for="(item, index) in element.options.brandList"
            :key="index"
            class="setup-content"
          >
            <img :src="item.img" width="240" height="105" alt="" />
            <div class="setup-box">
              <div>
                <Button size="small" @click.stop="handleSelectModel(item)"
                  >编辑</Button
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <!-- 好货推荐 -->
    <template v-if="element.type == 'recommend'">
      <recommend :data="element"></recommend>
    </template>
    <!-- 新品排行 -->
    <template v-if="element.type == 'newGoodsSort'">
      <new-goods-sort :data="element"></new-goods-sort>
    </template>
    <!-- 首页广告 -->
    <template v-if="element.type == 'firstAdvert'">
      <first-page-advert :data="element"></first-page-advert>
    </template>
    <!-- 横幅广告 -->
    <template v-if="element.type == 'bannerAdvert'">
      <div class="horizontal-advert setup-content">
        <img
          v-if="element.options.img"
          width="1200"
          :src="element.options.img"
          alt=""
        />
        <div v-else class="default-con">
          <p>广告图片</p>
          <p>1200*自定义</p>
        </div>
        <div class="setup-box">
          <div>
            <Button
              size="small"
              @click.stop="handleSelectModel(element.options)"
              >编辑</Button
            >
          </div>
        </div>
      </div>
    </template>
    <template v-if="element.type == 'notEnough'">
      <not-enough :data="element"></not-enough>
    </template>
    <div class="del-btn">
      <Button size="small" type="error" @click="handleModelDelete">删除</Button>
    </div>
    <Modal
      v-model="showModal"
      title="装修"
      draggable
      width="800"
      :z-index="100"
      :mask-closable="false"
    >
      <div class="modal-top-advert">
        <div>
          <!-- 热门广告两种图片尺寸 -->
          <img
            class="show-image"
            width="600"
            height="40"
            v-if="selected.size && selected.size.indexOf('1200') >= 0"
            :src="selected.img"
            alt
          />
          <img
            class="show-image"
            width="230"
            height="190"
            v-if="selected.size && selected.size.indexOf('230*190') >= 0"
            :src="selected.img"
            alt
          />
          <!-- 折扣广告三种图片尺寸 -->
          <img
            class="show-image"
            width="600"
            height="300"
            v-if="selected.size && selected.size.indexOf('1300') >= 0"
            :src="selected.img"
            alt
          />
          <img
            class="show-image"
            width="190"
            height="210"
            v-if="selected.size && selected.size.indexOf('190*210') >= 0"
            :src="selected.img"
            alt
          />
          <img
            class="show-image"
            width="240"
            height="105"
            v-if="selected.size && selected.size.indexOf('240*105') >= 0"
            :src="selected.img"
            alt
          />
        </div>
        <div class="tips">
          建议尺寸：<span>{{ selected.size }}</span>
        </div>
        <div>
          图片链接：<span>{{ selected.url }}</span>
          <Button size="small" type="primary" @click="handleSelectLink"
            >选择链接</Button
          >
        </div>
        <div>
          选择图片：<Button size="small" type="primary" @click="handleSelectImg"
            >选择图片</Button
          >&nbsp;
        </div>
      </div>
    </Modal>
    <!-- 选择商品。链接 -->
    <liliDialog
      ref="liliDialog"
      @selectedLink="selectedLink"
  
    ></liliDialog>
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
import ModelCarousel from "./modelList/carousel.vue";
import ModelCarousel1 from './modelList/carousel1.vue';
import ModelCarousel2 from './modelList/carousel2.vue';
import FirstPageAdvert from "./modelList/firstPageAdvert.vue";
import NewGoodsSort from "./modelList/newGoodsSort.vue";
import Recommend from "./modelList/recommend.vue";
import NotEnough from "./modelList/notEnough.vue";
import Seckill from "./modelList/seckill.vue";
import ossManage from "@/views/sys/oss-manage/ossManage";

export default {
  name: "modelFormItem",
  props: ["element", "select", "index", "data"],
  components: {
    ModelCarousel,
    ModelCarousel1,
    ModelCarousel2,
    Recommend,
    NewGoodsSort,
    FirstPageAdvert,
    NotEnough,
    Seckill,
    ossManage,
  },
  data() {
    return {
      showModal: false, // modal显隐
      selected: {}, // 已选数据
      picModelFlag: false, // 图片选择器
    };
  },
  methods: {
    // 编辑模块
    handleSelectModel(item) {
      this.selected = item;
      this.showModal = true;
    },
    // 删除模块
    handleModelDelete() {
      this.$Modal.confirm({
        title: "提示",
        content: "<p>确定删除当前模块吗？</p>",
        onOk: () => {
          this.$nextTick(() => {
            this.data.list.splice(this.index, 1);
          });
        },
      });
    },
    handleSelectLink(item, index) {
      // 调起选择链接弹窗
      this.$refs.liliDialog.open("link");
    },
    // 确定选择链接
    selectedLink(val) {
      this.selected.url = this.$options.filters.formatLinkType(val);
    },
    
    handleSelectImg() {
      // 选择图片
      this.$refs.ossManage.selectImage = true;
      this.picModelFlag = true;
    },
    // 回显图片
    callbackSelected(val) {
      this.picModelFlag = false;
      this.selected.img = val.url;
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./modelList/setup-box.scss";
.model-item {
  position: relative;
  margin-bottom: 20px;
  &:hover {
    .del-btn {
      display: block;
    }
  }
}
.del-btn {
  width: 100px;
  height: 100px;
  display: none;
  position: absolute;
  right: -100px;
  top: 0;
  &:hover {
    display: block;
  }
}
/** 横幅广告 */
.horizontal-advert {
  width: 1200px;
  height: auto;
  .default-con {
    height: 100px;
    padding-top: 30px;
    text-align: center;
    background: #ddd;
  }
}
/** 热门广告 */
.advert-list {
  background: $theme_color;
  height: 200px;
  display: flex;
  justify-content: space-around;
  padding: 3px 10px;
  > li {
    img {
      cursor: pointer;
      border-radius: 10px;
      transition: all 150ms ease-in-out;
      &:hover {
        transform: translateY(-3px);
        box-shadow: rgba(0, 0, 0, 0.4) 0px 5px 20px 0px;
      }
    }
  }
}
/** 限时秒杀 */
.limit-img {
  display: flex;
  flex-direction: row;
  img {
    width: 300px;
    height: 100px;
  }
}
/** 折扣广告 */
.discountAdvert {
  height: 566px;
  background-repeat: no-repeat;
  margin-left: -97px;
  position: relative;
  > div {
    padding-left: 295px;
    display: flex;
    flex-wrap: wrap;
    &:nth-child(1) img {
      margin: 10px 10px 0 0;
    }
    &:nth-child(2) img {
      margin: 0 10px 0 0;
    }
  }
}
/** 首页品牌 */
.brand {
  .brand-view {
    display: flex;
    margin-top: 10px;
    .brand-view-content {
      width: 470px;
      margin-left: 10px;
      img {
        width: 100%;
        height: 316px;
      }
      .brand-view-title {
        height: 50px;
        padding: 0 5px;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
    }
    .brand-view-content:first-child {
      width: 240px;
      margin-left: 0;
    }
  }
  .brand-list {
    margin-top: 10px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    li {
      width: 121px;
      height: 112px;
      position: relative;
      overflow: hidden;
      border: 1px solid #f5f5f5;
      margin: -1px -1px 0 0;
      &:hover {
        .brand-mash {
          display: flex;
        }
      }
      .brand-img {
        text-align: center;
        margin-top: 30px;
        img {
          width: 100px;
          height: auto;
        }
      }
      .brand-mash {
        display: none;
        position: absolute;
        top: 0;
        left: 0;
        background: rgba(0, 0, 0, 0.5);
        width: inherit;
        height: inherit;
        font-size: 12px;
        font-weight: bold;
        .ivu-icon {
          position: absolute;
          right: 10px;
          top: 10px;
          font-size: 15px;
        }
        align-items: center;
        justify-content: center;
        flex-direction: column;
        color: #fff;
        cursor: pointer;
        div:last-child {
          background-color: $theme_color;
          border-radius: 9px;
          padding: 0 10px;
          margin-top: 5px;
        }
      }
    }
    .refresh {
      display: flex;
      align-items: center;
      flex-direction: column;
      justify-content: center;
      .ivu-icon {
        font-size: 18px;
        transition: all 0.3s ease-out;
      }
      &:hover {
        background-color: $theme_color;
        color: #fff;
        .ivu-icon {
          transform: rotateZ(360deg);
        }
      }
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

