<template>
	<view class="wrapper">
		<div class="coupon-empty" v-if="!res">暂无优惠券</div>
		<view class="coupon-List" v-for="(item, index) in couponRes" :key="index">
			<view class="coupon-item">
				<view class="top">
					<div class="price">
						<span v-if="item.couponType == 'DISCOUNT'">{{ item.couponDiscount }}折</span>
						<span v-if="item.couponType == 'PRICE'">￥{{ item.price | unitPrice }}</span>
					</div>
					<view class="text">
						<div class="coupon-List-title">
							<view v-if="item.scopeType">
								<span v-if="item.scopeType == 'ALL' && item.storeId == '0'">全平台</span>
								<span v-if="item.scopeType == 'PORTION_CATEGORY'">仅限品类</span>
								<view v-else>{{
                         item.storeName == "platform" ? "全平台" : item.storeName + "店铺"
                       }}使用</view>
							</view>
						</div>
						<div>满{{ item.consumeThreshold | unitPrice }}可用</div>
					</view>
					<view class="lingqu-btn" @click="getCoupon(item, index)">
						<div :class="yhqFlag[index] ? 'cur' : ''">
							{{ yhqFlag[index] ? "已领取或领完" : "立即领取" }}
						</div>
					</view>
				</view>
				<view class="line"></view>
				<view class="time">{{ item.startTime / 1000 | unixToDate }} - {{ item.endTime / 1000 | unixToDate }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				yhqFlag: [], //获取优惠券判断是否点击
				couponRes: [],
			};
		},
		props: {
			res: {
				type: null,
				default: "",
			},
		},
		watch: {
			res: {
				handler() {
					if (this.res && this.res.length != 0) {
						Object.keys(this.res).forEach((item) => {
							let key = item.split("-")[0];
							if (key === "COUPON") {
								this.couponRes.push(this?.res[item]);

							}
						});
					}
				},
				immediate: true,
			},
		},
		methods: {
			// 提交优惠券
			getCoupon(item, index) {
				this.yhqFlag[index] = true;
				this.$emit("getCoupon", item);
			},
		},
	};
</script>

<style lang="scss" scoped>
	.coupon-item {
		width: 100%;
		height: 100%;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
	}

	.coupon-List {
		display: flex;
		flex-direction: column;
		height: 230rpx;
		background: #e9ebfb;
		margin: 30rpx 0;
		padding: 10rpx 30rpx;

		.line {
			height: 1px;
			background: #fff;
			margin: 0 20rpx;
			position: relative;

			&:before,
			&:after {
				content: "";
				display: block;
				width: 15rpx;
				height: 30rpx;
				background: #fff;
				position: absolute;
				top: -15rpx;
			}

			&:before {
				left: -50rpx;
			}

			&:after {
				right: -50rpx;
			}
		}

		.time {
			flex: 1;
			font-size: 24rpx;
			align-items: center;
			display: flex;
			align-items: center;
		}
	}

	.top {
		height: 140rpx;
		display: flex;

		.price {
			width: 33%;
			justify-content: center;
			color: #6772e5;
			font-size: 40rpx;
			display: flex;

			height: 100%;
			align-items: center;

			span {
				font-size: 50rpx;
			}
		}

		.text {
			width: 33%;
			display: flex;
			flex-direction: column;
			justify-content: center;
			font-size: 26rpx;
			color: 333;
			margin-left: 40rpx;

			.coupon-List-title {
				font-size: 30rpx;
				font-weight: bold;
			}
		}

		.lingqu-btn {
			display: flex;
			align-items: center;
			margin-left: 40rpx;

			text {
				width: 140rpx;
				height: 40rpx;
				text-align: center;
				line-height: 40rpx;
				color: #fff;
				background: #6772e5;
				border-radius: 5px;
				font-size: 26rpx;

				&.cur {
					background: none;
					transform: rotate(45deg) translate(10rpx, -46rpx);
				}
			}
		}
	}
</style>
