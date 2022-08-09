<template>
	<view class="myTracks">
		<u-navbar title="我的足迹">

		</u-navbar>
		<u-empty text="暂无历史记录" style="margin-top:200rpx;" mode="history" v-if="whetherEmpty"></u-empty>
		<div v-else>
			<view v-for="(item, index) in trackList" :key="index">
				<view class="myTracks-title" @click="navgaiteToStore(item)">{{item.storeName}}</view>
				<view class="myTracks-items">

					<u-swipe-action style="width: 100%;" :show="item.show" :index="index" :key="item.id"
						@click="delTracks" @open="open" :options="options">
						<view class="myTracks-item">
							<view class="myTracks-item-img" @click.stop="navgaiteToDetail(item)">
								<image :src="item.thumbnail"></image>
							</view>
							<view class="myTracks-item-content" @click.stop="navgaiteToDetail(item)">
								<view class="myTracks-item-title">
									{{ item.goodsName }}
									<view class="myTracks-item-title-desc"> </view>
								</view>
								<view class="myTracks-item-price">
									￥{{ item.price | unitPrice }}
								</view>
							</view>
						</view>
					</u-swipe-action>

				</view>
				<view class="myTracks-divider"></view>

			</view>

		</div>

	</view>
</template>

<script>
	import {
		myTrackList,
		deleteHistoryListId
	} from "@/api/members.js";

	export default {
		data() {
			return {

				whetherEmpty: false, //是否数据为空
				params: {
					pageNumber: 1,
					pageSize: 10,
					order: "desc",
					sort: "updateTime",
				},
				options: [{
					text: '删除',
					style: {
						backgroundColor: '#dd524d'
					}
				}],
				trackList: [], //足迹列表
			};
		},

		/**
		 * 滑到底部加载下一页数据
		 */
		onReachBottom() {
			this.params.pageNumber++;
			this.getList();
		},
		onShow() {
			this.trackList = [];
			this.getList();
		},
		onPullDownRefresh() {
			this.trackList = [];
			this.getList();
		},
		methods: {
			/**
			 * 导航到店铺
			 */
			navgaiteToStore(val) {
				uni.navigateTo({
					url: "/pages/product/shopPage?id=" + val.storeId,
				});
			},
			open(index) {
				// 先将正在被操作的swipeAction标记为打开状态，否则由于props的特性限制，
				// 原本为'false'，再次设置为'false'会无效
				this.trackList[index].show = true;
				this.trackList.map((val, idx) => {
					if (index != idx) this.trackList[idx].show = false;
				})
			},
			/**
			 * 跳转详情
			 */
			navgaiteToDetail(item) {
				uni.navigateTo({
					url: "/pages/product/goods?id=" + item.id + "&goodsId=" + item.goodsId,
				});
			},

			/**
			 * 获取我的足迹列表
			 */
			getList() {
				uni.showLoading({
					title: "加载中",
				});
				myTrackList(this.params).then((res) => {
					uni.stopPullDownRefresh();
					uni.hideLoading();
					if (res.statusCode == 200) {
						res.data.result.records.length &&
							res.data.result.records.forEach((item) => {
								item.show = false;
							});

						let data = res.data.result.records;
						if (data.total == 0) {
							this.whetherEmpty = true;
						} else {
							this.trackList.push(...data);
						}
					}
				});
			},


			/**
			 * 删除足迹
			 */
			delTracks(index) {
				deleteHistoryListId(this.trackList[index].goodsId).then((res) => {
					if (res.data.code == 200) {
						this.trackList = [];
						this.getList();
					} else {
						uni.showToast({
							title: res.data.message,
							duration: 2000,
							icon: "none",
						});
					}
				});
			},
		},
	};
</script>

<style lang="scss" scoped>
	.myTracks {
		width: 100%;
		padding-top: 2rpx;
	}

	.myTracks-title {
		width: 100%;
		height: 110rpx;
		padding-left: 20rpx;
		font-size: 28rpx;
		color: #666;
		font-weight: bold;
		background-color: #fff;
		align-items: center;
		display: -webkit-box;
		display: -webkit-flex;
		display: flex;
	}

	.myTracks-items {
		padding-top: 2rpx;
		align-items: center;
		display: -webkit-box;
		display: -webkit-flex;
		display: flex;
		flex-direction: column;
	}

	.myTracks-item {
		width: 100%;
		height: 226rpx;
		padding-left: 20rpx;
		padding-right: 20rpx;
		margin-bottom: 2rpx;
		// border-radius: 6/@px;
		background-color: #fff;
		position: relative;
		align-items: center;
		display: -webkit-box;
		display: -webkit-flex;
		display: flex;
	}

	.myTracks-item-img {
		margin-right: 20rpx;
		border-radius: 8rpx;

		image {
			width: 130rpx;
			height: 130rpx;
			border-radius: 8rpx;
		}
	}

	.myTracks-item-title {
		font-size: 28rpx;
		color: #333;
	}

	.myTracks-item-title-desc {
		font-size: 28rpx;
		color: #999;
	}

	.myTracks-item-price {
		font-size: 28rpx;
		color: $light-color;
		padding: 10rpx 0 0 0;
	}

	.myTracks-action {
		display: flex;
		justify-content: space-between;
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		background: #fff;
		height: 75rpx;
		align-items: center;
		padding: 0 32rpx;
	}

	.myTracks-action-btn {
		width: 130rpx;
		height: 60rpx;
		line-height: 60rpx;
	}

	.myTracks-divider {
		width: 100%;
		height: 20rpx;
	}



	.myTracks-action-check {
		align-items: center;
		display: -webkit-box;
		display: -webkit-flex;
		display: flex;
	}
</style>
