<template>
  <div>
    <div class="breadcrumb">
      <span @click="clickBreadcrumb(item,index)" :class="{'active':item.selected}" v-for="(item,index) in dateList"
        :key="index"> {{item.title}}</span>
      <div class="date-picker">
        <Select @on-change="changeSelect(selectedWay)" v-model="month" placeholder="年月查询" clearable
          style="width:200px;margin-left:10px;">
          <Option v-for="(item,index) in dates" :value="item.year+'-'+item.month" :key="index" clearable>
            {{ item.year+'年'+item.month+'月' }}</Option>
        </Select>
      </div>
      <div class="shop-list" v-if="!closeShop">
        <Select clearable @on-change="changeshop(selectedWay)" v-model="storeId" placeholder="店铺查询"
          style="width:200px;margin-left:10px;">
          <Scroll :on-reach-bottom="handleReachBottom">
            <Option v-for="(item,index) in shopsData" :value="item.id" :key="index">{{ item.storeName }}</Option>
          </Scroll>
        </Select>
      </div>
    </div>
  </div>
</template>
<script>
import { getShopListData } from "@/api/shops.js";
export default {
  props: ["closeShop"],
  data() {
    return {
      month: "", // 月份

      selectedWay: {
        // 可选时间项
        title: "最近7天",
        selected: true,
        searchType: "LAST_SEVEN",
      },
      storeId: "", // 店铺id
      dates: [], // 日期列表
      params: {
        // 请求参数
        pageNumber: 1,
        pageSize: 10,
        storeName: "",
      },
      dateList: [
        // 筛选条件
        {
          title: "今天",
          selected: false,
          searchType: "TODAY",
        },
        {
          title: "昨天",
          selected: false,
          searchType: "YESTERDAY",
        },
        {
          title: "最近7天",
          selected: true,
          searchType: "LAST_SEVEN",
        },
        {
          title: "最近30天",
          selected: false,
          searchType: "LAST_THIRTY",
        },
      ],

      shopTotal: "", // 店铺总数
      shopsData: [], // 店铺数据
    };
  },
  mounted() {
    this.getFiveYears();
    this.getShopList();
  },
  methods: {
    // 页面触底
    handleReachBottom() {
      setTimeout(() => {
        if (this.params.pageNumber * this.params.pageSize <= this.shopTotal) {
          this.params.pageNumber++;
          this.getShopList();
        }
      }, 1500);
    },
    // 查询店铺列表
    getShopList() {
      getShopListData(this.params).then((res) => {
        if (res.success) {
          /**
           * 解决数据请求中，滚动栏会一直上下跳动
           */
          this.shopTotal = res.result.total;

          this.shopsData.push(...res.result.records);
        }
      });
    },
    // 变更店铺
    changeshop(val) {
      this.selectedWay.storeId = this.storeId;
      this.$emit("selected", this.selectedWay);
    },

    // 获取近5年 年月
    getFiveYears() {
      let getYear = new Date().getFullYear();

      let lastFiveYear = getYear - 5;
      let maxMonth = new Date().getMonth() + 1;
      let dates = [];
      // 循环出过去5年
      for (let year = lastFiveYear; year <= getYear; year++) {
        for (let month = 1; month <= 12; month++) {
          if (year == getYear && month > maxMonth) {
          } else {
            dates.push({
              year: year,
              month: month,
            });
          }
        }
      }
      this.dates = dates.reverse();
    },
    // 改变已选店铺
    changeSelect() {
      console.log(this.month);
      if (this.month) {
        this.dateList.forEach((res) => {
          res.selected = false;
        });
        this.selectedWay.year = this.month.split("-")[0];
        this.selectedWay.month = this.month.split("-")[1];
        this.selectedWay.searchType = "";

        this.$emit("selected", this.selectedWay);
      } else {
      }
    },
    // 变更时间
    clickBreadcrumb(item) {
      this.dateList.forEach((res) => {
        res.selected = false;
      });
      item.selected = true;
      item.storeId = this.storeId;
      this.month = "";

      if (item.searchType == "") {
        if (
          dateList.some((date) => {
            return date.title == item.title;
          })
        ) {
          item.searchType = date.searchType;
        } else {
          item.searchType = "LAST_SEVEN";
        }
      }

      this.selectedWay = item;
      this.selectedWay.year = new Date().getFullYear();
      this.selectedWay.month = "";

      this.$emit("selected", this.selectedWay);
    },
  },
};
</script>
<style lang="scss" scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  > span {
    margin-right: 15px;
    cursor: pointer;
  }
}
.active {
  color: $theme_color;
  position: relative;
}
.date-picker {
}
.active:before {
  content: "";
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 100%;
  height: 3px;
  background: $theme_color;
}
</style>
