<template>
  <div>
    <Card>
      <Form ref="form" :model="form" :label-width="120" :rules="formRule">
        <div class="base-info-item">
          <h4>基本信息</h4>
          <div class="form-item-view">
            <FormItem label="活动名称" prop="promotionName">
              <Input
                type="text"
                v-model="form.promotionName"
                placeholder="请填写活动名称"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="报名截止时间" prop="applyEndTime">
              <DatePicker
                type="datetime"
                v-model="form.applyEndTime"
                format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择"
                clearable
                style="width: 200px"
              >
              </DatePicker>
            </FormItem>
            <FormItem label="活动开始时间" prop="startTime">
              <DatePicker
                type="datetime"
                v-model="form.startTime"
                format="yyyy-MM-dd"
                placeholder="请选择"
                clearable
                style="width: 200px"
              >
              </DatePicker>
            </FormItem>
            <FormItem label="抢购时间段" prop="seckillPeriod">
              <Tag
                v-for="item in form.seckillPeriod"
                :key="item"
                :name="item"
                closable
                style="marrgin-left: 10px"
                @on-close="removePeriodTime"
                >{{ item >= 10 ? item : "0" + item }}:00</Tag
              >
              <InputNumber
                :max="23"
                :min="0"
                v-model="periodTime"
                v-show="showAddPeriod"
                @on-blur="addPeriodTime"
              ></InputNumber>
              <Button type="default" @click="addPeriod">添加时间段</Button>
            </FormItem>
            <FormItem label="申请规则" prop="seckillRule">
              <Input
                type="text"
                v-model="form.seckillRule"
                placeholder="申请规则"
                clearable
                style="width: 260px"
              />
            </FormItem>
          </div>
          <div class="foot-btn">
            <Button @click="closeCurrentPage" style="margin-right: 5px">返回</Button>
            <Button type="primary" :loading="submitLoading" @click="handleSubmit"
              >提交</Button
            >
          </div>
        </div>
      </Form>
    </Card>
  </div>
</template>

<script>
import { updateSeckill, seckillDetail } from "@/api/promotion";

export default {
  name: "addSeckill",
  data() {
    return {
      form: {
        /** 活动名称 */
        promotionName: "",
        /** 报名截止时间 */
        applyEndTime: "",
        /** 活动开始时间 */
        startTime: "",
        /** 抢购时间段 */
        seckillPeriod: [],
        /** 申请规则 */
        seckillRule: "",
        promotionStatus: "NEW",
      },
      id: this.$route.query.id, // 活动id
      periodTime: null, // 抢购时间段
      showAddPeriod: false, // input显隐
      submitLoading: false, // 添加或编辑提交状态

      formRule: {
        promotionName: [{ required: true, message: "请填写活动名称" }],
        applyEndTime: [{ required: true, message: "请填写报名截止时间" }],
        seckillPeriod: [{ required: true, message: "请填写抢购时间段" }],
        startTime: [{ required: true, message: "请填写活动开始时间" }],
        seckillRule: [{ required: true, message: "请输入申请规则" }],
      },
    };
  },
  mounted() {
    // 如果id不为空则查询信息
    if (this.id) {
      this.getData();
    }
  },
  methods: {
    // 关闭当前页面
    closeCurrentPage() {
      this.$store.commit("removeTag", "manager-seckill-add");
      localStorage.pageOpenedList = JSON.stringify(this.$store.state.app.pageOpenedList);
      this.$router.go(-1);
    },
    // 获取详情数据
    getData() {
      seckillDetail(this.id).then((res) => {
        if (res.success) {
          let data = res.result;
          data.seckillPeriod = res.result.hours.split(",");
          this.form = data;
        }
      });
    },
    addPeriod() {
      // 添加时间段显示input
      this.addPeriodTime();
      this.showAddPeriod = true;
    },
    addPeriodTime() {
      // 添加秒杀时间段
      this.showAddPeriod = false;
      if (
        this.periodTime !== null &&
        !this.form.seckillPeriod.includes(this.periodTime)
      ) {
        this.form.seckillPeriod.push(this.periodTime);
      }
    },
    removePeriodTime(event, name) {
      // 移除秒杀时间段
      this.form.seckillPeriod = this.form.seckillPeriod.filter((i) => i !== name);
    },
    // // 申请截止时间格式化
    // applyTimeChange (time) {
    //   console.log(time);
    //   this.form.applyEndTime = time
    // },
    // // 开始时间格式化
    // startTimeChange (time) {
    //   this.form.startTime = time
    // },
    /** 添加秒杀活动 */
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          this.form.hours = this.form.seckillPeriod.toString();
          delete this.form.createTime;
          delete this.form.updateTime;
          delete this.form.endTime;
          delete this.form.seckillApplyList;
          let params = this.form;
          params.startTime = this.$options.filters.unixToDate(this.form.startTime / 1000);
          params.applyEndTime = this.$options.filters.unixToDate(
            this.form.applyEndTime / 1000
          );
          // 编辑
          updateSeckill(params).then((res) => {
            this.submitLoading = false;
            if (res && res.success) {
              this.$Message.success("编辑成功");
              this.closeCurrentPage();
            }
          });
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
h4 {
  margin-bottom: 10px;
  padding: 0 10px;
  border: 1px solid #ddd;
  background-color: #f8f8f8;
  font-weight: bold;
  color: #333;
  font-size: 14px;
  line-height: 40px;
  text-align: left;
}
.ivu-form-item {
  margin-bottom: 30px;
}
</style>
