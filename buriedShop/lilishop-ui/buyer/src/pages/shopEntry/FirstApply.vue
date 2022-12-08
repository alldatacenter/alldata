<template>
  <div class="company-msg">
    <Form ref="firstForm" :model="form" :rules="rules" :label-width="140">
      <h4>基础信息</h4>
      <FormItem prop="companyName" label="公司名称">
        <Input
          type="text"
          v-model="form.companyName"
          placeholder="请填写公司信息"
        />
      </FormItem>
      <FormItem prop="companyAddressIdPath" label="公司所在地">
        <region
          style="width: 250px"
          @selected="selectedRegion"
          :addressId="address"
        />
      </FormItem>
      <FormItem prop="companyAddress" label="公司详细地址">
        <Input
          type="text"
          v-model="form.companyAddress"
          placeholder="请填写公司详细信息"
        />
      </FormItem>
      <FormItem prop="employeeNum" label="员工总数">
        <Input
          type="text"
          v-model="form.employeeNum"
          placeholder="请填写公司员工总数"
          ><span slot="append">人</span>
          </Input>
      </FormItem>
      <FormItem prop="companyPhone" label="公司电话">
        <Input
          type="text"
          v-model="form.companyPhone"
          placeholder="请填写公司电话"
          ></Input>
      </FormItem>
      <FormItem prop="registeredCapital" label="注册资金">
        <Input
          type="text"
          v-model="form.registeredCapital"
          placeholder="请填写注册资金"
          ><span slot="append">万元</span></Input>
      </FormItem>
      <FormItem prop="linkName" label="联系人姓名">
        <Input
          type="text"
          v-model="form.linkName"
          placeholder="请填写联系人姓名"
        />
      </FormItem>
      <FormItem prop="linkPhone" label="联系人电话">
        <Input
          type="text"
          v-model="form.linkPhone"
          placeholder="请填写联系人电话"
        />
      </FormItem>
      <FormItem prop="companyEmail" label="电子邮箱">
        <Input
          type="text"
          v-model="form.companyEmail"
          placeholder="请填写电子邮箱"
        />
      </FormItem>

      <h4>营业执照信息</h4>
      <FormItem prop="licenseNum" label="营业执照号">
        <Input
          type="text"
          v-model="form.licenseNum"
          placeholder="请填写营业执照号"
        />
      </FormItem>
      <FormItem prop="scope" label="法定经营范围">
        <Input
          type="textarea"
          v-model="form.scope"
          maxlength="200"
          show-word-limit
          :rows="4"
          placeholder="请输入营业执照所示经营范围"
        />
      </FormItem>
      <FormItem prop="licencePhoto" label="营业执照电子版">
        <Upload
          ref="uploadLicence"
          :show-upload-list="false"
          :on-success="handleSuccess"
          :format="['jpg', 'jpeg', 'png', 'gif']"
          :max-size="2048"
          :before-upload="beforeUpload"
          :on-format-error="handleFormatError"
          :on-exceeded-size="handleMaxSize"
          :on-error="uploadErr"
          multiple
          :action="action"
          :headers="accessToken"
        >
          <Button type="info" :loading="uploadLoading">证照上传</Button>
        </Upload>
        <div class="describe">
          请压缩图片在2M以内，格式为gif，jpg，png，并确保文字清晰，以免上传或审核失败
        </div>
        <div
          class="img-list"
          v-for="(item, index) in form.licencePhoto"
          :key="index"
        >
          <img :src="item" width="100" alt="" />
          <div class="cover">
            <Icon
              type="ios-eye-outline"
              @click.native="handleView(item)"
            ></Icon>
            <Icon
              type="ios-trash-outline"
              @click.native="handleRemove(index, 'licencePhoto')"
            ></Icon>
          </div>
        </div>
      </FormItem>

      <h4>法人信息</h4>
      <FormItem prop="legalName" label="法人姓名">
        <Input
          type="text"
          v-model="form.legalName"
          maxlength="20"
          placeholder="请输入法人姓名"
        />
      </FormItem>
      <FormItem prop="legalId" label="法人证件号">
        <Input
          type="text"
          v-model="form.legalId"
          placeholder="请输入法人证件号"
        />
      </FormItem>
      <FormItem prop="legalPhoto" label="法人证件电子版">
        <Upload
          ref="uploadLegal"
          :show-upload-list="false"
          :on-success="handleSuccess1"
          :before-upload="beforeUpload1"
          :max-size="2048"
          :format="['jpg', 'jpeg', 'png', 'gif']"
          :on-format-error="handleFormatError"
          :on-exceeded-size="handleMaxSize"
          :on-error="uploadErr"
          multiple
          :action="action"
          :headers="accessToken"
        >
          <Button type="info" :loading="uploadLoading1">证照上传</Button>
        </Upload>
        <div class="describe">
          请压缩图片在2M以内，身份证正反面两张照片，确保图片清晰无缺角
        </div>
        <div
          class="img-list"
          v-for="(item, index) in form.legalPhoto"
          :key="index"
        >
          <img :src="item" width="100" alt="" />
          <div class="cover">
            <Icon
              type="ios-eye-outline"
              @click.native="handleView(item)"
            ></Icon>
            <Icon
              type="ios-trash-outline"
              @click.native="handleRemove(index, 'legalPhoto')"
            ></Icon>
          </div>
        </div>
      </FormItem>
      <FormItem>
        <Button type="primary" :loading="loading" @click="next"
          >填写财务资质信息</Button
        >
      </FormItem>
    </Form>
    <Modal title="View Image" v-model="visible">
      <img :src="previewPicture" v-if="visible" style="width: 100%" />
    </Modal>
  </div>
</template>
<script>
import { applyFirst } from '@/api/shopentry';
import * as RegExp from '@/plugins/RegExp.js';
import region from '@/components/map/region.vue';
import storage from '@/plugins/storage';
import { commonUrl } from '@/plugins/request.js';
export default {
  components: { region },
  props: {
    content: {
      default: {},
      type: Object
    }
  },
  data () {
    return {
      action: commonUrl + '/common/common/upload/file', // 上传地址
      accessToken: {}, // 验证token
      visible: false, // 预览图片
      loading: false, // 加载状态
      address: '', // 地址
      previewPicture: '', // 预览图片url
      form: { // 表单数据
        legalPhoto: [],
        licencePhoto: []
      },
      rules: { // 验证规则
        companyName: [{ required: true, message: '请填写公司信息' }],
        companyAddressIdPath: [{ required: true, message: '请选择公司所在地' }],
        companyAddress: [{ required: true, message: '请填写公司详细地址' }],
        employeeNum: [
          { required: true, message: '请填写公司员工总数' },
          { pattern: RegExp.integer, message: '只能填写正整数' }
        ],
        registeredCapital: [
          { required: true, message: '请填写公司注册资金' },
          { pattern: RegExp.integer, message: '只能填写正整数' }
        ],
        linkName: [{ required: true, message: '请填写联系人姓名' }],
        linkPhone: [
          { required: true, message: '请填写联系人电话' },
          { pattern: RegExp.mobile, message: '请填写正确的号码' }
        ],
        companyPhone: [
          { required: true, message: '请填写公司电话' },
          { pattern: RegExp.TEL, message: '请填写正确的号码' }
        ],
        companyEmail: [
          { required: true, message: '请填写电子邮箱' },
          { type: 'email', message: '请输入正确的邮箱' }
        ],
        licenseNum: [
          { required: true, message: '请填写营业执照号' },
          { pattern: RegExp.licenseNum, message: '请输入正确的营业执照号' }
        ],
        scope: [{ required: true, message: '请填写营业执照所示经营范围' }],
        legalPhoto: [{ required: true, message: '请上传法人身份证照片' }],
        licencePhoto: [{ required: true, message: '请上传营业执照' }],
        legalName: [{ required: true, message: '请输入法人姓名' }],
        legalId: [
          { required: true, message: '请输入法人证件号' },
          { pattern: RegExp.IDCard, message: '请输入正确的证件号' }
        ]
      },
      uploadLoading1: false, // 上传loading
      uploadLoading: false // 上传loading
    };
  },
  methods: {
    // 下一步
    next () {
      this.$refs.firstForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          let params = JSON.parse(JSON.stringify(this.form));
          params.legalPhoto = this.form.legalPhoto.toString();
          params.licencePhoto = this.form.licencePhoto.toString();
          applyFirst(params)
            .then((res) => {
              this.loading = false;
              if (res.success) this.$emit('change', 1);
            })
            .catch(() => {
              this.loading = false;
            });
        } else {
          console.log('error');
        }
      });
    },
    // 地址选择回显
    selectedRegion (item) {
      this.$set(this.form, 'companyAddressIdPath', item[0].toString());
      this.$set(
        this.form,
        'companyAddressPath',
        item[1].toString().replace(/\s/g, '')
      );
    },
    // 上传之前
    beforeUpload () {
      this.uploadLoading = true;
      if (this.form.licencePhoto.length >= 3) {
        this.$Message.warning('最多上传三张图片')
        return false;
      }
    },
    // 上传之前
    beforeUpload1 () {
      this.uploadLoading1 = true;
      if (this.form.legalPhoto.length >= 2) {
        this.$Message.warning('最多上传两张图片')
        return false;
      }
    },
    // 上传成功回调
    handleSuccess (res, file) {
      this.uploadLoading = false;
      this.form.licencePhoto.push(res.result);
    },
    // 上传成功回调
    handleSuccess1 (res, file) {
      this.uploadLoading1 = false;
      this.form.legalPhoto.push(res.result);
    },
    // 上传失败
    uploadErr () {
      this.uploadLoading = false;
      this.uploadLoading1 = false;
    },
    // 上传失败回调
    handleFormatError (file) {
      this.uploadLoading = false;
      this.uploadLoading1 = false;
      this.$Notice.warning({
        title: 'The file format is incorrect',
        desc: '上传文件格式不正确'
      });
    },
    // 上传大小限制
    handleMaxSize (file) {
      this.uploadLoading = false;
      this.uploadLoading1 = false;
      this.$Notice.warning({
        title: 'Exceeding file size limit',
        desc: '文件大小不能超过2M'
      });
    },
    // 图片查看
    handleView (item) {
      this.previewPicture = item;
      this.visible = true;
    },
    // 删除图片
    handleRemove (index, listName) {
      this.form[listName].splice(index, 1);
    }
  },
  mounted () {
    this.accessToken.accessToken = storage.getItem('accessToken');
    if (Object.keys(this.content).length) { // 处理回显数据
      this.form = JSON.parse(JSON.stringify(this.content));
      if (this.form.licencePhoto) {
        this.form.legalPhoto = this.content.legalPhoto.split(',');
        this.form.licencePhoto = this.content.licencePhoto.split(',');
        this.address = this.form.companyAddressIdPath;
      }
    }
  }
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
.ivu-input-wrapper {
  width: 300px;
}
.img-list {
  display: inline-block;
  margin: 10px;
  width: 100px;
  position: relative;
  .cover {
    display: none;
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(0, 0, 0, 0.6);
    width: inherit;
    height: inherit;
    align-items: center;
    justify-content: space-around;
    i {
      color: #fff;
      font-size: 30px;
      cursor: pointer;
    }
  }
  &:hover .cover {
    display: flex;
  }
}
.describe {
  font-size: 12px;
  color: #999;
}
</style>
