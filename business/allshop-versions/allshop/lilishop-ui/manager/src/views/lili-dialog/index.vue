<template>
  <Modal :styles="{ top: '120px' }" width="1160" @on-cancel="clickClose" @on-ok="clickOK" v-model="flag" :mask-closable="false" scrollable>
    <template v-if="flag">
      <goodsDialog @selected="(val) => {goodsData = val;}"
        v-if="goodsFlag" ref="goodsDialog" :selectedWay='goodsData'/>
      <linkDialog @selectedLink="(val) => { linkData = val; }" v-else class="linkDialog" />
    </template>
  </Modal>
</template>
<script>
import goodsDialog from "./goods-dialog";
import linkDialog from "./link-dialog";
export default {
  components: {
    goodsDialog,
    linkDialog
  },
  data() {
    return {
      goodsFlag: false, // 是否商品选择器
      goodsData: [], //选择的商品
      linkData: "", //选择的链接
      flag: false, // modal显隐
    };
  },
  methods: {
    // 关闭弹窗
    clickClose() {
      this.$emit("closeFlag", false);
      this.goodsFlag = false;
    },
    // 单选商品
    singleGoods() {
      var timer = setInterval(() => {
        if (this.$refs.goodsDialog) {
          this.$refs.goodsDialog.type = "single";
          clearInterval(timer);
        }
      }, 100);
    },
    // 点击确认
    clickOK() {
      if (this.goodsFlag) {
        this.$emit("selectedGoodsData", this.goodsData);
      } else {
        this.$emit("selectedLink", this.linkData);
      }
      this.clickClose();
    },
    // 打开组件方法
    open(type, mutiple) {
      this.flag = true;
      if (type == "goods") {
        this.goodsFlag = true;
        if (mutiple) {
          this.singleGoods()
        }
      } else {
        this.goodsFlag = false;
      }

    },
    // 关闭组件
    close() {
      this.flag = false;
    },
  },
};
</script>
<style scoped lang="scss">
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
