<template>
  <div class="new-goods">
    <div class="left">
      <div
        class="top-header setup-content"
        :style="{ background: options.left.bgColor }"
      >
        <span>{{ options.left.title }}</span>
        <span>{{ options.left.secondTitle }} &gt;</span>
        <div class="setup-box">
          <div>
            <Button
              size="small"
              @click.stop="handleSelectModel(options.left, true)"
              >编辑</Button
            >
          </div>
        </div>
      </div>
      <div class="content">
        <div
          class="con-item setup-content"
          v-for="(item, index) in options.left.list"
          :key="index"
        >
          <div>
            <p>{{ item.name }}</p>
            <p class="describe">{{ item.describe }}</p>
          </div>
          <img :src="item.img" alt="" />
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

    <div class="middle">
      <div
        class="top-header setup-content"
        :style="{ background: options.middle.bgColor }"
      >
        <span>{{ options.middle.title }}</span>
        <span>{{ options.middle.secondTitle }} &gt;</span>
        <div class="setup-box">
          <div>
            <Button
              size="small"
              @click.stop="handleSelectModel(options.middle, true)"
              >编辑</Button
            >
          </div>
        </div>
      </div>
      <div class="content">
        <div
          class="con-item setup-content"
          v-for="(item, index) in options.middle.list"
          :key="index"
        >
          <div>
            <p>{{ item.name }}</p>
            <p class="describe">{{ item.describe }}</p>
          </div>
          <img :src="item.img" alt="" />
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

    <div class="right">
      <div
        class="top-header setup-content"
        :style="{ background: options.right.bgColor }"
      >
        <span>{{ options.right.title }}</span>
        <span>{{ options.right.secondTitle }} &gt;</span>
        <div class="setup-box">
          <div>
            <Button
              size="small"
              @click.stop="handleSelectModel(options.right, true)"
              >编辑</Button
            >
          </div>
        </div>
      </div>
      <div class="content">
        <div
          class="setup-content"
          v-for="(item, index) in options.right.list"
          :key="index"
        >
          <img :src="item.img" alt="" />
          <p>{{ item.name }}</p>
          <p>{{ item.price | unitPrice("￥") }}</p>
          <div class="jiaobiao" :class="'jiaobiao' + (index + 1)">
            {{ index + 1 }}
          </div>
          <div class="setup-box">
            <div>
              <Button size="small" @click.stop="handleSelectGoods(item)"
                >编辑</Button
              >
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 装修内容 -->
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
          <img
            class="show-image"
            width="160"
            height="160"
            v-if="selected.size && selected.size.indexOf('160*160') >= 0"
            :src="selected.img"
            alt
          />
          <img
            class="show-image"
            width="80"
            height="80"
            v-if="selected.size && selected.size.indexOf('90*90') >= 0"
            :src="selected.img"
            alt
          />
        </div>
        <div><span>图片主标题：</span><Input v-model="selected.name" /></div>
        <div><span>图片描述：</span><Input v-model="selected.describe" /></div>
        <div class="tips">
          建议尺寸：<span>{{ selected.size }}</span>
        </div>
        <div>
          图片链接：<Input
            class="outsideUrl"
            v-model="selected.url"
            :disabled="!!selected.type && selected.type !== 'link'"
            placeholder="https://"
          />
          <Button size="small" type="primary" @click="handleSelectLink"
            >选择链接</Button
          >
        </div>
        <div>
          <Button size="small" type="primary" @click="handleSelectImg"
            >选择图片</Button
          >&nbsp;

          <Button size="small" type="primary" @click="handleSelectGoods('')"
            >选择商品</Button
          >
        </div>
      </div>
    </Modal>
    <!-- 装修标题 -->
    <Modal
      v-model="showModal1"
      title="装修"
      draggable
      width="800"
      :z-index="100"
      :mask-closable="false"
    >
      <div class="modal-top-advert">
        <div><span>主标题：</span><Input v-model="selected.title" /></div>
        <div><span>副标题：</span><Input v-model="selected.secondTitle" /></div>
        <div>
          <span
            >副标题链接：<Input
              class="outsideUrl"
              v-model="selected.url"
              :disabled="!!selected.type && selected.type !== 'link'"
              placeholder="https://" /></span
          ><Button
            size="small"
            class="ml_10"
            type="primary"
            @click="handleSelectLink"
            >选择链接</Button
          >
        </div>
        <div>
          <span>背景色：</span
          ><ColorPicker v-if="selected.bgColor" v-model="selected.bgColor" />
        </div>
      </div>
    </Modal>
    <!-- 选择商品。链接 -->
    <liliDialog
      ref="liliDialog"
      @selectedLink="selectedLink"
      @selectedGoodsData="selectedGoodsData"
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
import ossManage from "@/views/sys/oss-manage/ossManage";
export default {
  props: {
    data: {
      type: Object,
      default: null,
    },
  },
  components: {
    ossManage,
  },
  data() {
    return {
      options: this.data.options, // 当前数据
      showModal: false, // modal显隐
      showModal1: false, // modal显隐
      selected: {}, // 已选数据
      picModelFlag: false, // 选择图片modal
    };
  },
  methods: {
    // 装修modal
    handleSelectModel(item, type) {
      this.selected = item;
      console.warn(item);
      if (type) {
        this.showModal1 = true;
      } else {
        this.showModal = true;
      }
    },
    handleSelectLink() {
      // 调起选择链接弹窗
      this.$refs.liliDialog.open("link");
    },
    handleSelectGoods(item) {
      // 调起选择商品
      console.warn(item);
      if (item) this.selected = item;
      this.$refs.liliDialog.open("goods", "single");
      setTimeout(() => {
        this.$refs.liliDialog.goodsData = [this.selected];
      }, 500);
    },
    // 选择链接回调
    selectedLink(val) {
      this.selected.url = this.$options.filters.formatLinkType(val);
      this.selected.type =
        val.___type === "other" && val.url === "" ? "link" : "other";
    },
    // 选择商品回调
    selectedGoodsData(val) {
      let goods = val[0];
      console.log(this.selected);
      this.selected.img = goods.thumbnail;
      this.selected.price = goods.price;
      this.selected.name = goods.goodsName;
      this.selected.url = `/goodsDetail?skuId=${goods.id}&goodsId=${goods.goodsId}`;
    },
    handleSelectImg() {
      // 选择图片
      this.$refs.ossManage.selectImage = true;
      this.picModelFlag = true;
    },
    // 选择图片回显
    callbackSelected(val) {
      this.picModelFlag = false;
      this.selected.img = val.url;
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./setup-box.scss";
.new-goods {
  display: flex;
  justify-content: space-between;
  > div {
    width: 393px;
    height: 440px;
  }

  .left > .content {
    > div:nth-child(1) {
      height: 240px;
      flex-direction: column;
      border: 1px solid #eee;
      border-top: none;
      border-left: none;
      justify-content: space-between;
      img {
        width: 160px;
        height: 160px;
      }
      .describe {
        margin-top: 10px;
      }
    }
    > div:nth-child(2) {
      border-right: 1px solid #eee;
    }
    > div:nth-child(3),
    > div:nth-child(4) {
      border-bottom: 1px solid #eee;
    }
  }

  .middle > .content {
    > div {
      border-style: solid;
      border-color: #eee;
      border-width: 0;
      border-bottom-width: 1px;
    }
    > div:nth-child(1),
    > div:nth-child(2),
    > div:nth-child(3) {
      border-right-width: 1px;
    }
    > div:nth-child(6),
    > div:nth-child(3) {
      border-bottom-width: 0;
    }
  }

  .right > .content {
    display: flex;
    flex-wrap: wrap;
    flex-direction: row;
    font-size: 12px;
    > div {
      position: relative;
      width: 120px;
      padding: 5px 10px 0 10px;
      img {
        width: 100px;
        height: 100px;
      }
      border-bottom: 1px solid #eee;
      :nth-child(2) {
        height: 38px;
        overflow: hidden;
      }
      :nth-child(3) {
        color: $theme_color;
        margin-top: 5px;
      }
      .jiaobiao {
        position: absolute;
        width: 23px;
        height: 23px;
        top: 10px;
        right: 16px;
        background: url("../../../assets/festival_icon.png");
        color: #fff;
        text-align: center;
      }
      .jiaobiao1,
      .jiaobiao4 {
        background-position: -2px -30px;
      }
      .jiaobiao2,
      .jiaobiao5 {
        background-position: -31px -30px;
      }
      .jiaobiao3,
      .jiaobiao6 {
        background-position: -60px -30px;
      }
    }
    > div:nth-child(4),
    > div:nth-child(5),
    > div:nth-child(6) {
      border: none;
    }
  }

  .top-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 50px;
    padding: 0 10px;
    background: #c43d7e;
    color: #fff;
    span:nth-child(1) {
      font-size: 20px;
    }
    span:nth-child(2) {
      font-size: 12px;
    }
  }
  .content {
    padding: 10px 12px 0;
    display: flex;
    flex-wrap: wrap;
    flex-direction: column;
    height: 370px;
  }
  .con-item {
    width: 185px;
    height: 120px;
    display: flex;
    padding-left: 10px;
    padding-top: 10px;
    img {
      width: 90px;
      height: 90px;
      margin-top: 10px;
    }
  }
  .describe {
    color: #999;
    font-size: 12px;
    margin-top: 15px;
  }
}
.modal-top-advert {
  align-items: start;
  padding: 0 30px;
}
</style>