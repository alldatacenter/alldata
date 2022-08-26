<template>
  <div class="first-page-advert">
    <div
      class="item setup-content"
      :style="{
        backgroundImage: `linear-gradient(to right, ${item.fromColor}, ${item.toColor})`,
      }"
      v-for="(item, index) in options.list"
      :key="index"
    >
      <div>
        <span class="line top-line"></span>
        <p>{{ item.name }}</p>
        <span class="line btm-line"></span>
        <p>{{ item.describe }}</p>
      </div>
      <img :src="item.img" width="170" height="170" alt="" />
      <div class="setup-box">
        <div>
          <Button size="small" @click.stop="handleSelectModel(item)"
            >编辑</Button
          >
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
            width="170"
            height="170"
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
          <span>渐变背景色：</span><Input v-model="selected.fromColor" />
          <ColorPicker v-if="selected.fromColor" v-model="selected.fromColor" />
        </div>
        <div>
          <span>渐变背景色：</span><Input v-model="selected.toColor" />
          <ColorPicker v-if="selected.toColor" v-model="selected.toColor" />
        </div>
        <div
          :style="{
            backgroundImage: `linear-gradient(to right, ${selected.fromColor}, ${selected.toColor})`,
          }"
          class="exhibition"
        ></div>
        <div>
          选择图片：<Button size="small" type="primary" @click="handleSelectImg"
            >选择图片</Button
          >&nbsp;
        </div>
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
  props: {
    data: {
      type: Object,
      default: null,
    },
  },
  components: { ossManage },
  data() {
    return {
      options: this.data.options, // 当前类型数据
      showModal: false, // modal显隐
      selected: {}, // 已选数据
      picModelFlag: false, // 图片选择器
    };
  },
  methods: {
    // 打开装修modal
    handleSelectModel(item, type) {
      this.selected = item;
      this.showModal = true;
    },
    handleSelectLink(item, index) {
      // 调起选择链接弹窗
      this.$refs.liliDialog.open("link");
    },
    // 选择链接回调
    selectedLink(val) {
      this.selected.url = this.$options.filters.formatLinkType(val);
      this.selected.type =
        val.___type === "other" && val.url === "" ? "link" : "other";
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
.first-page-advert {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  // margin-top: -10px;
  .item {
    width: 393px;
    height: 170px;
    margin-top: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    img {
      margin-left: 20px;
    }

    &:nth-of-type(1),
    &:nth-of-type(2),
    &:nth-of-type(3) {
      margin-top: 0;
    }

    p:nth-of-type(1) {
      margin: 3px 0;
      font-size: 18px;
      color: #fff;
    }
    p:nth-of-type(2) {
      margin-top: 3px;
      color: #fff;
    }
  }
  .line {
    position: relative;
    display: block;
    height: 2px;
    background: url(../../../assets/festival_icon.png);
    z-index: 1;
  }
  .top-line {
    width: 78px;
    background-position: -1px -3px;
  }
  .btm-line {
    background-position: 0 -11px;
    width: 154px;
  }
}
.modal-top-advert {
  align-items: start;
  padding: 0 30px;
  .exhibition {
    width: 300px;
    height: 50px;
  }
}
</style>