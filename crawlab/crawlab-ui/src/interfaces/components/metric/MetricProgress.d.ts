interface MetricProgressProps {
  percentage?: number;
  type?: MetricProgressType;
  strokeWidth?: number;
  textInside?: boolean;
  intermediate?: boolean;
  duration?: number;
  width?: number;
  showText?: boolean;
  strokeLinecap?: MetricProgressStrokeLinecap;
  format?: MetricProgressFormat;
  label?: MetricProgressLabel | string;
  labelIcon?: Icon;
  detailMetrics?: SelectOption[];
  status?: MetricProgressStatus;
}

type MetricProgressType = 'line' | 'circle' | 'dashboard';
type MetricProgressStrokeLinecap = 'butt' | 'round' | 'square';
type MetricProgressFormat = (percentage: number | null) => string;
type MetricProgressStatus =
  MetricProgressStatusData
  | ((percentage: number | null) => MetricProgressStatusData | undefined);

interface MetricProgressLabel {
  key?: string;
  title?: string;
}

interface MetricProgressStatusData {
  color?: string;
  icon?: Icon;
  label?: string;
}
