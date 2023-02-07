<template>
  <div class="header">
    <div class="tools">
      <div class="ddnewhead_operate">
        <div class="ddnewhead_hello" >
        </div>
        <div class="ddnewhead_welcome" >
          <a>个人中心</a>
          <el-popover placement="bottom" width="200" trigger="hover">
            <img src="@/assets/userEntity.png" height="20px" width="20px" slot="reference" />
            <p>用户名：{{username}}</p>
            <p>系统状态：在线</p>
            <el-button v-on:click="doLogout">退出登陆</el-button>
          </el-popover>
        </div>
      </div>
      
    </div>
    <div class="searcher">
      <div class="site" v-on:click="goHome" title="回到首页">
        <img src="@/assets/logo.jpg" class="site-img" />
        <div class="title"></div>
      </div>
      <div class="search_container">
          <form class="form-inline my-2 my-lg-0">
            <input class="text" v-model="searchInput" type="query" placeholder="搜索商品" aria-label="Search">
            <button class="button" @click.prevent="doSearch()" type="submit"><i class="fas fa-search"></i></button>
          </form>
        <!-- <form >
          <input class="text" type="text" v-model="searchInput" :clearable="true" placeholder="搜索商品" />
          <input class="button" type="info" plain @click="doSearch()"/>
        </form> -->
      </div>
    </div>
  </div>
</template>

<script src="./Header.ts" lang="ts" />

<style scoped lang="stylus">
@import url('./Header.stylus');
</style>
