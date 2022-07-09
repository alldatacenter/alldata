<template>
	<view class="container">
		<view class="list-cell b-b m-t" hover-class="cell-hover" :hover-stay-time="50">
			<u-row gutter="12" justify="start" @click="navigateTo('/pages/msgTips/sysMsg/index')">
				<u-col span="2" class="uCol" style="text-align:center;">
					<image class="img" src="/static/mine/setting.png"></image>
				</u-col>
				<u-col span="7">
					<p class="tit_title">系统消息</p>
					<p class="tit_tips">查看系统消息</p>
				</u-col>
				<u-col span="3">
					<view class="cell-more">
						<u-tag size="mini" v-if="no_read.system_num>0" shape="circle" mode="dark" type="error" :text="no_read.system_num"></u-tag>
						<span class="yticon icon-you"></span>
					</view>
				</u-col>
			</u-row>
		</view>
		<!-- <view class="list-cell b-b m-t" hover-class="cell-hover" :hover-stay-time="50">
			<u-row gutter="12" justify="start" @click="navigateTo('/pages/msgTips/packagemsg/index')">
				<u-col span="2" class="uCol" style="text-align:center;">
					<image class="img" src="/static/mine/logistics.png"></image>

				</u-col>
				<u-col span="7">
					<p class="tit_title">物流消息</p>
					<p class="tit_tips">查看物流消息</p>
				</u-col>
				<u-col span="3">
					<view class="cell-more">
						
						<u-tag v-if="no_read.logistics_num>0" shape="circle" mode="dark" type="warning" :text="no_read.logistics_num"></u-tag>
						<span class="yticon icon-you"></span>
					</view>
				</u-col>
			</u-row>
		</view> -->
	</view>
</template>

<script>
	import {
		mapMutations
	} from "vuex";
	import * as API_Message from "@/api/members.js";
	export default {
		data() {
			return {
				no_read: ''
			};
		},
		onLoad() {
			this.GET_NoReadMessageNum();
		},
		methods: {
			...mapMutations(["logout"]),
			navigateTo(url) {
				uni.navigateTo({
					url
				});
			},
			/** 获取未读消息数量信息 */
			GET_NoReadMessageNum() {
				API_Message.getNoReadMessageNum().then(response => {
					this.no_read = response.data
				})
			}
		}
	};
</script>

<style scoped lang='scss'>
	.uCol {
		display: flex;
		justify-content: center !important;
	}

	.img {
		width: 60rpx;
		height: 60rpx;

	}

	.container {
		background: #f9f9f9;
	}

	/deep/ .u-col-2 {
		height: 60px;
		line-height: 60px;
		text-align: center !important;

	}

	.qicon {
		text-align: center;
		display: block;
		font-size: 20px;
	}

	.redBox {
		display: inline-block;
		text-align: center;
		line-height: 1.5em;
		font-size: 12px;
		min-width: 1.5em;
		min-height: 1.5em;

		background: #ed6533;
		border-radius: 50%;
		color: #fff;
	}

	.tit_title {
		color: $u-main-color;
	}

	.tit_tips {
		color: $u-tips-color;
	}

	.u-col-3 {
		text-align: right !important;
		padding-right: 20rpx !important;
	}

	.list-cell {
		background: #fff;
		align-items: baseline;
		padding: 20rpx 0;
		line-height: 60rpx;

		background: #fff;
		justify-content: center;

		&.log-out-btn {
			margin-top: 40rpx;

			.cell-tit {
				color: $uni-color-primary;
				text-align: center;
				margin-right: 0;
			}
		}

		&.cell-hover {
			background: #fafafa;
		}

		&.b-b:after {
			left: 30rpx;
		}

		&.m-t {
			margin-top: 16rpx;
		}

		.cell-more {
			/* margin-top: 10rpx; */
			height: 60rpx;
			text-align: right;
			/* display: flex;
			justify-content: center; //这个是X轴居中
			align-items: center; //这个是 Y轴居中 */
			font-size: $font-lg;
			color: $font-color-light;
			/* width: 100rpx; */
		}

		.cell-tit {
			flex: 1;
			font-size: $font-base + 2rpx;
			color: $font-color-dark;
			margin-right: 10rpx;
		}

		.cell-tip {
			font-size: $font-base;
			color: $font-color-light;
		}
	}
</style>
