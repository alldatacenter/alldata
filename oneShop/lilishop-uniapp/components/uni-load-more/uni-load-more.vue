<template>
	<view class="uni-load-more">
		<view class="uni-load-more__img" v-show="status === 'loading' && showIcon">
			<view class="load1">
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
			</view>
			<view class="load2">
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
			</view>
			<view class="load3">
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
				<view :style="{ background: color }"></view>
			</view>
		</view>
		<text class="uni-load-more__text" :style="{ color: color }" v-if="showText">
			{{ status === 'more' ? contentText.contentdown : status === 'loading' ? contentText.contentrefresh : contentText.contentnomore }}
		</text>
	</view>
</template>

<script>
export default {
	name: 'uni-load-more',
	props: {
		status: {
			//上拉的状态：more-loading前；loading-loading中；noMore-没有更多了
			type: String,
			default: 'more'
		},
		showIcon: {
			type: Boolean,
			default: true
		},
		showText: {
			type: Boolean,
			default: true
		},
		color: {
			type: String,
			default: '#777777'
		},
		contentText: {
			type: Object,
			default() {
				return {
					contentdown: '上拉显示更多',
					contentrefresh: '正在加载...',
					contentnomore: '没有更多数据了'
				};
			}
		}
	},
	data() {
		return {};
	}
};
</script>

<style>
@charset "UTF-8";

.uni-load-more {
	display: flex;
	flex-direction: row;
	height: 80rpx;
	align-items: center;
	justify-content: center;
}

.uni-load-more__text {
	font-size: 28rpx;
	color: #999;
}

.uni-load-more__img {
	height: 24px;
	width: 24px;
	margin-right: 10px;
}

.uni-load-more__img > view {
	position: absolute;
}

.uni-load-more__img > view view {
	width: 6px;
	height: 2px;
	border-top-left-radius: 1px;
	border-bottom-left-radius: 1px;
	background: #999;
	position: absolute;
	opacity: 0.2;
	transform-origin: 50%;
	animation: load 1.56s ease infinite;
}

.uni-load-more__img > view view:nth-child(1) {
	transform: rotate(90deg);
	top: 2px;
	left: 9px;
}

.uni-load-more__img > view view:nth-child(2) {
	transform: rotate(180deg);
	top: 11px;
	right: 0;
}

.uni-load-more__img > view view:nth-child(3) {
	transform: rotate(270deg);
	bottom: 2px;
	left: 9px;
}

.uni-load-more__img > view view:nth-child(4) {
	top: 11px;
	left: 0;
}

.load1,
.load2,
.load3 {
	height: 24px;
	width: 24px;
}

.load2 {
	transform: rotate(30deg);
}

.load3 {
	transform: rotate(60deg);
}

.load1 view:nth-child(1) {
	animation-delay: 0s;
}

.load2 view:nth-child(1) {
	animation-delay: 0.13s;
}

.load3 view:nth-child(1) {
	animation-delay: 0.26s;
}

.load1 view:nth-child(2) {
	animation-delay: 0.39s;
}

.load2 view:nth-child(2) {
	animation-delay: 0.52s;
}

.load3 view:nth-child(2) {
	animation-delay: 0.65s;
}

.load1 view:nth-child(3) {
	animation-delay: 0.78s;
}

.load2 view:nth-child(3) {
	animation-delay: 0.91s;
}

.load3 view:nth-child(3) {
	animation-delay: 1.04s;
}

.load1 view:nth-child(4) {
	animation-delay: 1.17s;
}

.load2 view:nth-child(4) {
	animation-delay: 1.3s;
}

.load3 view:nth-child(4) {
	animation-delay: 1.43s;
}

@-webkit-keyframes load {
	0% {
		opacity: 1;
	}

	100% {
		opacity: 0.2;
	}
}
</style>
