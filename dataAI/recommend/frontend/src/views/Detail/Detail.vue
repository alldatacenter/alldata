<template>
  <div class="wrapper">
    <div class="clearfix">
      <div class="article">
        <div class="screening">
            
            <div class="info">
              <el-card class="itemdetail">
                <h1>商品详情</h1>
                <img :src="info.imageUrl" />
                <h3>{{info.name}}</h3>
                <el-rate v-model="info.score" :colors="colors" :allow-half="true" @change="doRate(info.score, info.productId)"></el-rate>
                <p>平均评分：{{info.score | numFilter}}</p>
                <p>商品类别：{{info.categories}}</p>
                <p>商品标签：{{info.tags}}</p>
              </el-card>
            </div>
            <div class="screen-hd">
              <h2 title="基于物品的相似推荐(itemCF)">看过该商品的人还看了</h2>
            </div>
            <div class="screen-bd">
              <el-card v-for="item in itemcf" :key="item.productId * 10" class="itemcard">
                <h5 class="name">{{item.name}}</h5>
                <router-link :to="{path: '/detail', query: {productId: item.productId} }" class="a-name">
                  <img :src="item.imageUrl" alt="商品图片" class="itemimage" />
                  <el-rate v-model="item.score" class="itemerate" :colors="colors" :allow-half="true" @change="doRate(item.score, item.productId)"></el-rate>
                </router-link>
              </el-card>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script src="./Detail.ts" lang="ts" />

<style scoped lang="stylus">
@import url('./Detail.stylus');
</style>
