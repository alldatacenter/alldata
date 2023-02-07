import {
  DATA_FIELD_TYPE_AUDIO,
  DATA_FIELD_TYPE_CURRENCY,
  DATA_FIELD_TYPE_TIME,
  DATA_FIELD_TYPE_GENERAL, DATA_FIELD_TYPE_IMAGE,
  DATA_FIELD_TYPE_NUMERIC, DATA_FIELD_TYPE_URL, DATA_FIELD_TYPE_VIDEO, DATA_FIELD_TYPE_HTML, DATA_FIELD_TYPE_LONG_TEXT
} from "@/constants/dataFields";
import {DEDUP_TYPE_IGNORE, DEDUP_TYPE_OVERWRITE} from "@/constants/dedup";

const result: LComponentsResult = {
  form: {
    dataType: '数据类型',
  },
  types: {
    [DATA_FIELD_TYPE_GENERAL]: '通用',
    [DATA_FIELD_TYPE_NUMERIC]: '数值',
    [DATA_FIELD_TYPE_TIME]: '时间',
    [DATA_FIELD_TYPE_CURRENCY]: '货币',
    [DATA_FIELD_TYPE_URL]: 'URL',
    [DATA_FIELD_TYPE_IMAGE]: '图片',
    [DATA_FIELD_TYPE_AUDIO]: '音频',
    [DATA_FIELD_TYPE_VIDEO]: '视频',
    [DATA_FIELD_TYPE_HTML]: 'HTML',
    [DATA_FIELD_TYPE_LONG_TEXT]: '长文本',
  },
  dedup: {
    dialog: {
      fields: {
        title: '去重字段配置',
        placeholder: '输入去重字段名称',
      },
    },
    labels: {
      dedupType: '去重类型',
    },
    types: {
      [DEDUP_TYPE_IGNORE]: '忽略',
      [DEDUP_TYPE_OVERWRITE]: '覆盖',
    }
  },
};

export default result;
