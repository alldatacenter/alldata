<template>
  <div class="item-class-show">
    <div class="head-bar">
      <!-- 有商品分类展示商品分类 -->
      <template v-if="$route.query.categoryId">
        <!-- 头部展示筛选信息 -->
        <div @click="cateClick(tabBar,1)">{{ tabBar.name }}</div>
        <Icon type="ios-arrow-forward" />

        <div class="bar" v-if="tabBar.first">
          {{ tabBar.first.name }} <Icon type="ios-arrow-down" />
          <ul>
            <li v-for="item in tabBar.children" :key="item.id"  @click="cateClick(item,2)">
              {{ item.name }}
            </li>
          </ul>
        </div>
        <Icon type="ios-arrow-forward" v-if="tabBar.first" />

        <div class="bar" v-if="tabBar.second">
          {{ tabBar.second.name }} <Icon type="ios-arrow-down" />
          <ul>
            <li v-for="item in tabBar.first.children" :key="item.id"  @click="cateClick(item,3)">
              {{ item.name }}
            </li>
          </ul>
        </div>
        <Icon type="ios-arrow-forward" v-if="tabBar.second" />
      </template>
      <!-- 无商品分类，展示搜索结果 -->
      <template v-else>
        <div style="font-size:14px">全部结果</div>
        <Icon type="ios-arrow-forward" />
        <div style="font-weight:bold;" class="mr_10">“{{params.keyword}}”</div>
      </template>
      <!-- 所选分类 -->
      <a
        class="selected-item"
        @click="cancelSelected(item, index)"
        v-for="(item, index) in selectedItem"
        :key="index"
        :title="item.name"
      >
        <span>{{ item.type }}：</span><span>{{ item.name }}</span><Icon type="md-close" />
      </a>
    </div>

    <!-- 筛选主体 -->
    <div class="content">
      <!-- 品牌， 有图片，独立出来 -->
      <div class="brand" v-show="tagsContent[0].show && tagsContent[0].values.length">
        <div>
          <strong>{{ tagsContent[0].key }}：</strong>
        </div>
        <div>
          <ul :class="{ 'show-more': tagsContent[0].more }">
            <li
              @click="selectBrand(item.name, 0)"
              :class="{ 'border-color': multSelected.includes(item) }"
              v-for="(item, index) in tagsContent[0].values"
              :key="index"
            >
              <img :src="item.url" alt="" /><span>{{ item.name }}</span>
              <div
                class="corner-icon"
                v-show="multSelected.includes(item.name)"
              >
                <div></div>
                <Icon type="md-checkmark" />
              </div>
            </li>
          </ul>

          <div class="btn" v-show="multiple !== 0">
            <span @click="moreBrand(0)"
              >{{ tagsContent[0].more ? "收起" : "更多"
              }}<Icon
                :type="tagsContent[0].more ? 'ios-arrow-up' : 'ios-arrow-down'"
            /></span>
            <span @click="multSelectBrand(0)"><Icon type="md-add" />多选</span>
          </div>

          <div class="multBtn" v-show="multiple === 0">
            <Button
              type="primary"
              size="small"
              :disabled="!multSelected.length"
              @click="sure(0)"
              >确定</Button
            >
            <Button size="small" @click="cancel">取消</Button>
          </div>
        </div>
      </div>

      <!-- 其他筛选项 -->
      <template v-for="(tag, tagIndex) in tagsContent">
        <div class="other" v-if="tag.show && tagIndex !== 0" v-show="tagIndex < showTagCount" :key="tagIndex">
          <div>
            <strong>{{ tag.key }}：</strong>
          </div>
          <div>
            <ul
              :class="{ 'show-more': tag.more }"
              class="list"
              v-show="multiple !== tagIndex"
            >
              <li
                @click="selectBrand(item, tagIndex)"
                class="item"
                v-for="(item, index) in tag.values"
                :key="index"
              >
                {{ item }}
              </li>
            </ul>

            <CheckboxGroup
              :class="{ 'show-more': tag.more }"
              class="list"
              v-model="multSelected"
              v-show="multiple === tagIndex"
            >
              <Checkbox
                class="item"
                :label="item"
                v-for="(item, index) in tag.values"
                :key="index"
                >{{ item }}</Checkbox
              >
            </CheckboxGroup>

            <div class="btn" v-show="multiple !== tagIndex">
              <span @click="moreBrand(tagIndex)" v-show="tag.values.length > 9"
                >{{ tag.more ? "收起" : "更多"
                }}<Icon :type="tag.more ? 'ios-arrow-up' : 'ios-arrow-down'"
              /></span>
              <span @click="multSelectBrand(tagIndex)"
                ><Icon type="md-add" />多选</span
              >
            </div>

            <div class="multBtn" v-show="multiple === tagIndex">
              <Button
                type="primary"
                size="small"
                :disabled="!multSelected.length"
                @click="sure(tagIndex)"
                >确定</Button
              >
              <Button size="small" @click="cancel">取消</Button>
            </div>
          </div>
        </div>
      </template>
      <div @click="moreOptions" v-if="tagsContent.length>4" class="more-options">{{showTagCount===5?'更多筛选项':'收起筛选项'}}<Icon :type="showTagCount===5?'ios-arrow-down':'ios-arrow-up'" /></div>
    </div>
  </div>
</template>

<script>
import * as APIGoods from '@/api/goods';
export default {
  name: 'GoodsClassNav',
  data () {
    return {
      tabBar: { // 分类数据
        name: '',
        first: {},
        second: {}
      },
      showTagCount: 5, // 展示的搜索项数量
      multiple: false, // 多选
      tagsContent: [
        // 标签
        {
          key: '品牌',
          more: false,
          show: true,
          values: []
        }
      ],
      multSelected: [], // 多选分类
      selectedItem: [], // 已选分类集合 顶部展示
      brandIds: [], // 品牌id合集
      params: {} // 请求参数
    };
  },
  computed: {
    cateList () { // 商品分类
      return this.$store.state.category || []
    }
  },
  watch: {
    selectedItem: {
      // 监听已选条件，来调用列表接口
      handler (val) {
        let classification = [];
        this.params.brandId = ''
        this.params.prop = ''
        val.forEach((item) => {
          if (item.type === '品牌') {
            this.params.brandId = this.brandIds.join('@');
          } else {
            const nameArr = item.name.split('、');
            nameArr.forEach((name) => {
              classification.push(item.type + '_' + name);
            });
          }
        });
        this.params.prop = classification.join('@');
        this.getFilterList(this.params);
        this.$emit('getParams', this.params);
      },
      deep: true
    },
    '$route': { // 监听路由
      handler (val, oVal) {
        if (this.$route.query.categoryId) {
          let cateId = this.$route.query.categoryId.split(',')
          Object.assign(this.params, this.$route.query)
          this.params.categoryId = cateId[cateId.length - 1]
        } else {
          Object.assign(this.params, this.$route.query)
        }
        this.getFilterList(this.params)
        this.getNav()
      },
      deep: true
    }
  },
  methods: {
    getNav () { // 获取商品分类，分类下展示
      if (!this.$route.query.categoryId) return
      if (!this.cateList.length) { // 商品分类存储在localstorage，接口未调用成功前再次刷新数据
        setTimeout(() => {
          this.getNav()
        }, 500)
        return
      }
      const arr = this.$route.query.categoryId.split(',')
      if (arr.length > 0) {
        this.tabBar = this.cateList.filter(e => {
          return e.id === arr[0]
        })[0]
      }
      if (arr.length > 1) {
        const first = this.tabBar.children.filter(e => {
          return e.id === arr[1]
        })[0]
        this.$set(this.tabBar, 'first', first)
      }
      if (arr.length > 2) {
        const second = this.tabBar.first.children.filter(e => {
          return e.id === arr[2]
        })[0]
        this.$set(this.tabBar, 'second', second)
      }
    },
    cateClick (item, index) { // 点选分类
      switch (index) {
        case 1:
          this.$router.push({
            path: '/goodsList',
            query: {categoryId: item.id}
          })
          break;
        case 2:
          this.$router.push({
            path: '/goodsList',
            query: {categoryId: [item.parentId, item.id].toString()}
          })
          break;
        case 3:
          this.$router.push({
            path: '/goodsList',
            query: {categoryId: [this.tabBar.id, item.parentId, item.id].toString()}
          })
          break;
      }
    },
    selectBrand (item, index) {
      // 选择筛选项
      if (this.multiple !== false) {
        // 非多选直接在顶部栏展示，多选则添加选择状态

        let key = this.multSelected.indexOf(item);
        if (key > -1) {
          this.multSelected.splice(key, 1);
        } else {
          this.multSelected.push(item);
        }
      } else {
        this.selectedItem.push({
          type: this.tagsContent[index].key,
          name: item
        });

        this.tagsContent[index].show = false;

        if (index === 0) {
          // 如果是品牌，获取品牌id

          let brands = this.tagsContent[0].values;

          brands.forEach((val) => {
            if (val.name === item) this.brandIds.push(val.value);
          });
        }
      }
    },

    cancelSelected (item, index) {
      // 顶部栏 取消已选中的项
      this.selectedItem.splice(index, 1);

      this.tagsContent.forEach((tag, index) => {
        if (tag.key === item.type) {
          tag.show = true;
          tag.more = false;
        }
      });
      if (item.type === '品牌') {
        this.brandIds = [];
      }
    },
    moreBrand (index) {
      // 更多按钮
      const flag = !this.tagsContent[index].more
      this.$set(this.tagsContent[index], 'more', flag)
    },
    multSelectBrand (index) {
      // 多选按钮
      this.$set(this.tagsContent[index], 'more', true)
      this.multiple = index;
    },
    sure (index) {
      // 多选确认按钮
      this.selectedItem.push({
        type: this.tagsContent[index].key,
        name: this.multSelected.join('、')
      });

      if (index === 0) {
        // 如果是品牌，获取品牌id

        let brands = this.tagsContent[0].values;

        brands.forEach((val) => {
          if (this.multSelected.includes(val.name)) this.brandIds.push(val.value);
        });
      }

      this.tagsContent[index].show = false;
      this.cancel();
    },
    cancel () {
      // 多选取消按钮
      this.multSelected = [];
      this.tagsContent[0].more = false;
      this.multiple = false;
    },
    getFilterList (params) {
      // 筛选、分类  列表
      APIGoods.filterList(params).then((res) => {
        if (res.success) {
          const data = res.result;
          this.tagsContent = [{
            key: '品牌',
            more: false,
            show: true,
            values: []
          }]
          this.tagsContent[0].values = data.brands;
          this.tagsContent = this.tagsContent.concat(data.paramOptions);
          this.tagsContent.forEach((item) => {
            this.$set(item, 'show', true)
            this.$set(item, 'more', false)
          });
        }
      });
    },
    // 展示更多搜索项
    moreOptions () {
      this.showTagCount = this.showTagCount === 5 ? 100 : 5
    }
  },
  mounted () {
    // 有分类id就根据id搜索
    if (this.$route.query.categoryId) {
      let cateId = this.$route.query.categoryId.split(',')
      Object.assign(this.params, this.$route.query)
      this.params.categoryId = cateId[cateId.length - 1]
    } else {
      Object.assign(this.params, this.$route.query)
    }
    this.getFilterList(this.params);
    this.getNav();
  }
}
</script>

<style scoped lang="scss">
/** 头部展示筛选项 */
.head-bar {
  width: 100%;
  background: #fff;
  margin-top: -13px;
  display: flex;
  height: 40px;
  align-items: center;
  > div:first-child {
    padding: 0 8px;
    font-size: 18px;
    font-weight: bold;
    &:hover {
      color: $theme_color;
      cursor: pointer;
    }
  }
  .bar {
    font-size: 12px;
    position: relative;
    background: #fff;
    border: 1px solid #999;
    padding: 0 8px;
    min-width: 85px;
    text-align: center;
    margin: 0 3px;
    &:hover {
      color: $theme_color;
      border-color: $theme_color;
      border-bottom-color: #fff;
      cursor: pointer;
      ul {
        display: block;
      }
      .ivu-icon {
        transform: rotate(180deg);
      }
    }
    ul {
      display: none;
      position: absolute;
      top: 18px;
      left: -1px;
      width: 300px;
      padding: 5px 10px;
      background: #fff;
      border: 1px solid $theme_color;
      z-index: 1;
      &::before {
        content: "";
        position: absolute;
        width: 83px;
        left: 0;
        top: -1px;
        z-index: 2;
        border-top: 1px solid #fff;
      }

      &:hover {
        display: block;
      }
      clear: left;
      li {
        color: #999;
        float: left;
        width: 30%;
        margin: 3px 0;
        text-align: left;
        &:hover {
          color: $theme_color;
          cursor: pointer;
        }
      }
    }
  }
  //所选分类
  .selected-item {
    font-size: 12px;
    color: #000;
    padding: 2px 22px 2px 8px;
    margin-right: 5px;
    max-width: 250px;
    height: 24px;
    overflow: hidden;
    position: relative;
    background-color: #f3f3f3;
    border: 1px solid #ddd;
    &:hover {
      border-color: $theme_color;
      background-color: #fff;
      .ivu-icon {
        color: #fff;
        background-color: $theme_color;
      }
    }

    span:nth-child(2) {
      color: $theme_color;
    }

    .ivu-icon {
      position: absolute;
      right: 0;
      top: 0;
      color: $theme_color;
      line-height: 22px;
      width: 21px;
      height: 22px;
      font-size: 14px;
    }
  }
}
/** 筛选主体 */
.content {
  background: #fff;
  border-top: 1px solid #ddd;
  border-bottom: 1px solid #ddd;
  margin: 10px 0;
}
/** 品牌 start */
.brand {
  border-bottom: 1px solid #ddd;
  display: flex;
  // min-height: 120px;
  font-size: 12px;
  > div:first-child {
    width: 100px;
    background: #eee;
    padding: 10px 0 0 10px;
  }
  > div:last-child {
    width: 1100px;
    padding: 10px;
    position: relative;
    ul {
      width: 900px;
      max-height: 100px;
      overflow: hidden;
      padding-top: 1px;
      clear: left;
      li {
        width: 100px;
        height: 50px;
        float: left;
        line-height: 45px;
        border: 1px solid #ddd;
        margin: -1px -1px 0 0;
        overflow: hidden;
        position: relative;
        padding: 2px;
        img {
          width: 100%;
          height: 100%;
        }

        &:hover {
          border-color: $theme_color;
          border: 2px solid $theme_color;
          top: 0;
          left: 0;
          position: relative;
          z-index: 1;
          img {
            display: none;
          }
        }

        span {
          display: inline-block;
          width: 100%;
          height: 100%;
          color: $theme_color;
          text-align: center;
          font-size: 12px;
          cursor: pointer;
        }

        .corner-icon {
          position: absolute;
          right: -1px;
          bottom: -1px;
          div {
            width: 0;
            border-top: 20px solid transparent;
            border-right: 20px solid $theme_color;
          }
          .ivu-icon {
            font-size: 12px;
            position: absolute;
            bottom: 0;
            right: 1px;
            transform: rotate(-15deg);
            color: #fff;
          }
        }
      }
      .border-color {
        border-color: $theme_color;
        z-index: 1;
      }
    }
    .show-more {
      height: auto;
      max-height: 200px;
      overflow: scroll;
    }
    .btn {
      position: absolute;
      right: 10px;
      top: 10px;
      span {
        border: 1px solid #ddd;
        margin-left: 10px;
        color: #999;
        display: inline-block;
        padding: 1px 3px;
        font-size: 12px;
        &:hover {
          cursor: pointer;
          color: $theme_color;
          border-color: $theme_color;
        }
      }
    }
    .multBtn {
      text-align: center;
      margin-top: 10px;
      .ivu-btn {
        font-size: 12px !important;
      }
      .ivu-btn:last-child {
        margin-left: 10px;
      }
    }
  }
}
/** 品牌 end */
/** 其他筛选项  start */
.other {
  border-bottom: 1px solid #ddd;
  display: flex;
  min-height: 30px;
  font-size: 12px;
  &:last-child {
    border: none;
  }
  > div:first-child {
    width: 100px;
    background: #eee;
    padding-left: 10px;
    line-height: 30px;
  }
  > div:last-child {
    width: 1100px;
    padding: 0 10px;
    position: relative;
    .list {
      width: 900px;
      height: 30px;
      overflow: hidden;
      clear: left;
      .item {
        width: 100px;
        height: 30px;
        float: left;
        line-height: 30px;
        color:#4d9cf1;
        overflow: hidden;
        position: relative;
        font-size: 12px;
        padding: 2px;
        cursor: pointer;
        &:hover {
          color: $theme_color;
        }
      }
    }

    .show-more {
      height: auto;
    }

    .btn {
      position: absolute;
      right: 10px;
      top: 5px;
      span {
        border: 1px solid #ddd;
        margin-left: 10px;
        color: #999;
        display: inline-block;
        padding: 1px 3px;
        font-size: 12px;
        &:hover {
          cursor: pointer;
          color: $theme_color;
          border-color: $theme_color;
        }
      }
    }

    .multBtn {
      text-align: center;
      margin-top: 10px;
      margin-bottom: 10px;
      .ivu-btn {
        font-size: 12px !important;
      }
      .ivu-btn:last-child {
        margin-left: 10px;
      }
    }
  }
}
.more-options{
  margin: 5px;
  color: #4d9cf1;
  font-size: 12px;
  cursor: pointer;
  text-align: right;
}
.more-options:hover{
  color:#0165d1;
}
/** 其他筛选项  end */
</style>
