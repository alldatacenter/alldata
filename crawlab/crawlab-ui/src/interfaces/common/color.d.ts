export declare global {
  type Color =
    'red'
    | 'magenta'
    | 'purple'
    | 'geekBlue'
    | 'blue'
    | 'cyan'
    | 'green'
    | 'limeGreen'
    | 'yellow'
    | 'gold'
    | 'orange';

  type ColorFunc = (m: MetricMeta) => string;
}
