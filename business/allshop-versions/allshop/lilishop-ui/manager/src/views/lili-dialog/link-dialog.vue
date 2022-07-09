<template>
  <div class="wrapper">
    <Tabs :value="wap[0].title" class="tabs">
      <TabPane
        :label="item.title"
        :name="item.title"
        @click="clickTag(item, i)"
        v-for="(item, i) in wap"
        :key="i"
      >
        <component
          ref="lili-component"
          :is="templateWay[item.name]"
          @selected="
            (val) => {
              changed = val;
            }
          "
        />
      </TabPane>
    </Tabs>
  </div>
</template>
<script>
import wap from "./wap.js";
import goodsDialog from "./goods-dialog";
import templateWay from "./template/index";
export default {
  components: {
    goodsDialog,
  },
  data() {
    return {
      templateWay, // 模板数据
      changed: "", // 变更模板
      selected: 0, // 已选数据
      selectedLink: "", //选中的链接
      wap, // tab标签
    };
  },
  watch: {
    changed: {
      handler(val) {
        this.$emit("selectedLink", val[0]); //因为是单选，所以直接返回第一个
      },
      deep: true,
    },
  },
  mounted() {
    this.$nextTick(() => {
      this.$refs["lili-component"][0].type = "single"; //商品页面设置成为单选
    });

    this.wap.forEach((item) => {
      if (item) {
        item.selected = false;
      }
    });
  },
  methods: {},
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
.tabs {
  width: 100%;
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
