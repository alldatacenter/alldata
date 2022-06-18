<template>
  <div class="wrapper">
    <div class="clearfix">
      <div class="article">
        <div class="screening">
        <!-- 实时推荐 -->
          <div class="screen-hd">
            <h2 title="实时推荐">猜你喜欢
              <a @click="doMore(1)" class="more" href="#" v-if="MAX_SHOW_NUM1 == 5">查看更多》</a>
              <a @click="undoMore(1)" class="more" href="#" v-if="MAX_SHOW_NUM1 == 19">收起更多》</a>
            </h2>
          </div>
          <div class="screen-bd">
            <span v-if="stream.length != 0">
              <el-card
                v-for="item in stream.slice(0, MAX_SHOW_NUM1)"
                :key="item.productId + 1"
                class="itemcard">
                <h5 class="name">{{item.name}}</h5>
                <router-link :to="{path: '/detail', query: {productId: item.productId} }" class="a-name">
                <img :src="item.imageUrl" alt="商品图片" class="itemimage" />
                </router-link>
                <el-rate class="itemerate"
                  v-model="item.score"                  
                  :colors="colors"
                  :allow-half="true"
                  @change="doRate(item.score, item.productId)">
                </el-rate>
              </el-card>
            </span>
            <span v-if="stream.length == 0">
            </span>
          </div>

          <!-- 热门商品 -->
          <div class="screen-hd">
            <h2 title="热门商品推荐">热门商品
              <!-- <span v-if="stream.length != 0"> -->
                <a @click="doMore(3)" class="more" href="#" v-if="MAX_SHOW_NUM3 == 5">查看更多》</a>
                <a @click="undoMore(3)" class="more" href="#" v-if="MAX_SHOW_NUM3 == 19">收起更多》</a>
              <!-- </span> -->
            </h2>
          </div>
          <div class="screen-bd">
            <span v-if="hot.length != 0">
              <el-card
                v-for="item in hot.slice(0, MAX_SHOW_NUM3)"
                :key="item.productId + 1"
                class="itemcard">
                <h5 class="name">{{item.name}}</h5>
                <router-link :to="{path: '/detail', query: {productId: item.productId} }" class="a-name">
                <img :src="item.imageUrl" alt="商品图片" class="itemimage" />
                </router-link>
                <el-rate class="itemerate"
                  v-model="item.score"
                  :colors="colors"
                  :allow-half="true"
                  @change="doRate(item.score, item.productId)">
                </el-rate>
              </el-card>
            </span>
          </div>


          <!-- 好评商品 -->
          <div class="screen-hd">
            <h2 title="好评商品推荐">好评商品
              <a @click="doMore(4)" class="more" href="#" v-if="MAX_SHOW_NUM4 == 5">查看更多》</a>
              <a @click="undoMore(4)" class="more" href="#" v-if="MAX_SHOW_NUM4 == 19">收起更多》</a>
            </h2>
          </div>
          <div class="screen-bd">
            <span v-if="rate.length != 0">
              <el-card
                v-for="item in rate.slice(0, MAX_SHOW_NUM4)"
                :key="item.productId + 1"
                class="itemcard">
                <h5 class="name">{{item.name}}</h5>
                <router-link :to="{path: '/detail', query: {productId: item.productId} }" class="a-name">
                <img :src="item.imageUrl" alt="商品图片" class="itemimage" />
                </router-link>
                <el-rate class="itemerate"
                  v-model="item.score"
                  :colors="colors"
                  :allow-half="true"
                  @change="doRate(item.score, item.productId)">
                </el-rate>
              </el-card>
            </span>
          </div>   


          <!-- 实时热门榜单 -->
          <div class="aside">
            <div class="billboard">
              <div class="billboard-hd">
                <h2>实时热门商品
                  <span><a class="more" href="#">更多榜单》</a></span>
                </h2>
              </div>
              <div class="billboard-bd">
                <table>
                  <tbody v-for="item in onlineHot" :key="item.productId + 1">
                    <tr>
                      <td class="order">{{item.score}}</td>
                      <td class="title">
                        <router-link :to="{path: '/detail', query: {productId: item.productId} }" class="a-name">
                          <a class="itemname">{{item.name}}</a>
                        </router-link>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          
        </div>
      </div>
    </div>
  </div>
</template>

<script src="./Home.ts" lang="ts" />

<style scoped lang="stylus">
@import url('./Home.stylus');
</style>