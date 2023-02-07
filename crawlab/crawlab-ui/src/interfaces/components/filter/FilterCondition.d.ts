export declare global {
  interface FilterConditionProps {
    condition?: FilterConditionData;
  }

  interface FilterConditionData {
    key?: string;
    op?: string;
    value?: any;
  }
}
