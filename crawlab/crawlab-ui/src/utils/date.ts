import dayjs, {ManipulateType} from 'dayjs';

export const getPastNDateRangeItem = (key: string, n: number, unit: ManipulateType): RangeItem => {
  return {
    key,
    value: () => {
      return {
        start: dayjs(),
        end: dayjs().subtract(n, unit)
      };
    },
  };
};
