export declare global {
  interface Setting extends BaseModel {
    key: string;
    value: { [key: string]: string };
  }
}
