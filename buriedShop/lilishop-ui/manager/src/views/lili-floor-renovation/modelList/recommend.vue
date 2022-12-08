<template>
  <div class="recommend">
    <div class="recommend-left">
      <div
        class="head-recommend setup-content"
        :style="{ background: msgLeft.bgColor }"
      >
        <span>{{ msgLeft.title }}</span>
        <span>{{ msgLeft.secondTitle }}&gt;</span>
        <div class="setup-box">
          <div>
            <Button size="small" @click.stop="handleSelectModel(msgLeft, true)"
              >编辑</Button
            >
          </div>
        </div>
      </div>
      <div class="content-left">
        <div class="setup-content">
          <img :src="msgLeft.list[0].img" width="160" height="160" alt="" />
          <div class="margin-left">{{ msgLeft.list[0].name }}</div>
          <div class="margin-left">{{ msgLeft.list[0].describe }}</div>
          <Button
            size="small"
            :style="{ background: msgLeft.bgColor }"
            class="fz_12 view-btn"
            >点击查看</Button
          >
          <div class="setup-box">
            <div>
              <Button
                size="small"
                @click.stop="handleSelectModel(msgLeft.list[0])"
                >编辑</Button
              >
            </div>
          </div>
        </div>
        <div>
          <template v-for="(item, index) in msgLeft.list">
            <div v-if="index != 0" :key="index" class="setup-content">
              <img :src="item.img" width="80" height="80" alt="" />
              <div>
                <div>{{ item.name }}</div>
                <div>{{ item.describe }}</div>
              </div>
              <div class="setup-box">
                <div>
                  <Button size="small" @click.stop="handleSelectModel(item)"
                    >编辑</Button
                  >
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
    <div class="recommend-right">
      <div
        class="head-recommend setup-content"
        :style="{ background: msgRight.bgColor }"
      >
        <span>{{ msgRight.title }}</span>
        <span>{{ msgRight.secondTitle }}&gt;</span>
        <div class="setup-box">
          <div>
            <Button size="small" @click.stop="handleSelectModel(msgRight, true)"
              >编辑</Button
            >
          </div>
        </div>
      </div>
      <div class="content-right">
        <div
          v-for="(item, index) in msgRight.list"
          :key="index"
          class="setup-content"
        >
          <div
            class="right-item"
            :style="{ border: index === 2 || index === 3 ? 'none' : '' }"
          >
            <div>
              <span :style="{ background: msgRight.bgColor }">{{
                item.name
              }}</span>
              <span>{{ item.describe }}</span>
            </div>
            <div class="right-img">
              <img :src="item.img" alt="" />
            </div>
          </div>
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
            v-if="selected.size && selected.size.indexOf('80*80') >= 0"
            :src="selected.img"
            alt
          />
          <img
            class="show-image"
            width="100"
            height="100"
            v-if="selected.size && selected.size.indexOf('100*100') >= 0"
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
          <Button
            size="small"
            class="ml_10"
            type="primary"
            @click="handleSelectLink"
            >选择链接</Button
          >
        </div>
        <div>
          <Button size="small" type="primary" @click="handleSelectImg"
            >选择图片</Button
          >&nbsp;
          <Button size="small" type="primary" @click="handleSelectGoods"
            >选择商品</Button
          >
        </div>
      </div>
    </Modal>
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
          <span>背景色：</span><Input v-model="selected.bgColor" />
          <ColorPicker v-if="selected.bgColor" v-model="selected.bgColor" />
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
      default: {},
    },
  },
  components: {
    ossManage,
  },
  data() {
    return {
      msgLeft: this.data.options.contentLeft, // 左侧数据
      msgRight: this.data.options.contentRight, // 右侧数据
      showModal: false, // modal显隐
      showModal1: false, // modal显隐
      selected: {}, // 已选数据
      picModelFlag: false, // 图片选择
    };
  },
  methods: {
    // 编辑
    handleSelectModel(item, type) {
      this.selected = item;
      if (type) {
        this.showModal1 = true;
      } else {
        this.showModal = true;
      }
    },
    handleSelectLink(item, index) {
      // 调起选择链接弹窗
      this.$refs.liliDialog.open("link");
    },
    handleSelectGoods(item) {
      // 调起选择商品
      this.$refs.liliDialog.open("goods", "single");
    },
    // 选择链接回调
    selectedLink(val) {
      this.selected.url = this.$options.filters.formatLinkType(val);
      this.selected.type =
        val.___type === "other" && val.url === "" ? "link" : "other";
    },
    // 选择商品回调
    selectedGoodsData(val) {
      console.log(val);
      let goods = val[0];
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
    // 选择图片回调
    callbackSelected(val) {
      this.picModelFlag = false;
      this.selected.img = val.url;
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./setup-box.scss";
.recommend {
  display: flex;
  justify-content: space-between;
  .recommend-left {
    width: 595px;
    .content-left {
      display: flex;
      padding-top: 10px;
      font-size: 12px;
      > div:nth-child(1) {
        width: 189px;
        border-right: 1px solid #eee;
        height: 360px;
        img {
          margin: 40px 0 0 15px;
        }
        .margin-left {
          margin-left: 15px;
          width: 145px;
        }
        div:nth-of-type(1) {
          font-weight: bold;
          border-top: 1px solid #eee;
          padding-top: 10px;
          padding-bottom: 10px;
        }
        div:nth-of-type(2) {
          color: #999;
        }
        .view-btn {
          margin-left: 15px;
          margin-top: 10px;
          color: #fff;
        }
      }
      > div:nth-child(2) {
        width: 405px;
        display: flex;
        flex-wrap: wrap;
        > div {
          display: flex;
          align-items: center;
          width: 200px;
          height: 120px;
          img {
            margin: 0 10px;
          }
          > div:nth-child(2) {
            // margin: 0 10px;
            :nth-child(2) {
              color: #449dae;
            }
          }
        }
      }
    }
  }

  .recommend-right {
    width: 595px;
    height: 360px;
    .head-recommend {
      background: #a25684;
    }
    .content-right {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: center;
      padding-top: 10px;
      > div {
        width: 50%;
        text-align: center;
        height: 180px;
        padding-top: 10px;
        .right-item {
          border-bottom: 1px solid #eee;
          display: flex;
          margin-top: 30px;
          margin-left: 5px;
          margin-right: 5px;
          height: 150px;
          padding: 0 10px;
          font-size: 12px;
          > div:nth-child(1) {
            width: 130px;
            margin-top: 30px;
            span:nth-child(1) {
              color: #fff;
              border-radius: 10px;
              padding: 0 5px;
              background-color: #a25684;
              display: block;
              width: 120px;
              overflow: hidden;
              white-space: nowrap;
              margin: 0 10px 10px 0;
            }
            span:nth-child(2) {
              font-size: 12px;
              color: #666;
              display: block;
            }
          }
          .right-img {
            width: 100;
            height: 100px;
            text-align: center;
            margin: 0 auto;
            img {
              max-height: 100px;
              max-width: 100px;
            }
          }
        }
      }
      > div:nth-child(n + 1) {
        border-right: 1px solid #eee;
      }
    }
  }

  .head-recommend {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 50px;
    padding: 0 10px;
    background: #449dae;
    color: #fff;
    span:nth-child(1) {
      font-size: 20px;
    }
    span:nth-child(2) {
      font-size: 12px;
    }
  }
}

.modal-top-advert {
  align-items: start;
  padding: 0 30px;
}
</style>