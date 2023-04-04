import { FontStyle, LineStyle } from 'app/types/ChartConfig';

export type GaugeTitleStyle = {
  show: boolean;
  offsetCenter: number[];
} & FontStyle;

export interface GaugeProgressStyle {
  show: boolean;
  roundCap: boolean;
  width: number;
}

export interface GaugeSplitLineStyle {
  show: boolean;
  length: number;
  distance: number;
  lineStyle: LineStyle;
}

export interface GaugePointerStyle {
  show: boolean;
  length: string | number;
  width: string | number;
  itemStyle: {
    color: string;
    borderType: string;
    borderWidth: number;
    borderColor: string;
  };
}

export type GaugeDetailStyle = {
  show: boolean;
  offsetCenter: number[];
  formatter: (value) => string;
} & FontStyle;

export interface GaugeAxisStyle {
  axisLine: {
    lineStyle: {
      width: number;
      color: Array<number | string[]>;
    };
    roundCap: boolean;
  };
  axisTick: {
    show: boolean;
    splitNumber: number;
    distance: number;
    lineStyle: LineStyle;
  };
  axisLabel: {
    show: boolean;
    distance: number;
  } & FontStyle;
}

export interface GaugeStyle {
  type: string;
  max: number;
  splitNumber: number;
  radius: string;
  startAngle: number;
  endAngle: number;
}

export interface DataConfig {
  name: string;
  value: string | number;
  itemStyle: any;
}

export type SeriesConfig = {
  data: DataConfig[];
  pointer: GaugePointerStyle;
  title: GaugeTitleStyle;
  splitLine: GaugeSplitLineStyle;
  detail: GaugeDetailStyle;
  progress: GaugeProgressStyle;
} & GaugeStyle &
  GaugeAxisStyle;
