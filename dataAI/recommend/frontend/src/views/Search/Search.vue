<template>
  <div class="container">
    <h1>搜索结果</h1>
    <el-card v-for="item in searchResult" :key="item.productId + 1" class="card">
      <router-link :to="{path: '/detail', query: {productId: item.productId} }" class="a-name">
        <h5 class="name">{{item.name}}</h5>
      </router-link>
      <img :src="item.imageUrl" alt="商品图片" class="image" />
      <el-rate
        v-model="item.score"
        :colors="colors"
        :allow-half="true"
        @change="doRate(item.score, item.productId)"
      ></el-rate>
    </el-card>
  </div>
</template>

<script src="./Search.ts" lang="ts" />

<style scoped lang="stylus">
@import url('./Search.stylus');
</style>