import {
  DATA_FIELD_TYPE_AUDIO,
  DATA_FIELD_TYPE_CURRENCY, DATA_FIELD_TYPE_TIME,
  DATA_FIELD_TYPE_GENERAL, DATA_FIELD_TYPE_IMAGE,
  DATA_FIELD_TYPE_NUMERIC,
  DATA_FIELD_TYPE_URL, DATA_FIELD_TYPE_VIDEO, DATA_FIELD_TYPE_HTML, DATA_FIELD_TYPE_LONG_TEXT
} from "@/constants/dataFields";
import {DEDUP_TYPE_IGNORE, DEDUP_TYPE_OVERWRITE} from "@/constants/dedup";

const result: LComponentsResult = {
  form: {
    dataType: 'Data Type',
  },
  types: {
    [DATA_FIELD_TYPE_GENERAL]: 'General',
    [DATA_FIELD_TYPE_NUMERIC]: 'Numeric',
    [DATA_FIELD_TYPE_TIME]: 'Time',
    [DATA_FIELD_TYPE_CURRENCY]: 'Currency',
    [DATA_FIELD_TYPE_URL]: 'URL',
    [DATA_FIELD_TYPE_IMAGE]: 'Image',
    [DATA_FIELD_TYPE_AUDIO]: 'Audio',
    [DATA_FIELD_TYPE_VIDEO]: 'Video',
    [DATA_FIELD_TYPE_HTML]: 'HTML',
    [DATA_FIELD_TYPE_LONG_TEXT]: 'Long Text',
  },
  dedup: {
    dialog: {
      fields: {
        title: 'Deduplication Fields Configuration',
        placeholder: 'Enter a field name for deduplication',
      },
    },
    labels: {
      dedupType: 'Deduplication Type',
    },
    types: {
      [DEDUP_TYPE_IGNORE]: 'Ignore',
      [DEDUP_TYPE_OVERWRITE]: 'Overwrite',
    },
  },
};

export default result;
