<template>
  <div class="wrapper">
    <div class="wap-list">
      <div
        class="wap-item"
        @click="clickTag(item, i)"
        v-for="(item, i) in wap"
        :key="i"
        :class="{ active: selected == i }"
      >
        {{ item.title }}
      </div>
    </div>
    <div class="wap-content"></div>

    <!-- 弹出选择商品的modal -->
    <Modal
      title="选择"
      :styles="{ top: '120px' }"
      width="750"
      @on-cancel="clickClose"
      @on-ok="clickClose"
      v-model="flag"
      :mask-closable="false"
      scrollable
    >
      <goodsDialog
        @selected="
          (val) => {
            goodsData = val;
          }
        "
        ref="goodsDialog"
      />
    </Modal>
  </div>
</template>
<script>
import wap from "./wap.js";
import goodsDialog from "./goods-dialog";
export default {
  components: {
    goodsDialog,
  },
  data() {
    return {
      goodsData: "", // 商品列表
      flag: false, // 控制商品模块显隐
      selected: 0, // 已选模块
      selectedLink: "", //选中的链接
      wap, // tab标签栏数据
    };
  },
  watch: {
    selectedLink(val) {
      this.$emit("selectedLink", val);
    },
  },
  mounted() {
    this.wap.forEach((item) => {
      item.selected = false;
    });
  },
  methods: {
    clickClose() {
      this.flag = false;
    },

    // 点击链接
    clickTag(val, i) {
      this.selected = i;
      if (!val.openGoods) {
        this.selectedLink = val;
      }
      //   打开选择商品
      else {
        this.$refs.goodsDialog.selectedWay = [];
        this.$refs.goodsDialog.type = "single";
        this.flag = true;
      }
    },
  },
};
</script>
<style scoped lang="scss">
@import "./style.scss";
.wap-content-list {
  display: flex;
  flex-wrap: wrap;
}
.wap-flex {
  margin: 2px;
}

/deep/ .ivu-modal {
  overflow: hidden;
  height: 650px !important;
}
/deep/ .ivu-modal-body {
  width: 100%;
  height: 500px;
  overflow: hidden;
}
</style>