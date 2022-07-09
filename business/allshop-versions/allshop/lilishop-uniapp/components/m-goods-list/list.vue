<template>
	<view>
		<!-- 一行两列商品展示 -->
		<view class="goods-list" v-if="type == 'twoColumns'">
			<view v-for="(item, index) in res" :key="index" class="goods-item">
				<view class="image-wrapper" @click="navigateToDetailPage(item)">
					<u-image :src="item.content.thumbnail" width="100%" height='330rpx' mode="aspectFill">
						  <u-loading slot="loading"></u-loading>
					</u-image>
				</view>
				<view class="goods-detail">
					<div class="title clamp" @click="navigateToDetailPage(item)">{{ item.content.goodsName }}</div>
					<view class="price-box" @click="navigateToDetailPage(item)">
						<div class="price" v-if="item.content.price!=undefined">
							¥<span>{{ formatPrice(item.content.price )[0] }} </span>.{{
	            formatPrice(item.content.price )[1]
	          }}
						</div>
					</view>
					<div class="promotion" @click="navigateToDetailPage(item)">
						<div v-if="item.content.salesModel == 'WHOLESALE'">
							<span>批</span>
						</div>
						<div v-for="(promotionItem,promotionIndex) in  getPromotion(item)" :key="promotionIndex">
							<span v-if="promotionItem.indexOf('COUPON') != -1">劵</span>
							<span v-if="promotionItem.indexOf('FULL_DISCOUNT') != -1">满减</span>
							<span v-if="promotionItem.indexOf('SECKILL') != -1">秒杀</span>
						</div>
					</div>
					<div class="count-config" @click="navigateToDetailPage(item)">
						<span>已售 {{ item.content.buyCount || "0" }}</span>
						<span>{{ item.content.commentNum || "0" }}条评论</span>
					</div>
					<div class="store-seller-name" v-if="storeName" @click="navigateToStoreDetailPage(item)">
						<div class="text-hidden" >
							<u-tag style="margin-right: 10rpx" size="mini" mode="dark" v-if="item.selfOperated"
								text="自营" type="error" />
							<span >{{ item.content.storeName || "暂无" }}</span>
						</div>
						<span>
							<u-icon name="arrow-right"></u-icon>
						</span>
					</div>
				</view>
			</view>
		</view>
		<!-- 一行一列商品展示 -->
		<div v-if="type == 'oneColumns'">
			<div v-for="(item, index) in res" :key="index" class="goods-row">
				<div class="flex goods-col">
					<div class="goods-img" @click="navigateToDetailPage(item)">
						<u-image width="230rpx" border-radius='16' height="230rpx" :src="item.content.thumbnail">
							<u-loading slot="loading"></u-loading>
						</u-image>
					</div>
					<div class="goods-detail">
						<div class="title clamp3" @click="navigateToDetailPage(item)">{{ item.content.goodsName }}</div>
						<view class="price-box" @click="navigateToDetailPage(item)">
							<div class="price" v-if="item.content.price!=undefined">
								¥<span>{{ formatPrice(item.content.price )[0] }} </span>.{{
			            formatPrice(item.content.price )[1]
			          }}
							</div>
						</view>
						<div class="promotion" @click="navigateToDetailPage(item)">
							<div v-if="item.content.salesModel == 'WHOLESALE'">
								<span>批</span>
							</div>
							<div v-for="(promotionItem,promotionIndex) in  getPromotion(item)" :key="promotionIndex">
								<span v-if="promotionItem.indexOf('COUPON') != -1">劵</span>
								<span v-if="promotionItem.indexOf('FULL_DISCOUNT') != -1">满减</span>
								<span v-if="promotionItem.indexOf('SECKILL') != -1">秒杀</span>
							</div>
						</div>
						<div style="overflow: hidden" @click="navigateToDetailPage(item)" class="count-config">
							<span style="float: left; font-size: 22rpx">已售 {{ item.content.buyCount || '0' }}</span>
							<span style="float: right; font-size: 22rpx">{{ item.content.commentNum || '0' }}条评论</span>
						</div>
						<div style="overflow: hidden" @click="navigateToStoreDetailPage(item)" class="count-config">
							<div class="text-hidden" v-if="storeName">
								<u-tag style="margin-right: 10rpx" size="mini" mode="dark" v-if="item.selfOperated"
									text="自营" type="error" />
								<span class="line1-store-name">{{ item.content.storeName }}</span>
								<span class="to-store">进店<u-icon size="24" name="arrow-right" color="#666"></u-icon>
								</span>
							</div>
							<span>
								<u-icon name="arrow-right" color="#c5c5c5"></u-icon>
							</span>
						</div>
					</div>
				</div>

			</div>
		</div>

	</view>
</template>

<script>
	export default {
		data() {
			return {}
		},
		props: {
			// 遍历的数据
			res: {
				type: Array,
				default: () => {
					return []
				}
			},
			// 一行两列还是一行一列显示
			type: {
				type: String,
				default: 'twoColumns',
				validator() {
					return ['twoColumns', 'oneColumns']
				}
			},
			storeName: {
				type: Boolean,
				default: true
			}

		},
		methods: {
			// 格式化金钱  1999 --> [1999,00]
			formatPrice(val) {
				if (typeof val == "undefined") {
					return val;
				}
				return val.toFixed(2).split(".");
			},

			// 数据去重一下 只显示一次 减免 劵 什么的
			getPromotion(item) {
				if (item.promotionMap) {
					let array = [];
					Object.keys(item.promotionMap).forEach((child) => {
						if (!array.includes(child.split("-")[0])) {
							array.push(child.split("-")[0]);
						}
					});
					return array;
				}
			},
			// 跳转到商品详情
			navigateToDetailPage(item) {
				uni.navigateTo({
					url: `/pages/product/goods?id=${item.content.id}&goodsId=${item.content.goodsId}`,
				});
			},
			// 跳转地址
			navigateToStoreDetailPage(item) {
				uni.navigateTo({
					url: `/pages/product/shopPage?id=${item.content.storeId}`,
				});
			},
		}
	}
</script>

<style lang='scss' scoped>
	/* 商品列表 */
	.goods-list {
		display: flex;
		flex-wrap: wrap;
		margin: 10rpx 20rpx 284rpx;
		width: 100%;

		>.goods-item {
			background-color: #ffffff;
			display: flex;
			border-radius: 16rpx;
			flex-direction: column;
			width: calc(50% - 30rpx);
			margin-bottom: 20rpx;
			padding-bottom: 20rpx;

			&:nth-child(2n + 1) {
				margin-right: 20rpx;
			}


			.image-wrapper {
				width: 100%;
				height: 330rpx;
				border-radius: 16rpx 16rpx 0 0;
				overflow: hidden;
				padding: 0;
			}
		}

		.count-config,
		.store-seller-name {
			font-size: $font-sm;
		}

		.text-hidden {
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
	}


	.goods-row {
		background: #fff;
		padding: 16rpx;

		>.goods-col {
			display: flex;

			>.goods-img {
				overflow: hidden;
				flex: 4;
			}

			>.goods-detail {
				flex: 7;
			}
		}
	}

	.goods-detail {
		margin: 0 20rpx;

		>.title {
			font-size: $font-base;
			color: $font-color-dark;
			line-height: 1.5;
			height: 84rpx;
			padding: 10rpx 0 0;
			display: -webkit-box;
			-webkit-box-orient: vertical;
			-webkit-line-clamp: 2;
			overflow: hidden;
		}

		.promotion {
			margin-top: 4rpx;
			display: flex;

			div {
				span {
					font-size: 24rpx;
					color: $light-color;
					margin-right: 10rpx;
					padding: 0 4rpx;
					border-radius: 2rpx;
				}
			}
		}

		.store-seller-name {
			color: #666;
			overflow: hidden;
			display: flex;
			justify-content: space-between;
		}

		.count-config {
			padding: 5rpx 0;
			color: #666;
			display: flex;
			font-size: 24rpx;
			justify-content: space-between;
		}

		>.price-box {
			margin-top: 10rpx;
			display: flex;
			align-items: center;
			justify-content: space-between;
			padding-right: 10rpx;
			font-size: 24rpx;
			color: $font-color-light;

			>.price {
				font-size: 26rpx;
				line-height: 1;
				color: $main-color;
				font-weight: bold;

				/deep/ span:nth-of-type(1) {
					font-size: 38rpx;
				}
			}
		}

	}
</style>
