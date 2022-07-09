<template>
	<view>
		<chat></chat>
	</view>
</template>
<script>
	var chat = requirePlugin('myPlugin')
	export default {
		data() {
			return {
				
			}
		},
		methods: {
			
		},
		onLoad (e) {
			const params = JSON.parse((decodeURIComponent(e.params)))
			chat.init({
				sign: params.mpSign,  //必传，公司渠道唯一标识，腾讯云智服后台系统创建「小程序插件」渠道后，在「渠道管理」获取
				token: params.token, //非必填
				uid: params.uuid,   //用户唯一标识，如果没有则不填写，默认为空
				title: params.storageName, //非必填，如果未填写，默认获取配置标题
				isRMB: '', //商品是否显示人民币￥,默认显示，false不显示
				data: {    //参数c1,c2,c3,c4,c5用于传递用户信息，参数d1,d2,d3,d4,d5,d6用于传递商品信息，默认为空
				  c1: '',
				  c2: '',
				  c3: '',
				  c4: '',
				  c5: '',
				  d1: params.goodsName, //商品描述
				  d2: params.price, //价格
				  d3: '', //原价格
				  d4: params.goodsImg, //展示商品图片链接
				  d5: '', //商品跳转链接
				  d6: params.goodsId, //商品id
				  data: ''//加密串,非必填
				},
				viewUrl(res){  //需要跳转外部链接，则需要配置一个web-view
				if (res) {
				  wx.navigateTo({
					url: '/pages/webview/index?href=' + res
				  })
				}
				},
				setTitle(res){  //设置标题
				if (res) {
				  wx.setNavigationBarTitle({
					title: res
				  })
				}
				},
				setBarColor(res) {   //设置导航栏背景色
				if (res) {
				  wx.setNavigationBarColor({
					frontColor: '#ffffff',
					backgroundColor: res
				  })
				}
				},
				success(res){  //初始化成功时调用
				if (res.data == 'success') {
				  console.log('success');
				}
				},
				fail(res){    //初始化失败时调用
				if (res.data == 'initError') {
				  console.log(res.message);
				}
				},
				leave(res){       //离开会话页面
				if (res) {
				  console.log(res);
				}
				}
			})
		}
	}
</script>