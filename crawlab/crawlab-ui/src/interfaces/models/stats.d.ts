import {Dayjs} from 'dayjs';

export declare global {
  interface StatsResult extends Result {
    date?: string | number;
  }

  interface DateRange {
    start: Dayjs;
    end: Dayjs;
  }
}
