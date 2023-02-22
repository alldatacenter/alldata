<!--
 * @Author: mjzhu
 * @Date: 2022-06-13 14:04:05
 * @LastEditTime: 2022-09-07 10:56:39
 * @FilePath: \ddh-ui\src\components\commonTemplate\index.vue
-->
<template>
  <div class="common-template steps">
    <a-form-model ref="formRef" :label-col="labelCol" :wrapper-col="wrapperCol" :model="form" class="form-content mgh160">
      <div v-for="(item, index) in form.testData" :key="index">
        <div class="form-item-container" v-if="!['multipleWithKey', 'multiple', 'multipleSelect'].includes(item.type)">
          <a-form-model-item :label="item.label" :prop="'testData.'+index+'.'+ `${item.name}`" :rules="{ required: item.required, message: `${item.label}不能为空!` }">
            <a-input v-if="item.type==='input'" v-model="item[`${item.name}`]" placeholder="请输入" />
            <a-slider v-if="item.type==='slider'" :marks="marks(item)" :min="item.minValue" :max="item.maxValue" style="width: 96%;display: inline-block" v-model="item.value" />
            <a-switch v-if="item.type==='switch'" v-model="item.value"></a-switch>
            <a-select v-if="item.type==='select'" v-model="item.value" placeholder="请选择">
              <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
            </a-select>
            <a-tooltip v-if="item.description">
              <template slot="title">
                <span>{{item.description}}</span>
              </template>
              <a-icon :class="['mgl10','filed-name-tips-icon', item.type === 'slider' ? 'slider-icon' : '']" type="question-circle-o" />
            </a-tooltip>
          </a-form-model-item>
          <div class="filed-name-tips">
            <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
          </div>
        </div>
        <div v-else>
          <div v-if="['multiple'].includes(item.type)" class="form-item-container">
            <a-form-model-item :prop="'testData.'+index+'.'+ `${item.name+'multiple'+childIndex}`" v-for="(child, childIndex) in item.value" :key="childIndex" v-bind="childIndex === 0 ? labelCol : formItemLayoutWithOutLabel" :label="(childIndex === 0 || item.value.length === 0) ? item.label : ''">
              <a-input v-model='item.value[childIndex]' placeholder="请输入" />
              <span @click="() => reduceMultiple(item.name, childIndex, 'multiple')">
                <svg-icon v-if="item.value.length > 1" icon-class="reduce-icon" class="reduce-icon" />
              </span>
            </a-form-model-item>
            <a-form-model-item class="form-multiple-item" v-bind="item.value.length === 0 ? labelCol : formItemLayoutWithOutLabel" :label="item.value.length === 0 ? item.label : ''">
              <a-button type="dashed" @click="() => addMultiple(item.name, 'multiple')">
                <a-icon type="plus" />Add field
              </a-button>
            </a-form-model-item>
            <div class="filed-name-tips">
              <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
            </div>
          </div>
          <div v-if="['multipleWithKey'].includes(item.type)" class="form-item-container">
            <a-form-model-item  v-for="(child, childIndex) in item.value" :rules="[{required: item.required, whitespace: true, message: `${item.label}不能为空!`}]" :prop="'testData.'+index+'.'+ `${item.name+'arrayWithKey'+childIndex}`" style="margin-bottom: 0px" :key="childIndex" :required="item.required" v-bind="childIndex === 0 ? labelCol : formItemLayoutWithOutLabel" :label="childIndex === 0 || item.value.length === 0  ? item.label : ''">
              <a-row type="flex" style="position: relative">
                <a-col :span="12">
                  <a-form-model-item style="width:97%">
                    <a-input v-model="child.key" placeholder="请输入" />
                  </a-form-model-item>
                </a-col>
                <a-col :span="12">
                  <a-form-model-item :prop="'testData.'+index+'.'+ `${item.name+'arrayWithValue'+childIndex}`" :rules="[{required: item.required, whitespace: true, message: `${item.label}不能为空!`}]"  style="width:97%">
                    <a-input v-model="child.value" placeholder="请输入" />
                  </a-form-model-item>
                </a-col>
                <span style="position: absolute; right: 0px" @click="() => reduceMultiple(item.name, childIndex, 'multipleWithKey')">
                  <svg-icon v-if="item.value.length > 1" icon-class="reduce-icon" class="reduce-icon" />
                </span>
              </a-row>
            </a-form-model-item>
            <a-form-model-item class="form-multiple-item" v-bind="item.value.length === 0 ? labelCol : formItemLayoutWithOutLabel" :label="item.value.length === 0 ? item.label : ''">
              <a-button type="dashed" @click="() => addMultiple(item.name, 'multipleWithKey')">
                <a-icon type="plus" />Add field
              </a-button>
            </a-form-model-item>
            <div class="filed-name-tips">
              <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
            </div>
          </div>
          <div v-if="['multipleSelect'].includes(item.type)" class="form-item-container">
            <a-form-model-item :label="item.label" :prop="item.name" :rules="[{required: item.required, whitespace: true, message: `${item.label}不能为空!`}]">
              <a-select mode="multiple" v-model="item.value" placeholder="请选择">
                <a-select-option v-for="(child, childIndex) in item.selectValue" :key="childIndex" :value="child">{{child}}</a-select-option>
              </a-select>
              <a-tooltip v-if="item.description">
                <template slot="title">
                  <span>{{item.description}}</span>
                </template>
                <a-icon class="mgl10 filed-name-tips-icon" type="question-circle-o" />
              </a-tooltip>
            </a-form-model-item>
            <div class="filed-name-tips">
              <span class="filed-name-tips-word" :title="item.name">{{item.name.replaceAll("!", ".")}}</span>
            </div>
          </div>
        </div>
      </div>
    </a-form-model>
  </div>
</template>
<script>
export default {
  name: "CommonTemplate",
  components: {},
  props: { templateData: Array },
  data() {
    const self = this
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 5 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 19 },
      },
      initFormFiledFlag: false,
      formItemLayoutWithOutLabel: {
        wrapperCol: {
          xs: { span: 24, offset: 0 },
          sm: { span: 19, offset: 5 },
        },
      },
      form: {
        testData: this.templateData,
      },
      // form: this.$form.createForm(this, {
      //   onValuesChange: function (props, fileds) {
      //     if (self.initFormFiledFlag) {
      //       self.initFormFiledFlag = false
      //       return false
      //     }
      //     for (var i in fileds) {
      //       if (i.includes('multiple')) {
      //         console.log(fileds, 'sssss')
      //         self.initFormFiledFlag = true
      //         self.form.getFieldsValue([`${i}`])
      //         self.form.setFieldsValue({
      //           [`${i}`]: fileds[i]
      //         })
      //       }
      //     }
      //   },
      // }),
    };
  },
  watch: {
    templateData: {
      handler(val) {
        this.form.testData = val;
        this.initFormData();
      },
      deep: true,
      immediate: true,
    },
  },
  methods: {
    initFormData() {
      let arr = _.cloneDeep(this.form.testData);
      console.log(arr, '要渲染的数据')
      let formData = arr.filter((item) => !item.hidden);
      formData.forEach((item, index) => {
        if (["multipleWithKey", "multiple"].includes(item.type)) {
          if (item.value.length === 0) {
            if (["multipleWithKey"].includes(item.type)) {
              item.value.push({
                key: "",
                value: "",
              });
            } else {
              item.value.push("");
            }
          } else {
            if (["multipleWithKey"].includes(item.type)) {
              let arr = [];
              item.value.map((childItem, childIndex) => {
                for (let key in childItem) {
                  arr.push({
                    key: key,
                    value: childItem[key],
                  });
                }
              });
              item.value = arr;
            }
          }
        } else {
          item.value = [null, undefined, ""].includes(item.value)
            ? item.defaultValue
            : item.value;
          if (Object.prototype.toString.call(item.value) === '[object Array]' && item.value.length === 0) {
            item.value = item.defaultValue
          }
        }
        item[`${item.name}`] = item.value
      });
      this.form.testData = formData;
    },
    marks(item) {
      return {
        [`${item.minValue}`]: item.minValue,
        [`${item.maxValue}`]: item.maxValue,
      };
    },
    addMultiple(name, type) {
      this.form.testData.forEach((item) => {
        if (item.name === name) {
          if (["multipleWithKey"].includes(type)) {
            item.value.push({
              key: "",
              value: "",
            });
          } else {
            item.value.push("");
          }
        }
      });
    },
    reduceMultiple(name, childIndex, type) {
      this.form.testData.forEach((item) => {
        if (item.name === name) {
          item.value.splice(childIndex, 1);
        }
      });
    },
  },
  created() {
    // this.initFormData();
  },
};
</script>
<style lang="less" scoped>
.common-template {
  .mgh160 {
    // margin: 0 110px;
  }
  .form-content {
    .reduce-icon {
      cursor: pointer;
      font-size: 18px;
      color: @primary-color;
      margin-left: 6px;
    }
    .form-multiple-item:last-child {
      margin-bottom: 12px;
    }
    .form-item-container {
      position: relative;
      // margin-bottom: 6px;
      .filed-name-tips {
        display: flex;
        align-items: center;
        position: absolute;
        font-size: 12px;
        top: 32px;
        left: 12px;
        width: 20%;
        &-word {
          display: inline-block;
          max-width: calc(100% - 0px);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
      .filed-name-tips-icon {
          cursor: pointer;
        }
      .slider-icon {
        position: relative;
        top: -28px;
      }
    }
  }
}
</style>