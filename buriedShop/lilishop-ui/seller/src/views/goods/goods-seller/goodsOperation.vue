<template>
  <div class="goods-operation">
    <div class="step-list">
      <steps :current="activestep" style="height:60px;margin-top: 10px">
        <step title="选择商品品类"/>
        <step title="填写商品详情"/>
        <step title="商品发布成功"/>
      </steps>
    </div>
    <!-- 第一步 选择分类 -->
    <first-step ref='first' v-show="activestep === 0" @change="getFirstData"></first-step>
    <!-- 第二步 商品详细信息 -->
    <second-step ref='second' :firstData="firstData" v-if="activestep === 1"></second-step>
    <!-- 第三步 发布完成 -->
    <third-step ref='third' v-if="activestep === 2"></third-step>
    
    
  </div>
</template>
<script>
import firstStep from  './goodsOperationFirst'
import secondStep from  './goodsOperationSec'
import thirdStep from  './goodsOperationThird'
export default {
  name: "addGoods",
  components: {
    firstStep,
    secondStep,
    thirdStep
  },

  data() {
    return {
      /** 当前激活步骤*/
      activestep: 0,
      firstData: {}, // 第一步传递的数据
    };
  },
  methods: {
    // 选择商品分类回调
    getFirstData (item) {
      this.firstData = item;
      this.activestep = 1;
    }
  },
  mounted() {
    // 编辑商品、模板
    if (this.$route.query.id || this.$route.query.draftId) {
      this.activestep = 1;
    } else {
      this.activestep = 0
      this.$refs.first.selectGoodsType = true;
    }
    
  }
};
</script>
<style lang="scss" scoped>
@import "./addGoods.scss";
</style>
