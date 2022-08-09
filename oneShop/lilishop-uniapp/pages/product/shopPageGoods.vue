<template>
	<div>
		<u-navbar :title="title"></u-navbar>
		<!-- 商品 -->
		<div class="contant">
			<view v-if="!goodsList.length" class="empty">暂无商品信息</view>
			<goodsTemplate :res='goodsList' :storeName='false' />
		</div>
	</div>

</template>

<script>
	import {
		getGoodsList
	} from "@/api/goods.js";
	import goodsTemplate from '@/components/m-goods-list/list'
	export default {
		data() {
			return {
				title: "",
				routerVal: "",
				goodsList: [],
				params: {
					pageNumber: 1,
					pageSize: 10,
					keyword: "",
					storeCatId: "",
					storeId: "",
				},
			};
		},
		components: {
			goodsTemplate
		},
		onLoad(options) {
			this.routerVal = options;
			this.params.storeId = options.storeId;
			this.params.storeCatId = options.id;
			this.title = options.title;
		},
		onShow() {
			this.goodsList = []
			this.params.pageNumber = 1;
			this.getGoodsData();
		},
		onReachBottom() {
			this.params.pageNumber++;
			this.getGoodsData();
		},
		methods: {
			async getGoodsData() {
				// #TODO
				let goodsList = await getGoodsList(this.params);
				if (goodsList.data.success) {
					this.goodsList.push(...goodsList.data.result.content);
				}
			},
		},
	};
</script>

<style lang="scss" scoped>
	.contant {
		margin-top: 20rpx;
		display: flex;
		flex-wrap: wrap;
		justify-content: space-between;

		>.empty {
			width: 100%;
			display: flex;
			justify-content: center;
			margin-top: 40rpx;
		}
}
</style>
