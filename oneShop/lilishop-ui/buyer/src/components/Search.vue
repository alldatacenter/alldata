<template>
  <div>
    <div class="container">
      <img
        :src="$store.state.logoImg"
        v-if="showLogo"
        class="logo-img"
        alt=""
        @click="$router.push('/')"
      />
      <i-input
        v-model="searchData"
        size="large"
        class="search"
        placeholder="输入你想查找的商品"
        @keyup.enter.native="search"
      >
        <Button v-if="!store"  slot="append" @click="search">搜索</Button>
      </i-input>
      <div v-if="store" class="btn-div">
        <Button class="store-search" type="warning" @click="searchStore">搜本店</Button>
        <Button class="store-search" type="primary" @click="search">搜全站</Button>
      </div>
      <template v-if="showTag">
        <div style="height:12px" v-if="promotionTags.length === 0"></div>
        <div v-else>
          <Tag
            v-for="(item, index) in promotionTags"
            :key="index"
            class="mr_10"
          >
            <span class="hover-color" @click="selectTags(item)">{{ item }}</span>
          </Tag>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import storage from '@/plugins/storage.js'
import {hotWords} from '@/api/goods.js'
export default {
  name: 'search',
  props: {
    showTag: { // 是否展示搜索栏下方热门搜索
      type: Boolean,
      default: true
    },
    showLogo: { // 是否展示左侧logo
      type: Boolean,
      default: true
    },
    store: { // 是否为店铺页面
      type: Boolean,
      default: false
    },
    hover: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      searchData: '' // 搜索内容
    };
  },
  methods: {
    selectTags (item) { // 选择热门标签
      this.searchData = item;
      this.search();
    },
    search () { // 全平台搜索商品
      this.$router.push({
        path: '/goodsList',
        query: { keyword: this.searchData }
      });
    },
    searchStore () { // 店铺搜索商品
      this.$emit('search', this.searchData)
    }
  },
  computed: {
    promotionTags () {
      if (this.$store.state.hotWordsList) {
        return JSON.parse(this.$store.state.hotWordsList)
      } else {
        return []
      }
    }
  },
  created () {
    this.searchData = this.$route.query.keyword
    if (!this.hover) { // 首页顶部固定搜索栏不调用热词接口
      // 搜索热词每5分钟请求一次
      const reloadTime = storage.getItem('hotWordsReloadTime')
      const time = new Date().getTime() - 5 * 60 * 1000
      if (!reloadTime) {
        hotWords({count: 5}).then(res => {
          if (res.success && res.result) storage.setItem('hotWordsList', res.result)
        })
        storage.setItem('hotWordsReloadTime', new Date().getTime())
      } else if (reloadTime && time > reloadTime) {
        hotWords({count: 5}).then(res => {
          if (res.success && res.result) storage.setItem('hotWordsList', res.result)
        })
        storage.setItem('hotWordsReloadTime', new Date().getTime())
      }
    }
  }
};
</script>
<style scoped lang="scss">
.container {
  margin: 30px auto;
  width: 460px;
  position: relative;
}
.search {
  margin: 10px 0px 5px 0;
  /deep/ .ivu-input.ivu-input-large {
    border: 2px solid $theme_color;
    font-size: 12px;
    height: 34px;
    &:focus {
      box-shadow: none;
    }
  }
  /deep/ .ivu-input-group-append {
    border: 1px solid $theme_color;
    border-left: none;
    height: 30px;
    background-color: $theme_color;
    color: #ffffff;
    button {
      font-size: 14px;
      font-weight: 600;
      line-height: 1;
    }
  }
}
.logo-img {
  position: absolute;
  left: -360px;
  top: -9px;
  max-width: 150px;
  cursor: pointer;
}
.store-search{
  width:55.6px;
  padding: 0 9px;
  border-radius: 0;
  border-radius: 3px;
  &:nth-child(2){
    width:55px;
    margin-left: -2px;
    border-radius: 3px;
  }
}
.btn-div{
  position: relative;
  height: 0px;
  top: -38px;
  left: 352px;
}
</style>
