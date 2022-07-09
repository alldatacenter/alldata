<template>
	<view class="default-page">
		<view class="default-wrap">
			
			<text>{{title}}</text>
			<view v-if="isBtn" class="btn" @click="toHome">
				去逛逛
			</view>
		</view>
	</view>
</template>

<script>
export default {
	props: {
		// 购物车 cart 消息 msg  订单 order 查询 search
		type: {
			type: String,
			default: 'search'
		},
		isBtn:{
			type: Boolean,
			default: false
		},
		title:{
			type: String,
			default: '没有相关内容'
		}
	},
	
	data() {
		return {
			src:''
		};
	},
	mounted() {
		this.src ='/static/default/default_'+ this.type + '.png';
	},
	methods: {
		toHome() {
			uni.switchTab({
			    url: '/pages/home/home'
			});
		}
	}
};
</script>

<style lang="scss" scoped>
	.default-page{
		background: #FFFFFF;
		width: 100%;
		height: 100vh;
		.default-wrap{
			position: absolute;
			top: calc(50vh - 320rpx);
			left: calc(50% - 120rpx);
			display: flex;
			flex-direction: column;
			align-items: center;
			
			>image{
				width: 260rpx;
				height: 240rpx;
				
				margin-bottom: 24rpx;
			}
			text{
				font-size: 32rpx;
				color: #828385;
			}
			.btn{
				width: 160rpx;
				height: 56rpx;
				border: 2rpx solid #3180F6;
				border-radius: 6rpx;
				text-align: center;
				line-height: 56rpx;
				margin-top: 32rpx;
				font-size: 24rpx;
				color: #3180F6;
			}
		}
		
	}
</style>
