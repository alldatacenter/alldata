<template>
  <view class="recommend-box" >
    <h4 class="goods-recommend-title">宝贝推荐</h4>
		<goodsList :res='res' v-if="res" :storeName="false" />
	 </view>
</template>

<script>
import goodsList from '@/components/m-goods-list/list.vue'
export default {
  props: ["res"],
	components:{goodsList},
  methods: {
    }
};
</script>

<style lang="scss" scoped>
@import "../product.scss";
</style>
