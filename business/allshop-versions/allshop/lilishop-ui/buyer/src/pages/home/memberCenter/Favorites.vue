<template>
  <div class="wrapper">
    <card _Title="近期收藏" :_Tabs="favoriteWay" @_Change="change" :_Size="16" v-if="!homePage"/>
    <card _Title="近期收藏" :_Size="16" :_Tabs="favoriteWay" @_Change="change" _More="全部收藏" _Src="/home/Favorites" v-else></card>
    <div v-if="list.length">
      <template v-for="(item) in list">
        <div class="goodsItem" :key="item.skuId">
          <div class="goodsImg hover-pointer" v-if="params.type === 'GOODS'">
            <img :src="item.image" />
          </div>
          <div class="goodsImg hover-pointer" v-else>
            <img :src="item.storeLogo" />
          </div>
          <div class="goodsTitle hover-color" v-if="params.type === 'GOODS'" @click="buynow(item.skuId, item.goodsId)">
            {{item.goodsName}}
          </div>
          <div v-else class="goodsTitle hover-pointer">
            {{item.storeName}}
          </div>
          <div class="goodsPrice">
            <span v-if="params.type === 'GOODS'">{{item.price | unitPrice('￥')}}</span>
            <Tag color="error" v-if="item.selfOperated">商家自营</Tag>
          </div>
          <div class="goodsBuy">
            <Button size="small" type="primary" @click="buynow(item.skuId, item.goodsId)" v-if="params.type === 'GOODS'">立即购买</Button>
            <Button size="small" type="primary" @click="goShop(item.id)" v-else>店铺购买</Button>
            <Button size="small" v-if="params.type === 'GOODS'" @click="cancel(item.skuId)">取消收藏</Button>
            <Button size="small" v-if="params.type === 'SHOP'" @click="cancel(item.id)">取消收藏</Button>
          </div>
        </div>
      </template>
      <Spin size="large" fix v-if="spinShow"></Spin>
    </div>
    <empty v-else />
  </div>
</template>

<script>
import { collectList, cancelCollect } from '@/api/member.js'
export default {
  name: 'Favorites',
  props: {
    homePage: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      favoriteWay: ['收藏商品', '收藏店铺'], // 收藏分类
      list: [], // 收藏列表
      total: 0, // 收藏总数
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 100,
        type: 'GOODS'
      },
      spinShow: false // 加载状态
    };
  },
  methods: {
    getList () { // 获取收藏列表
      this.spinShow = true
      collectList(this.params).then(res => {
        this.spinShow = false
        if (res.success) this.list = res.result.records;
      })
    },
    change (index) { // tab栏切换
      if (index === 0) { this.params.type = 'GOODS' }
      if (index === 1) { this.params.type = 'SHOP' }
      this.getList()
    },
    cancel (id) { // 取消收藏
      let typeName = this.params.type === 'GOODS' ? '商品' : '店铺'
      this.$Modal.confirm({
        title: 'Title',
        content: `<p>确定取消收藏该${typeName}吗？</p>`,
        onOk: () => {
          cancelCollect(this.params.type, id).then(res => {
            if (res.success) {
              this.getList();
            }
          })
        }
      });
    },
    buynow (skuId, goodsId) { // 跳转详情
      let url = this.$router.resolve({
        path: '/goodsDetail',
        query: {skuId, goodsId}
      })
      window.open(url.href, '_blank')
    },
    goShop (id) { // 跳转店铺页面
      let url = this.$router.resolve({
        path: '/merchant',
        query: {id}
      })
      window.open(url.href, '_blank')
    }
  },
  mounted () {
    if (this.homePage) this.params.pageSize = 5;
    this.getList()
  }
};
</script>

<style scoped lang="scss">
.goodsShop,
.goodsImg,
.goodsPrice,
.goodsShop,
.goodsTitle {
  margin: 0 6px;
}
.wrapper {
  margin-bottom: 40px;
}
.goodsItem {
  display: flex;
  height: 60px;
  line-height: 60px;
  margin-bottom: 10px;

  > .goodsImg {
    width: 60px;
    height: 60px;
    overflow: hidden;

    > img {
      width: 100%;
      height: 100%;
    }
  }

  > .goodsPrice,
  .goodsShop {
    width: 150px;
    text-align: center;
  }

  > .goodsTitle {
    width: 400px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.goodsBuy {
  margin-left: 80px;
  >*{
    margin: 0 4px;
  }
}
.page-size{
  text-align: right;
}
</style>
