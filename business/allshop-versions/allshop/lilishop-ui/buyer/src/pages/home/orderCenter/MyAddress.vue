<template>
  <div>
    <card _Title="收货地址" _More="添加新地址" _Src="/home/addAddress"></card>
    <div class="address-box" v-for="(item, index) in list" :key="index">
      <div class="address-header">
        <span>
          {{ item.name }}
          <Tag class="ml_10" v-if="item.isDefault" color="red">默认地址</Tag>
          <Tag class="ml_10" v-if="item.alias" color="warning">{{item.alias}}</Tag>
        </span>
        <div class="address-action">
          <span @click="edit(item.id)"><Icon type="edit"></Icon>修改</span>
          <span @click="del(item.id)"><Icon type="trash-a"></Icon>删除</span>
        </div>
      </div>
      <div class="address-content">
        <p>
          <span class="address-content-title"> 收 货 人 :</span> {{ item.name }}
        </p>
        <p>
          <span class="address-content-title">收货地区:</span
          >{{ item.consigneeAddressPath | unitAddress }}
        </p>
        <p>
          <span class="address-content-title">详细地址:</span> {{ item.detail }}
        </p>
        <p>
          <span class="address-content-title">手机号码:</span> {{ item.mobile }}
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import card from '@/components/card';
import { memberAddress, delMemberAddress } from '@/api/address.js';

export default {
  name: 'MyAddress',

  data () {
    return {
      list: [] // 地址列表
    };
  },
  methods: {
    edit (id) {
      // 编辑地址
      this.$router.push({ path: '/home/addAddress', query: { id: id } });
    },
    del (id) {
      // 删除地址
      this.$Modal.confirm({
        title: '提示',
        content: '你确定删除这个收货地址',
        onOk: () => {
          delMemberAddress(id).then((res) => {
            if (res.success) {
              this.$Message.success('删除成功');
              this.getAddrList();
            }
          });
        },
        onCancel: () => {
          this.$Message.info('取消删除');
        }
      });
    },
    getAddrList () {
      // 获取地址列表
      memberAddress().then((res) => {
        console.log(res);
        if (res.success) {
          this.list = res.result.records;
        }
      });
    }
  },
  mounted () {
    this.getAddrList();
  }
};
</script>

<style scoped lang="scss">
.address-box {
  padding: 15px;
  margin: 15px;

  border-bottom: 1px solid $border_color;
}

.address-header {
  cursor: pointer;
  height: 35px;
  display: flex;
  justify-content: space-between;
  @include title_color($light_title_color);
  font-size: 18px;
}

.address-content {
  cursor: pointer;
  font-size: 14px;

  > p {
    padding: 12px 0;
  }
}

.address-content-title {
 
}

.address-action span {
  margin-left: 15px;
  font-size: 14px;
  color: $theme_color;
  cursor: pointer;
}

#map-container {
  width: 500px;
  height: 300px;
}
</style>
