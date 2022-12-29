import { BorderStyle, FontStyle, LabelStyle } from '../../../types/ChartConfig';

export type GeoInfo = {
  map?: string;
  roam?: boolean | 'move' | 'scale';
  emphasis?: {
    focus?: string;
    itemStyle?: {
      areaColor: string;
    };
  };
  itemStyle?: {
    areaColor: string;
  } & BorderStyle;
  regions?: { name?: string; itemStyle?: { [x: string]: any } }[];
  zoom?: number;
  center?: number[] | undefined;
} & LabelStyle;

export interface GeoVisualMapStyle {
  type: string;
  seriesIndex: number;
  dimension: undefined | number;
  show: boolean;
  orient: string;
  align: string;
  itemWidth: number;
  itemHeight: number;
  inRange: {
    color: string[];
  };
  text: string[];
  min: number;
  max: number;
  textStyle: FontStyle;
  formatter: (value) => string;
  bottom?: number | string;
  right?: number | string;
  top?: number | string;
  left?: number | string;
}

export interface GeoSeries {
  type: string;
  roam?: boolean;
  map?: string;
  geoIndex?: number;
  emphasis?: {
    label?: {
      show?: boolean;
    };
    disabled?: boolean;
  };
  select?: {
    disabled?: boolean;
  };
  data: Array<{
    rowData: { [key: string]: any };
    name: string;
    value: string;
    visualMap?: boolean;
  }>;
}

export type MetricAndSizeSeriesStyle = {
  data: Array<{
    rowData: { [key: string]: any };
    name: string;
    value: Array<number[] | number | string>;
  }>;
  type: string;
  zlevel: number;
  coordinateSystem: string;
  symbol: string;
  symbolSize: (value: number) => number;
  emphasis: {
    label: {
      show: boolean;
    };
  };
} & LabelStyle;

export interface MapOption {
  geo?: GeoInfo;
  visualMap?: GeoVisualMapStyle[];
  series?: GeoSeries[];
  tooltip?: { trigger: string; formatter: (params: any) => string };
  toolbox?: {
    orient: string;
    top: string;
    feature: {
      [p: string]: {
        onclick: () => void;
        show: boolean;
        icon: string;
        title: any;
      };
    };
    left: string;
    show: boolean;
  };
}
