<template>
  <Modal :mask-closable="false" :value="switched" v-model="switched" title="选择地址" @on-ok="submit" @on-cancel="cancel">
    <div class="flex">
      <Spin size="large" fix v-if="spinShow"></Spin>
      <Tree ref="tree" class="tree" :data="data" expand-node show-checkbox multiple></Tree>
    </div>
  </Modal>
</template>
<script>
import { getAllCity } from "@/api/index";
export default {
  data() {
    return {
      switched: false, // 控制模态框显隐
      spinShow: false, // 加载loading
      data: [], // 地区数据
      selectedWay: [], // 选择的地区
      callBackData: "", // 打开组件的回显数据
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    clear() {  // 引用该组件的父组件会调用
      this.data = [];
      this.selectedWay = [];
      this.init();
    },
    /**
     * 关闭
     */
    cancel() {
      this.switched = false;

      // 关闭的时候所有数据设置成disabled为true
      this.data.forEach((item) => {
        this.$set(item, "disabled", false);
        item.children.forEach((child) => {
          this.$set(child, "disabled", false);
        });
      });
    },

    /**
     * 打开地图选择器
     * @param {val} 回调的数据
     * @param {index} 当前操作的运费模板的索引
     */
    open(val, index) {
      if (val) {
        //已选中的地址
        let checkedData = this.$store.state.shipTemplate;

        let checkData = [];
        let disabledData = checkedData.filter((item, i) => {
          if (i != index) {
            return i != index;
          } else {
            checkData.push(item);
          }
        });
        // 选中
        checkData.forEach((check) => {
          // 循环出已经选中的地址id
          check.areaId.split(",").forEach((ids) => {
            this.data.forEach((item) => {
              // 如果当前省份下市区全部选中则选中该省份
              if (check.selectedAll) {
                check.area.split(",").forEach((area) => {
                  if (area == item.name) {
                    this.$set(item, "checked", true);
                  }
                });
              }

              // 将市区继续循环
              item.children.forEach((child, childIndex) => {
                // 判断当前市区是否是已选中状态
                if (item.checked) {
                  this.$set(child, "checked", true);
                }
                if (child.id == ids) {
                  this.$set(child, "checked", true);
                }
              });
            });
          });
        });

        // 禁用
        disabledData.forEach((dis) => {
          // 循环出已经选中的地址id
          dis.areaId.split(",").forEach((ids) => {
            // 循环出省份
            this.data.forEach((item) => {
              // 如果当前省份下市区全部选中则禁用该省份
              if (dis.selectedAll) {
                dis.area.split(",").forEach((area) => {
                  if (area == item.name) {
                    this.$set(item, "disabled", true);
                  }
                });
              }
              // 将市区继续循环
              item.children.forEach((child, childIndex) => {
                // 判断当前市区是否是已禁用状态
                if (item.disabled) {
                  this.$set(child, "disabled", true);
                } else {
                  if (child.id == ids) {
                    this.$set(child, "disabled", true);
                  }
                }
              });
            });
          });
        });
      }
      this.switched ? (this.switched = true) : (this.switched = true);
    },

    /**
     * 提交并筛选出省市
     */
    submit() {
      // 筛选出省市
      let list = this.$refs.tree.getCheckedAndIndeterminateNodes();

      let sort = [];
      list.forEach((item, i) => {
        item.selectedList = [];
        item.selectedAll = false;
        // 筛选出当前的省份
        if (item.level == "province" && !item.disabled) {
          sort.push({
            ...item,
          });
        }

        // 筛选出当前选中的市
        sort.forEach((sortItem, sortIndex) => {
          if (
            item.level != "province" &&
            sortItem.id == item.parentId &&
            !item.disabled
          ) {
            sortItem.selectedList.push({
              ...item,
            });
          }
        });
      });

      // 判断如果当前省是否全选
      this.data.forEach((whether) => {
        sort.forEach((item) => {
          // 如果当前省匹配
          if (
            item.id == whether.id &&
            item.selectedList.length == whether.children.length
          ) {
            // 给一个全选子级的标识符
            item.selectedList.forEach((child) => {
              this.$set(child, "selectedAll", true);
            });
            this.$set(item, "selectedAll", true);
          }
        });
      });

      this.$emit("selected", sort);

      this.cancel();
    },
    // 初始化地区数据
    init() {
      getAllCity().then((res) => {
        if (res.result) {
          res.result.forEach((item) => {
            item.children.forEach((child) => {
              child.title = child.name;
            });

            let data = {
              title: item.name,

              ...item,
            };
            this.data.push(data);

            this.selectedWay.push({ name: data.title, id: data.id });
          });
          this.$store.state.regions = this.data;
        }
      });
    },
  },
};
</script>
<style scoped lang="scss">
.flex {
  display: flex;
  position: relative;
}
.tree {
  flex: 2;
}
.form {
  flex: 8;
}
.button-list {
  margin-left: 80px;
  > * {
    margin: 0 4px;
  }
}
/deep/ .ivu-modal-body {
  height: 400px !important;
  overflow: auto;
}
</style>
