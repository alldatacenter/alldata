<template>
	<view class="similar-goods">
		<view class="goods" @click="goDetail(goods.goods_id)">
			<image :src="goods.goods_img" mode=""></image>
			<view class="goods-intro">
				<view>{{goods.goodsName}}</view>
				<view>{{goods.goods_sn}}</view>
				<view>¥{{goods.goods_price | unitPrice}}</view>
			</view>
			<!-- <button>找相似</button> -->
		</view>
		<view class="title">相似好货&nbsp;为您推荐</view>
		<view class="scroll-con">
			<view v-if="nomsg">没有相似商品</view>
			<view v-else class="con" v-for="(item,index) in goodsList" :key="index" @click="goDetail(item)">
				<image :src="item.content.thumbnail" mode=""></image>
				<view class="nowrap">{{item.content.name}}</view>
				<view>
					<text>￥{{item.content.price | unitPrice}}
					<!-- <text v-if="item.point">+{{item.point || 0}}积分</text> -->
					</text>
					<text>￥{{item.content.mktprice}}</text>
				</view>
				<view>
					<text>已售{{item.content.buy_count}}件</text>
					<text>{{item.content.grade}}%好评</text>
				</view>
			</view>
		</view>
		<uni-load-more :status="loadStatus"></uni-load-more>
	</view>
</template>

<script>
	import {
		getGoodsList
	} from '@/api/goods.js';
	export default {
		data() {
			return {
				loadStatus: 'more',
				params: {
					pageNumber: 1,
					pageSize: 10,
					keyword: ''
				},
				goods: {},
				goodsList: [],
				nomsg: false,
			};
		},
		methods: {
			getList() {
				uni.showLoading({
					title: "加载中"
				})
				this.params.keyword = this.goods.goodsName;
				getGoodsList(this.params).then(res => {
					uni.hideLoading()
					if (res.statusCode == 200) {
						let data = res.data;
						if (data.data_total == 0) {
							// this.nomsg = true;
							this.loadStatus = 'noMore';
						} else if (data.data_total < 10) {
							this.loadStatus = 'noMore'
							this.goodsList.push(...data.data)
						} else {
							this.goodsList.push(...data.data);
							if (data.data.length < 10) this.loadStatus = 'noMore'
						}
					}
				})
			},
			goDetail(item) {
				uni.navigateTo({
					url: '/pages/product/goods?id=' + item.content.id + "&goodsId=" +item.content.goodsId
				})
			},
			loadData() {
				if(this.loadStatus!='noMore'){
					this.params.pageNumber++;
					this.getList()
				}
			},
		},
		onLoad(option) {
			this.goods = JSON.parse(decodeURIComponent(option.goods))
			
			this.getList()
		},
		onReachBottom() { //触底事件，页面整个滚动使用
			this.loadData()
		}
	}
</script>

<style lang="scss" scoped>
	@import './collect.scss';

	.title {
		height: 110rpx;
		line-height: 110rpx;
		text-align: center;
		color: #333;
		background-color: #F1F1F1;
		margin-top: 20rpx;
		font-size: $font-base;
	}

	.goods {
		padding: 0 30rpx;
	}

	.scroll-con {
		width: 750rpx;
		flex-wrap: wrap;

		.con {
			width: 345rpx;
			margin: 20rpx 0 0 20rpx;
			background-color: #FFFFFF;
			border-radius: 10rpx;
			box-sizing: border-box;
			display: inline-block;
			font-size: $font-sm;

			// line-height: 1.5em;
			image {
				width: 100%;
				height: 320rpx;
				border-radius: 8rpx 8rpx 0 0;
			}
			view{
				padding: 0 20rpx;
				&::after{
					content: '';
					display: block;
					clear: right;
				}
				text {
					display: inline-block;
					color: #999;
				}
				
				text:nth-child(2) {
					float: right;
					text-align: right;
				}
			}
			view:last-child{
				margin-bottom: 20rpx;
			}
			.nowrap {
				position: relative;
				line-height: 1.4em;
				max-height: 2.8em; //height是line-height的整数倍，防止文字显示不全
				overflow: hidden;
			}

			

			view:nth-child(2) {
				font-size: 26rpx;
			}

			view:nth-child(3) {
				margin-top: 10rpx;
				font-weight: bold;

				text:nth-child(1) {
					color: #f56c6c;
				}

				text:nth-child(2) {
					color: #d7d7d7;
					text-decoration: line-through;
				}
			}

			view:nth-child(4) {
				margin-top: 10rpx;
			}

		}
	}
</style>
