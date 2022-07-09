<template>
  <div>
    <Cascader
      :data="data"
      :load-data="loadData"
      change-on-select
      @on-visible-change="handleChangeOnSelect"
      @on-change="change"
    ></Cascader>
  
  </div>
</template>
<script>

import * as API_Setup from "@/api/common.js";
export default {
  data() {
    return {
      dd:"",
      data: [], // 地区数据
      selected: [], // 已选地区
      changeOnSelect: false, // 选择时的变化
      id: 0 // 0层id
    };
  },
  mounted() {
    this.init();
  },

  props: ['addressId'],
  methods: {
    // 选择地区回显
    change(val, selectedData) {
      /**
       * @returns [regionId,region]
       */
      this.$emit("selected", [
        val,
        selectedData[selectedData.length - 1].__label.split("/"),
      ]);
    },
    /**
     * 动态设置change-on-select的值
     * 当级联选择器弹窗展开时，设置change-on-select为true，即可以点选菜单选项值发生变化
     * 当级联选择器弹窗关闭时，设置change-on-select为false，即能够设置初始值
     */
    handleChangeOnSelect(value) {
      this.changeOnSelect = value;
    },
    // 动态加载数据
    loadData(item, callback) {
      item.loading = true;
      API_Setup.getChildRegion(item.value).then((res) => {
        if (res.result.length <= 0) {
          item.loading = false;
          this.selected = item;

          /**
           * 处理数据并返回
           */
        } else {
          res.result.forEach((child) => {
            item.loading = false;

            let data = {
              value: child.id,
              label: child.name,
              loading: false,
              children: [],
            };

            if (
              child.level == "street" ||
              item.label == "香港特别行政区" ||
              item.label == "澳门特别行政区"
            ) {
              item.children.push({
                value: child.id,
                label: child.name,
              });
            } else {
              item.children.push(data);
            }
          });
          this.selected = item;
          callback();
        }
      });
    },
    // 初始化数据
    init() {
      API_Setup.getChildRegion(this.id).then((res) => {
        let way = [];

        res.result.forEach((item) => {
          let data;
          // 台湾省做处理
          if (item.name == "台湾省") {
            data = {
              value: item.id,
              label: item.name,
            };
          } else {
            data = {
              value: item.id,
              label: item.name,
              loading: false,
              children: [],
            };
          }
          way.push(data);
        });

        this.data = way;
      });
    },
  },
};
</script>
