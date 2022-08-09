<template>
	<view class="coupon-center">
		<div class="swiper-box">
			<div class="swiper-item">
				<div class="scroll-v" enableBackToTop="true" scroll-y>
					<u-empty mode="coupon" style='margin-top: 20%;' text="没有优惠券了" v-if="whetherEmpty"></u-empty>
					<view v-else class="coupon-item" v-for="(item, index) in couponList" :key="index">
						<view class="left">
							<view class="wave-line">
								<view class="wave" v-for="(item, index) in 12" :key="index"></view>
							</view>
							<view class="message">
								<view>
									<!--判断当前优惠券类型  couponType  PRICE || DISCOUNT -->
									<span v-if="item.couponType == 'DISCOUNT'">{{ item.couponDiscount }}折</span>
									<span v-else>{{ item.price }}元</span>
								</view>
								<view>满{{ item.consumeThreshold | unitPrice }}元可用</view>
							</view>
							<view class="circle circle-top"></view>
							<view class="circle circle-bottom"></view>
						</view>
						<view class="right">
							<view>
								<!-- 根据scopeType 判断是否是 平台、品类或店铺  -->
								<view class="coupon-title wes-3" v-if="item.scopeType">
									<span v-if="item.scopeType == 'ALL' && item.storeId == '0'">全平台</span>
									<span v-if="item.scopeType == 'PORTION_CATEGORY'">仅限品类</span>
									<view v-else>{{ item.storeName == 'platform' ? '全平台' :item.storeName+'店铺' }}使用
									</view>
								</view>
								<view v-if="item.endTime">有效期至：{{ item.endTime.split(" ")[0] }}</view>
							</view>
							<view class="receive" @click="receive(item)">
								<text>点击</text><br />
								<text>领取</text>
							</view>
							<view class="bg-quan"> 券 </view>
						</view>
					</view>

				</div>
			</div>
		</div>
	</view>
</template>

<script>
	import {
		receiveCoupons
	} from "@/api/members.js";
	import {
		getAllCoupons
	} from "@/api/promotions.js";
	export default {
		data() {
			return {
				loadStatus: "more", //下拉状态
				whetherEmpty: false, //是否为空
				couponList: [], // 优惠券列表
				params: {
					pageNumber: 1,
					pageSize: 10,
				},
				storeId: "", //店铺 id,
				couponData: ""
			};
		},
		onLoad(option) {
			this.storeId = option.storeId;
			this.getCoupon();
		},
		onReachBottom() {

			this.loadMore()
		},
		onPullDownRefresh() {
			//下拉刷新
			this.params.pageNumber = 1;
			this.couponList = [];
			this.getCoupon();
		},
		methods: {
			/**
			 * 获取当前优惠券
			 */
			getCoupon() {
				uni.showLoading({
					title: "加载中",
				});
				let submitData = {
					...this.params
				};
				// 判断当前是否有店铺
				this.storeId ? (submitData = {
						...this.params,
						storeId: this.storeId
					}) : "",
					getAllCoupons(submitData)
					.then((res) => {
						uni.hideLoading();
						uni.stopPullDownRefresh();
						if (res.data.code == 200) {
							// 如果请求成功，展示数据并进行展示
							this.couponData = res.data.result
							if (this.couponData.total == 0) {
								// 当本次请求数据为空展示空信息
								this.whetherEmpty = true;
							} else {
								this.couponList.push(...this.couponData.records);
								this.loadStatus = "noMore";
							}
						}
					})
					.catch((err) => {
						uni.hideLoading();
					});
			},
			/**
			 * 领取优惠券
			 */
			receive(item) {
				receiveCoupons(item.id).then((res) => {
					if (res.data.code == 200) {
						uni.showToast({
							title: "领取成功",
							icon: "none",
						});
					} else {
						uni.showToast({
							title: res.data.message,
							icon: "none",
						});
					}
				});
			},
			/**
			 * 加载更多
			 */
			loadMore() {
				if (this.couponData.total > this.params.pageNumber * this.params.pageSize) {
					this.params.pageNumber++;
					this.getCoupon();
				}
			},
		},
		onNavigationBarButtonTap(e) {
			uni.navigateTo({
				url: "/pages/cart/coupon/couponIntro",
			});
		},
	};
</script>
<style>
	page {
		height: 100%;
	}
</style>
<style lang="scss" scoped>
	.coupon-center {
		height: 100%;

		.swiper-box {
			.coupon-item {
				display: flex;
				align-items: center;
				height: 220rpx;
				margin: 20rpx;

				.left {
					height: 100%;
					width: 260rpx;
					background-color: $light-color;
					position: relative;

					.message {
						color: $font-color-white;
						display: flex;
						justify-content: center;
						align-items: center;
						flex-direction: column;
						margin-top: 40rpx;

						view:nth-child(1) {
							font-weight: bold;
							font-size: 60rpx;
						}

						view:nth-child(2) {
							font-size: $font-sm;
						}
					}

					.wave-line {
						height: 220rpx;
						width: 8rpx;
						position: absolute;
						top: 0;
						left: 0;
						background-color: $light-color;
						overflow: hidden;

						.wave {
							width: 8rpx;
							height: 16rpx;
							background-color: #ffffff;
							border-radius: 0 16rpx 16rpx 0;
							margin-top: 4rpx;
						}
					}

					.circle {
						width: 40rpx;
						height: 40rpx;
						background-color: $bg-color;
						position: absolute;
						border-radius: 50%;
						z-index: 111;
					}

					.circle-top {
						top: -20rpx;
						right: -20rpx;
					}

					.circle-bottom {
						bottom: -20rpx;
						right: -20rpx;
					}
				}

				.right {
					display: flex;
					justify-content: space-between;
					align-items: center;
					width: 450rpx;
					font-size: $font-sm;
					height: 100%;
					background-color: #ffffff;
					overflow: hidden;
					position: relative;

					>view:nth-child(1) {
						color: #666666;
						margin-left: 20rpx;
						display: flex;
						height: 100%;
						flex-direction: column;
						justify-content: space-around;

						>view:nth-child(1) {
							color: #ff6262;
							font-size: 30rpx;
						}
					}

					.receive {
						color: #ffffff;
						background-color: $main-color;
						border-radius: 50%;
						width: 86rpx;
						height: 86rpx;
						text-align: center;
						margin-right: 30rpx;
						vertical-align: middle;
						padding-top: 8rpx;
						position: relative;
						z-index: 2;
					}

					.bg-quan {
						width: 244rpx;
						height: 244rpx;
						border: 6rpx solid $main-color;
						border-radius: 50%;
						opacity: 0.1;
						color: $main-color;
						text-align: center;
						padding-top: 30rpx;
						font-size: 130rpx;
						position: absolute;
						right: -54rpx;
						bottom: -60rpx;
					}
				}
			}
		}
	}

	.coupon-title {
		width: 260rpx;

	}
</style>
