import dayjs from 'dayjs';
import {
  DATA_FIELD_TYPE_CURRENCY,
  DATA_FIELD_TYPE_TIME,
  DATA_FIELD_TYPE_GENERAL, DATA_FIELD_TYPE_IMAGE,
  DATA_FIELD_TYPE_NUMERIC, DATA_FIELD_TYPE_URL, DATA_FIELD_TYPE_HTML, DATA_FIELD_TYPE_LONG_TEXT
} from "@/constants/dataFields";

export const inferDataFieldTypes = (fields: DataField[], data: Result[]): DataField[] => {
  const fieldTypesMap = new Map<string, Map<DataFieldType, number>>();
  fields.forEach(f => {
    fieldTypesMap.set(f.key as string, new Map<DataFieldType, number>());
  });

  data.forEach((d) => {
    fields.forEach((f) => {
      const type = inferDataFieldType(d[f.key as string]);
      const typeCountMap = fieldTypesMap.get(f.key as string) as Map<DataFieldType, number>;
      typeCountMap.set(type, (typeCountMap.get(type) || 0) + 1);
    });
  });

  return fields.map((f) => {
    const typeCountMap = fieldTypesMap.get(f.key as string) as Map<DataFieldType, number>;
    const type = Array.from(typeCountMap.entries()).reduce((a, b) => a[1] > b[1] ? a : b)[0];
    return {
      ...f,
      type,
    };
  })
};

export const inferDataFieldType = (value: any): DataFieldType => {
  // convert to string
  value = String(value);

  // html
  if (value.match(/<(?:"[^"]*"['"]*|'[^']*'['"]*|[^'">])+>/)) {
    return DATA_FIELD_TYPE_HTML;
  }

  // currency
  if (value.match(/[0-9]+(\.[0-9]+)?/) && value.match(/\$|£|¥/)) {
    return DATA_FIELD_TYPE_CURRENCY;
  }

  // numeric
  if (value.match(/^[0-9,?]+(\.[0-9]+)?$/)) {
    return DATA_FIELD_TYPE_NUMERIC;
  }

  // date
  if (dayjs(value).toString() !== 'Invalid Date') {
    return DATA_FIELD_TYPE_TIME;
  }

  // image
  if (value.match(/(http(s?):)([/|.|\w|\s|-])*\.(?:jpg|gif|png)/)) {
    return DATA_FIELD_TYPE_IMAGE;
  }

  // url
  if (value.match(/(http(s?):)([/|.|\w|\s|-])*/)) {
    return DATA_FIELD_TYPE_URL;
  }

  // long text
  if (value.length > 200) {
    return DATA_FIELD_TYPE_LONG_TEXT;
  }

  return DATA_FIELD_TYPE_GENERAL;
};

export const getDataFieldIconByType = (type: DataFieldType): Icon => {
  switch (type) {
    case DATA_FIELD_TYPE_TIME:
      return ['fa', 'clock'];
    case DATA_FIELD_TYPE_NUMERIC:
      return ['fa', 'hashtag'];
    case DATA_FIELD_TYPE_CURRENCY:
      return ['fa', 'dollar'];
    case DATA_FIELD_TYPE_URL:
      return ['fa', 'link'];
    case DATA_FIELD_TYPE_IMAGE:
      return ['fa', 'image'];
    case DATA_FIELD_TYPE_HTML:
      return ['fa', 'code'];
    case DATA_FIELD_TYPE_LONG_TEXT:
      return ['fa', 'file'];
    default:
      return ['fa', 'font'];
  }
};

export const getDataFieldIconClassNameByType = (type: DataFieldType): string => {
  const icon = getDataFieldIconByType(type);
  return `${icon[0]} fa-${icon[1]}`;
}
