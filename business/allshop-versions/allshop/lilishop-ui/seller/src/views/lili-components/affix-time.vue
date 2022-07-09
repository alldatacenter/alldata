<template>
  <div>
    <div class="breadcrumb">
      <span @click="clickBreadcrumb(item,index)" :class="{'active':item.selected}" v-for="(item,index) in dateList" :key="index"> {{item.title}}</span>
      <div class="date-picker">
        <Select @on-change="changeSelect(selectedWay)" v-model="month" placeholder="年月查询" style="width:200px;margin-left:10px;">
          <Option v-for="(item,index) in dates" :value="item.year+'-'+item.month" :key="index">{{ item.year+'年'+item.month+'月' }}</Option>
        </Select>
      </div>

    </div>
  </div>
</template>
<script>
import Cookies from "js-cookie";
export default {
  props: ["closeShop"],
  data() {
    return {
      month: "", // 所选月份

      defuaultWay: {
        title: "最近7天",
        selected: true,
        searchType: "LAST_SEVEN",
      },

      selectedWay: {
        title: "最近7天",
        selected: true,
        searchType: "LAST_SEVEN",
      },
      storeId: "", // 店铺id
      dates: [], // 日期列表
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 10,
        storeName: "",
        storeId: "",
      },

      dateList: [
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
    };
  },
  mounted() {
   this.storeId = JSON.parse(Cookies.get("userInfoSeller")).id;
    this.getFiveYears();
  },
  methods: {
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
    // 选择回调
    changeSelect() {
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
    // 点击时间筛选
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
