<template>
  <div>
    <Col span="25">
      <div class="itemWrap" v-for="(part, index) in configs" :key="index">
        <div class="normal" v-if="part.child_def && part.child_def.length">
          <h3>{{ part.name }}</h3>
          <div>
            <Form v-if="valueMap[part.key]" :model="valueMap[part.key]" :ref="part.key" :rules="rule[part.key]">
              <FormItem v-for="(def, index) in part.child_def" :key="index" :label="def.name" :prop="def.key" :data-prop="def.key">
                <Input v-if="def.type === 'INPUT' || def.type === 'NUMBER'" v-model="valueMap[part.key][def.key]" />
                <!-- <Input v-else-if="def.type === 'NUMBER'" v-model="valueMap[part.key][def.key]" type="number" /> -->
                <Select
                  v-else
                  v-model="valueMap[part.key][def.key]"
                  class="select"
                >
                  <Option
                    v-for="(item, index) in def.ref_values"
                    :value="item"
                    :key="index"
                  >
                    {{ item }}
                  </Option>
                </Select>
              </FormItem>
            </Form>
          </div>
        </div>
        <div class="canEdited" v-else-if="part.child_def">
          <h3>{{ part.name }}</h3>
          <div>
            <Form>
              <Row v-for="(item, index) in diyMap[part.key]" :key="index">
                <Col span="9">
                  <FormItem>
                    <div class="inputWrap">
                      <div class="flinkIndex">{{ index + 1 }}</div>
                      <Input v-model="item.key" />
                      <div class="equity">=</div>
                    </div>
                  </FormItem>
                </Col>
                <Col span="5">
                  <FormItem>
                    <Input v-model="item.value" />
                  </FormItem>
                </Col>
                <Col span="10">
                  <div class="inputWrap">
                    <div
                      class="icon"
                      @click="removeParameter(index, part.key)"
                      v-show="diyMap[part.key].length !== 1"
                    >
                      <Icon type="md-close" />
                    </div>
                    <div
                      class="icon"
                      v-show="index + 1 === diyMap[part.key].length"
                      @click="addParameter(part.key)"
                    >
                      <Icon type="md-add" />
                    </div>
                  </div>
                </Col>
              </Row>
            </Form>
          </div>
        </div>
        <div class="noChild" v-else>
          <h3>{{ part.name }}</h3>
        </div>
      </div>
    </Col>
    <div class="saveBtn">
      <Button
        type="primary"
        @click="handleSaveConfig()"
        :loading="saveLoading"
        style="width:100px;height:40px;background:rgba(22, 155, 213, 1);"
      >
        {{$t('message.streamis.formItems.saveBtn')}}
      </Button>
    </div>
  </div>
</template>
<script>
import api from '@/common/service/api'
import { cloneDeep } from 'lodash';
export default {
  data() {
    return {
      configs: [],
      valueMap: {},
      diyMap: {},
      saveLoading: false,
      rule: {},
    }
  },
  mounted() {
    this.getUsers()
    this.getConfigs()
  },
  methods: {
    getUsers() {
      api
        .fetch('streamis/streamJobManager/config/getWorkspaceUsers', 'get')
        .then(res => {
          console.log(res)
          if (res && res.users) {
            this.users = res.users
          }
        })
        .catch(e => console.warn(e))
    },
    getValues() {
      api
        .fetch(
          'streamis/streamJobManager/config/json/' + this.$route.params.id,
          'get'
        )
        .then(res => {
          const valueMap = this.valueMap;
          Object.keys(res || {}).forEach(key => {
            valueMap[key] = {};
            Object.keys(res[key]).forEach(k => {
              const formatKey = k.replace(/\./g, '/');
              valueMap[key][formatKey] = res[key][k];
            })
          })
          Object.keys(this.diyMap).forEach(key => {
            if (Object.keys(valueMap).includes(key)) {
              let keyValue = Object.keys(valueMap[key] || {}).map(k => ({key: k.replace(/\//g, '.'), value: valueMap[key][k]}));
              valueMap[key] = {};
              if (!keyValue.length) keyValue = [{value: '', key: ''}];
              this.diyMap = {
                ...this.diyMap,
                [key]: keyValue
              }
            }
          })
          this.valueMap = cloneDeep(valueMap);
        })
        .catch(e => console.warn(e))
    },
    getConfigs() {
      api
        .fetch(
          'streamis/streamJobManager/config/definitions',
          'get'
        )
        .then(res => {
          console.log(res)
          let configs = res.def;
          const valueMap = {};
          const rule = {};
          configs = configs.map(conf => {
            valueMap[conf.key] = {};
            rule[conf.key] = {};
            if (!conf.child_def) return conf;
            if (!conf.child_def.length) {
              this.diyMap = {...this.diyMap, [conf.key]: [{value: '', key: ''}]};
            }
            conf.child_def = conf.child_def.map(def => {
              if (def.validate_type !== 'Regex') def.validate_rule = '';
              else def.validate_rule = def.validate_rule || '';
              const defaultValue = def.default_value || '';
              const defKey = def.key.replace(/\./g, '/');
              def.key = defKey;
              valueMap[conf.key][defKey] = defaultValue;
              const rules = [{required: def.required, message: this.$t('message.streamis.formItems.notEmpty'), trigger: 'blur'}];
              if (def.type !== 'SELECT') {
                if (def.validate_rule) rules.push({
                  pattern: new RegExp(def.validate_rule),
                  message: this.$t('message.streamis.formItems.wrongFormat'),
                })
              }
              rule[conf.key][defKey] = rules;
              return def;
            }).filter(def => ['SELECT', 'INPUT', 'NUMBER'].includes(def.type)).filter(def => !!def.visiable);
            return conf;
          });
          this.valueMap = cloneDeep(valueMap);
          this.rule = cloneDeep(rule);
          this.configs = configs;
          this.getValues()
        })
        .catch(e => console.warn(e))
    },
    removeParameter(index, key) {
      console.log('removeParameter', index);
      const keyValue = this.diyMap[key];
      keyValue.splice(index, 1)
      this.diyMap = {...this.diyMap, [key]: keyValue}
    },
    addParameter(key) {
      console.log('addParameter')
      this.diyMap = {...this.diyMap, [key]: this.diyMap[key].concat({value: '', key: ''})}
    },
    async handleSaveConfig() {
      console.log('handleSaveConfig')
      this.valueMap = cloneDeep(this.valueMap);
      const flags = await Promise.all(Object.keys(this.$refs).map(async ref => {
        const ele = this.$refs[ref][0];
        if (typeof ele.validate === 'function') return ele.validate();
        else return true;
      }));
      console.log('flags', flags);
      if (!flags.every(Boolean)) return;
      this.saveLoading = true;
      const configuration = {};
      Object.keys(this.valueMap).forEach(key => {
        configuration[key] = {};
        Object.keys(this.valueMap[key]).forEach(k => {
          const formatKey = k.replace(/\//g, '.');
          configuration[key][formatKey] = this.valueMap[key][k];
        })
      });
      let warning = false;
      let emptyWarning = false;
      Object.keys(this.diyMap).forEach(key => {
        configuration[key] = {};
        (this.diyMap[key] || []).forEach(mapKey => {
          emptyWarning = !mapKey.key || !mapKey.key.trim();
          if (configuration[key][mapKey.key]) warning = true;
          configuration[key][mapKey.key] = mapKey.value || '';
        });
        if ((this.diyMap[key] || []).length <= 1) {
          const only = (this.diyMap[key] || [])[0] || {};
          emptyWarning = !((!only.key || !only.key.trim()) && (!only.value || !only.value.trim()))
        }
      });
      console.log('configuration', configuration, this.valueMap)
      if (emptyWarning) {
        this.saveLoading = false;
        return this.$Message.error({ content: '请删除多余自定义字段，key值不能为空' });
      }
      if (warning) {
        this.saveLoading = false;
        return this.$Message.error({ content: '自定义字段名称不能重复' });
      }
      api
        .fetch(
          `streamis/streamJobManager/config/json/${this.$route.params.id}`,
          { ...configuration }
        )
        .then(res => {
          this.saveLoading = false
          console.log(res)
          if (res.errorMsg) {
            this.$Message.error(res.errorMsg.desc)
          } else {
            this.$Message.success(this.$t('message.streamis.operationSuccess'))
          }
        })
        .catch(e => {
          console.log(e)
          this.saveLoading = false
        })
    }
  }
}
</script>
<style lang="scss" scoped>
.inputWrap {
  display: flex;
}
.unit {
  margin-left: 10px;
}
.itemWrap {
  padding: 10px;
  & > p {
    font-weight: 700;
    font-size: 16px;
  }
  & > div {
    margin-left: 60px;
    margin-top: 10px;
  }
}
.flinkIndex {
  margin-right: 10px;
}
.equity {
  margin-left: 10px;
  margin-right: 10px;
}
.icon {
  font-size: 20px;
  height: 30px;
  margin-left: 10px;
  cursor: pointer;
  color: #666666;
}
.programArguement {
  background: rgba(94, 94, 94, 1);
  color: #fff;
  padding: 10px 20px;
  min-height: 64px;
}
.saveBtn {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}
</style>
