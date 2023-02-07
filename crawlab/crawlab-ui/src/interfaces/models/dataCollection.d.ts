export declare global {
  interface DataCollection extends BaseModel {
    name?: string;
    fields?: DataField[];
    dedup?: {
      enabled?: boolean;
      keys?: string[];
      type?: string;
    };
  }
}
