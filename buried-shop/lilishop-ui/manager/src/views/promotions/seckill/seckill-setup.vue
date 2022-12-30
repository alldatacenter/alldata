<template>
  <div v-if="templateShow">
    <Form :model="form" :label-width="120">
      <FormItem label="每日场次设置">
        <Row :gutter="16" class="row">
          <Col class="time-item" @click.native="handleClickTime(item,index)" v-for="(item,index) in this.times" :key="index" span="3">
          <div class="time" :class="{'active':item.check}">{{item.time}}:00</div>
          </Col>
        </Row>
      </FormItem>
      <FormItem label="秒杀规则">
        <Input type="textarea" :autosize="{minRows: 4,}" v-model="form.seckillRule" placeholder="申请规则" clearable style="width: 360px; margin-left:10px" />
      </FormItem>
      <FormItem>
        <div class="foot-btn">
          <Button @click="closeCurrentPage" style="margin-right: 5px">返回</Button>
          <Button type="primary" :loading="submitLoading" @click="handleSubmit">提交</Button>
        </div>
      </FormItem>
    </Form>
  </div>
</template>

<script>
import { getSetting, setSetting } from "@/api/index";
export default {
  data() {
    return {
      templateShow:false, // 设置是否显示
      submitLoading: false,
      times: [], //时间集合 1-24点
      form: {
        seckillRule: "",
      },
    };
  },
  mounted() {
    /**
     * 初始化
     */
    this.init();
  },
  methods: {
    /**
     * 关闭当前页面
     */
    closeCurrentPage() {
      this.$store.commit("removeTag", "manager-seckill-add");
      localStorage.pageOpenedList = JSON.stringify(
        this.$store.state.app.pageOpenedList
      );
      this.$router.go(-1);
    },
    /**
     * 提交秒杀信息
     */
    async handleSubmit() {
      let hours = this.times
        .filter((item) => {
          return item.check;
        })
        .map((item) => {
          return item.time;
        })
        .join(",");

      let result = await setSetting("SECKILL_SETTING", {
        seckillRule: this.form.seckillRule,
        hours,
      });
      if (result.success) {
        this.$Message.success("设置成功!");
        this.init();
      }
    },

    /**
     * 初始化当前信息
     */
    async init() {
      let result = await getSetting("SECKILL_SETTING");
      if (result.success) {
        this.templateShow = true
        this.form.seckillRule = result.result.seckillRule;
        this.times=[]
        for (let i = 0; i < 24; i++) {
          // 将数据拆出
          if (result.result.hours) {
            let way = result.result.hours.split(",");
            way.forEach((hours) => {
              if (hours == i) {
                this.times.push({
                  time: i,
                  check: true,
                });
              }
            });
          }
          if (!this.times[i]) {
            this.times.push({
              time: i,
              check: false,
            });
          }
        }
      }
    },
    /**
     * 选中时间
     */
    handleClickTime(val, index) {
      val.check = !val.check;
    },
  },
};
</script>

<style scoped lang="scss">
.row {
  width: 50%;
}
.foot-btn {
  margin-left: 10px;
}
.time-list {
  display: flex;
  flex-wrap: wrap;
}
.active {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
  color: #fff;
  background: $theme_color !important;
}
.time {
  width: 100%;
  cursor: pointer;
  transition: 0.35s;
  border-radius: 0.8em;
  justify-content: center;
  align-items: center;
  display: flex;
  background: #f3f5f7;
  height: 100%;
}
.time-item {
  height: 50px;
  margin: 8px 0;
  font-size: 15px;
}
</style>
