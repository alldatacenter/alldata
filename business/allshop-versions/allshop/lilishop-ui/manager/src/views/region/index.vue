<template>
  <Card>
    <Row class="operation">
      <Button
        @click="handleAsyncRegion"
        :loading="asyncLoading"
        type="primary"
        icon="md-add"
        >同步数据</Button
      >
    </Row>
    <div class="flex">
      <Tree
        class="tree"
        :data="data"
        :load-data="loadData"
        @on-select-change="changeTree"
      ></Tree>
      <div class="form">
        <Form
          ref="formValidate"
          :model="formValidate"
          :rules="ruleValidate"
          :label-width="100"
        >
          <FormItem v-if="formValidate.adCode" prop="adCode" label="区域编码">
            <Input v-model="formValidate.adCode" />
          </FormItem>
          <FormItem label="城市代码" prop="cityCode">
            <Input v-model="formValidate.cityCode" />
          </FormItem>
          <FormItem label="行政区划级别" prop="cityCode">
            <RadioGroup
              type="button"
              button-style="solid"
              v-model="formValidate.level"
            >
              <Radio disabled label="country">国家</Radio>
              <Radio disabled label="province">省份</Radio>
              <Radio disabled label="city">市</Radio>
              <Radio disabled label="district">区县</Radio>
              <Radio disabled label="street">街道</Radio>
            </RadioGroup>
          </FormItem>

          <FormItem label="经纬度" prop="center">
            <Input v-model="formValidate.center" />
          </FormItem>
          <FormItem label="名称" prop="name">
            <Input v-model="formValidate.name" />
          </FormItem>
          <FormItem label="排序" prop="orderNum">
            <Input v-model="formValidate.orderNum" />
          </FormItem>

          <div class="button-list">
            <Button type="default" @click="handleUpdate('formValidate')"
              >修改</Button
            >
            <Button type="primary" ghost @click="handleInsert()">添加</Button>
            <Button type="error" @click="handleDel('formValidate')"
              >删除</Button
            >
          </div>
        </Form>
      </div>
    </div>

    <Modal v-model="modalFlag" title="添加" @on-ok="submit">
      <Form ref="addValidate" :model="addValidate" :label-width="100">
        <FormItem label="父级">
          <Input disabled v-model="addValidate.parentName" />
        </FormItem>
        <FormItem prop="adCode" label="区域编码">
          <Input v-model="addValidate.adCode" />
        </FormItem>
        <FormItem label="城市代码" prop="cityCode">
          <Input v-model="addValidate.cityCode" />
        </FormItem>

        <FormItem label="经纬度" prop="center">
          <Input v-model="addValidate.center" />
        </FormItem>
        <FormItem label="名称" prop="name">
          <Input v-model="addValidate.name" />
        </FormItem>
        <FormItem label="排序" prop="orderNum">
          <Input v-model="addValidate.orderNum" />
        </FormItem>
      </Form>
    </Modal>
  </Card>
</template>
<script>
import {
  getChildRegion,
  updateRegion,
  delRegion,
  asyncRegion,
  addRegion,
} from "@/api/index";
export default {
  data() {
    return {
      asyncLoading: false, // 加载状态
      num: 10, // 更新倒计时
      modalFlag: false, // 新增编辑标识
      timerNum: 10, // 定时器
      data: [], // 加载数据
      id: 0, // id
      addValidate: {
        // 添加级别
        parentName: "无父级",
      },
      formValidate: {
        // 表单数据
        adCode: "",
        cityCode: "",
        center: "",
        name: "",
        orderNum: "",
        level: "",
      },
      ruleValidate: {
        // 验证规则
        adCode: [
          {
            required: true,
            message: "区域编码不能为空",
            trigger: "blur",
          },
        ],
        center: [
          {
            required: true,
            message: "经纬度不能为空",
            trigger: "blur",
          },
        ],
        name: [
          {
            required: true,
            message: "名称不能为空",
            trigger: "blur",
          },
        ],
      },
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    // 提交数据
    submit() {
      delete this.addValidate.children;
      addRegion(this.addValidate).then((res) => {
        if (res.success) {
          this.$Message.success("添加成功!请稍后查看");
        }
      });
    },

    // 添加
    handleInsert() {
      if (!this.formValidate.parentId) {
        this.$Message.error("请选择要添加的数据");
        return false;
      }
      this.addValidate = JSON.parse(JSON.stringify(this.formValidate));

      let level = ["country", "province", "city", "district", "street"];

      let child;
      level.forEach((item, index) => {
        if (this.addValidate.level == item) {
          if (index == level.length - 1) {
            child = level[index - 1];
          } else {
            child = level[index + 1];
          }
        }
      });

      this.addValidate.level = child;

      this.addValidate.parentId = this.formValidate.id;
      this.addValidate.parentName = this.formValidate.name;

      delete this.addValidate.id;
      delete this.addValidate.createBy;
      delete this.addValidate.createTime;
      delete this.addValidate.updateBy;
      delete this.addValidate.updateTime;
      delete this.addValidate.selected;

      this.addValidate.name = "";
      this.addValidate.center = "";
      this.modalFlag = true;
    },
    // 删除
    handleDel() {
      this.$Modal.confirm({
        title: "确定删除？",
        content: "删除后店铺以及用户地区绑定数据将全部错乱",
        onOk: () => {
          delRegion(this.formValidate.id).then((res) => {
            if (res.success) {
              this.$Message.success("删除成功!,请稍后查看数据");
            }
          });
        },
      });
    },

    // 修改
    handleUpdate(name) {
      delete this.formValidate.createBy;
      delete this.formValidate.createTime;
      delete this.formValidate.updateBy;
      delete this.formValidate.updateTime;
      delete this.formValidate.selected;
      this.$refs[name].validate((valid) => {
        if (valid) {
          this.formValidate.adCode
            ? this.formValidate.adCode
            : (this.formValidate.adCode = "");
          updateRegion(this.formValidate.id, this.formValidate).then((res) => {
            if (res.result) {
              this.$Message.success("修改成功!,请稍后查看数据");
            }
          });
        } else {
          this.$Message.error("请选择数据");
        }
      });
    },
    // 树结构点击事件
    changeTree(array, val) {
      val.cityCode == "null" ? (val.cityCode = "") : val.cityCode;
      this.$set(this, "formValidate", val);
    },
    // 异步加载数据
    loadData(item, callback) {
      item.loading = true;
      // console.log(item);
      getChildRegion(item.id).then((res) => {
        if (res.result.length <= 0) {
          item.loading = false;
        } else {
          let way = [];
          res.result.forEach((child, index) => {
            item.loading = false;
            let data;
            data = {
              ...child,
              title: child.name,
              loading: false,
              children: [],
            };

            if (
              child.level == "street" ||
              item.label == "香港特别行政区" ||
              item.label == "澳门特别行政区"
            ) {
              data = {
                ...child,
                title: child.name,
              };
            }

            way.push(data);
          });
          callback(way);
        }
      });
    },
    // 初始化数据
    init() {
      getChildRegion(this.id).then((res) => {
        if (res.result) {
          res.result.forEach((item) => {
            let data;

            // 台湾省做处理
            if (item.name == "台湾省") {
              data = {
                ...item,
                title: item.name,
              };
            } else {
              data = {
                ...item,
                title: item.name,
                loading: false,
                children: [],
              };
            }

            this.data.push(data);
          });
        }
      });
    },
    // 同步数据
    handleAsyncRegion() {
      this.num = 10;
      this.$Modal.confirm({
        title: "确定更新？",
        content: "更新后店铺以及用户地区绑定数据将全部错乱",
        onOk: () => {
          let timer;
          let number;

          this.asyncLoading = true;

          this.$Message.info({
            duration: this.timerNum,
            render: (h) => {
              return h("span", [
                `地区数据将在${this.num}秒后更新`,
                h(
                  "a",
                  {
                    on: {
                      click: () => {
                        this.$Message.destroy();
                        this.asyncLoading = false;
                        clearInterval(number);
                        clearTimeout(timer);
                      },
                    },
                  },
                  "点击撤销"
                ),
              ]);
            },
          });

          number = setInterval(() => {
            this.num--;
          }, 1000);

          timer = setTimeout(() => {
            clearInterval(number);
            asyncRegion().then((res) => {
              this.asyncLoading = false;
              this.$Message.loading("地区数据正在更新中！");
            });
          }, 10000);
        },
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
</style>
