export interface WordCloudConfig {
  drawOutOfBound: boolean;
  shape: string;
  width: string;
  height: string;
  left: string;
  top: string;
  right: string;
  bottom: string;
}

export interface WordCloudLabelConfig {
  sizeRange: number[];
  rotationRange: number[];
  rotationStep: number;
  gridSize: number;
  emphasis: {
    focus: string;
    textStyle: {
      textShadowBlur: number;
      textShadowColor: string;
    };
  };
  data?:
    | {
        name: string;
        rowData: { [p: string]: any };
        textStyle: { opacity?: number; [p: string]: any };
        value: string;
      }[]
    | undefined;
}
