<template>
  <div>
    <Cascader
      :data="data"
      :load-data="loadData"
      v-model="addr"
      @on-change="change"
      style="width: 300px"
    ></Cascader>
  </div>
</template>
<script>
import {getRegion} from '@/api/common.js';
export default {
  data () {
    return {
      data: [], // 地区数据
      addr: [] // 已选数据
    };
  },
  props: ['addressId'],
  mounted () {},
  methods: {
    change (val, selectedData) { // 选择地区
      /**
       * @returns [regionId,region]
       */
      this.$emit('selected', [
        val,
        selectedData[selectedData.length - 1].__label.split('/')
      ]);
    },
    loadData (item, callback) { // 加载数据
      item.loading = true;
      getRegion(item.value).then((res) => {
        if (res.result.length <= 0) {
          item.loading = false;
        } else {
          res.result.forEach((child) => {
            item.loading = false;

            let data = {
              value: child.id,
              label: child.name,
              loading: false,
              children: []
            };

            if (child.level === 'street' || item.label === '香港特别行政区') {
              item.children.push({
                value: child.id,
                label: child.name
              });
            } else {
              item.children.push(data);
            }
          });
          callback();
        }
      });
    },
    async init () { // 初始化地图数据
      let data = await getRegion(0);
      let arr = [];
      data.result.forEach((item) => {
        let obj;
        // 台湾省做处理
        if (item.name === '台湾省') {
          obj = {
            value: item.id,
            label: item.name
          };
        } else {
          obj = {
            value: item.id,
            label: item.name,
            loading: false,
            children: []
          };
        }
        arr.push(obj);
      });
      this.data = arr;
      console.warn('init');
    },
    async reviewData () {
      // 数据回显
      let addr = JSON.parse(JSON.stringify(this.addressId.split(',')));
      let length = addr.length;
      let data = await getRegion(0);
      let arr0 = [];
      let arr1 = [];
      let arr2 = [];
      // 第一级数据
      data.result.forEach((item) => {
        let obj;
        // 台湾省做处理
        if (item.name === '台湾省') {
          obj = {
            value: item.id,
            label: item.name
          };
        } else {
          obj = {
            value: item.id,
            label: item.name,
            loading: false,
            children: []
          };
        }
        arr0.push(obj);
      });
      // 根据选择的数据来加载数据列表
      if (length > 0) {
        let children = await getRegion(addr[0]);
        children = this.handleData(children.result);
        arr0.forEach((e) => {
          if (e.value === addr[0]) {
            e.children = arr1 = children;
          }
        });
      }
      if (length > 1) {
        let children = await getRegion(addr[1]);
        children = this.handleData(children.result);
        arr1.forEach((e) => {
          if (e.value === addr[1]) {
            e.children = arr2 = children;
          }
        });
      }
      if (length > 2) {
        let children = await getRegion(addr[2]);
        children = this.handleData(children.result);
        arr2.forEach((e) => {
          if (e.value === addr[2]) {
            e.children = children;
          }
        });
      }
      this.data = arr0;
      this.addr = addr;
    },
    handleData (data) {
      // 处理接口数据
      let item = [];
      data.forEach((child) => {
        let obj = {
          value: child.id,
          label: child.name,
          loading: false,
          children: []
        };

        if (child.level === 'street' || item.label === '香港特别行政区') {
          item.push({
            value: child.id,
            label: child.name
          });
        } else {
          item.push(obj);
        }
      });
      return item;
    }
  },
  watch: {
    addressId: {
      handler: function (v) {
        if (v) {
          this.reviewData();
        } else {
          this.init();
        }
      },
      immediate: true
    }
  }
};
</script>
<style scoped lang="scss">
</style>
