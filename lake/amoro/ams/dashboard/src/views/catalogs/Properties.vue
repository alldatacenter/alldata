
<template>
  <div class="config-properties">
    <div v-if="isEdit">
      <div class="config-header g-flex">
        <div class="td g-flex-ac">{{$t('key')}}</div>
        <div class="td g-flex-ac bd-left">{{$t('value')}}</div>
      </div>
      <a-form ref="propertiesFormRef" :model="propertiesForm" class="g-mt-12">
        <div class="config-row" v-for="(item, index) in propertiesForm.data" :key="item.uuid">
          <!-- validator: validateUnique -->
          <a-form-item
            :name="['data', index, 'key']"
            :rules="[{
              required: true,
              message: `${$t(placeholder.inputPh)}`
            }]"
            class="g-mr-8"
          >
            <a-input
              v-model:value="item.key"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            :name="['data', index, 'value']"
            :rules="[{
              required: true,
              message: `${$t(placeholder.inputPh)}`
            }]"
          >
            <a-input
              v-model:value="item.value"
              style="width: 100%"
            />
          </a-form-item>
          <close-outlined class="icon-close" @click="removeRule(item)"  />
        </div>
      </a-form>
      <a-button class="config-btn" @click="addRule">+</a-button>
    </div>
    <a-table
      v-if="!isEdit"
      rowKey="uuid"
      :columns="propertiesColumns"
      :data-source="propertiesForm.data"
      :pagination="false"
    ></a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowReactive, watch } from 'vue'
import { IMap } from '@/types/common.type'
import { CloseOutlined } from '@ant-design/icons-vue'
import { getUUid } from '@/utils/index'
import { usePlaceholder } from '@/hooks/usePlaceholder'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const props = defineProps<{ propertiesObj: IMap<string>, isEdit: boolean }>()
const propertiesColumns = shallowReactive([
  { dataIndex: 'key', title: t('key'), width: 284, ellipsis: true },
  { dataIndex: 'value', title: t('value'), ellipsis: true }
])
const propertiesFormRef = ref()
const propertiesForm = reactive<IMap<string>>({
  data: []
})
const placeholder = reactive(usePlaceholder())

const isEdit = computed(() => props.isEdit)

watch(() => props.propertiesObj, () => {
  initPropertiesArray()
}, {
  immediate: true,
  deep: true
})

function initPropertiesArray() {
  propertiesForm.data.length = 0
  Object.keys(props.propertiesObj).forEach(key => {
    propertiesForm.data.push({
      key: key,
      value: props.propertiesObj[key],
      uuid: getUUid()
    })
  })
}

function removeRule(item) {
  const index = propertiesForm.data.indexOf(item)
  if (index !== -1) {
    propertiesForm.data.splice(index, 1)
  }
  // propertiesFormRef.value.validateFields()
}
function addRule() {
  propertiesForm.data.push({
    key: '',
    value: '',
    uuid: getUUid()
  })
}

// async function validateUnique(rule, value) {
//   if (!value) {
//     return Promise.reject(new Error(t(placeholder.inputPh)))
//   } else if (value && (propertiesForm.data || []).filter(item => item.key === value).length > 1) {
//     return Promise.reject(new Error(t('duplicateKey')))
//   } else {
//     return Promise.resolve()
//   }
// }

defineExpose({
  getProperties() {
    return propertiesFormRef.value
      .validateFields()
      .then(() => {
        const propObj: IMap<string> = {}
        propertiesForm.data.forEach(e => {
          propObj[e.key] = e.value
        })
        return Promise.resolve(propObj)
      })
      .catch(() => {
        return false
      })
  }
})

onMounted(() => {
})

</script>

<style lang="less">
  .config-properties {
    width: 100%;
    display: flex;
    flex-direction: column;
    line-height: 32px;
    .config-header {
      width: 100%;
      padding-right: 32px;
      background: #fafafa;
      border-bottom: 1px solid #e8e8f0;
      .td {
        width: 50%;
        height: 40px;
        padding: 8px 12px;
        color: #102048;
        font-weight: 500;
      }
      .bd-left {
        position: relative;
        &:before {
          position: absolute;
          top: 50%;
          left: 0;
          width: 1px;
          height: 1.6em;
          background-color: rgba(0, 0, 0, 0.06);
          transform: translateY(-50%);
          transition: background-color 0.3s;
          content: '';
        }
      }
    }
    .config-row {
      display: flex;
      // height: 40px;
      position: relative;
      padding-right: 32px;
      width: 100%;
      .ant-form-item {
        width: 50%;
      }
      .ant-select-auto-complete {
        width: 50%;
        input {
          color: #79809a;
        }
      }
      .ant-input {
        width: 50%;
      }
      .icon-close {
        cursor: pointer;
        position: absolute;
        right: 8px;
        top: 12px;
        font-size: 12px;
        &.disabled {
          cursor: not-allowed;
        }
      }
    }
    .config-btn {
      border-radius: 4px;
      width: 100%;
      border: 1px solid #e5e5e5;
      text-align: center;
      color: #102048;
      box-shadow: none;
    }
  }
</style>
