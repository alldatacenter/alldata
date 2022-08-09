<template>
  <Modal
    :title="title"
    :styles="{ top: '120px' }"
    width="750"
    @on-cancel="clickClose"
    @on-ok="clickOK"
    v-model="flag"
    :mask-closable="false"
    scrollable
  >
    <goodsDialog
      @selected="(val) => {goodsData = val;}"
      :selectedWay='goodsData'
      ref="goodsDialog"
      v-if="goodsFlag"
    />
    <linkDialog
      @selectedLink="(val) => {linkData = val;}"
      v-else
      class="linkDialog"
    />
  </Modal>
</template>
<script>
import goodsDialog from "./goods-dialog";
import linkDialog from "./link-dialog";
export default {
  components: {
    goodsDialog,
    linkDialog,
  },
  data() {
    return {
      title: "选择", // 模态框标题
      goodsFlag: false, // 是否商品选择器
      goodsData: [], //选择的商品
      linkData: "", //选择的链接
      flag: false, // 控制模态框显隐
    };
  },
  methods: {
    // 关闭弹窗
    clickClose() {
      this.$emit("closeFlag", false);
      this.goodsFlag = false;
    },

    // 单选商品
    singleGoods(){
      var timer = setInterval(() => {
        if (this.$refs.goodsDialog) {

          this.$refs.goodsDialog.type = "single";
         clearInterval(timer);
        }
      }, 100);
    },
    clickOK() { // 确定按钮回调，
      if (this.goodsFlag) {
        this.$emit("selectedGoodsData", this.goodsData);
      } else {
        this.$emit("selectedLink", this.linkData);
      }
      this.clickClose();
    },
    open (type) { // 父组件通过ref调用，打开商品选择器
      this.flag = true;
      if(type == 'goods'){
        this.goodsFlag = true;
      } else {
        this.goodsFlag = false
      }

    },
    close(){ // 关闭组件
      this.flag = false;
    }
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
