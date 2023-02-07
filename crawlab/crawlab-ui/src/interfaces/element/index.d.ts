export declare global {
  type BasicType = '' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'plain';
  type BasicEffect = 'dark' | 'light' | 'plain';
  type BasicSize = 'mini' | 'small' | 'medium' | 'large';

  type ElFormValidator = (rule: any, value: any, callback: any) => void;

  interface ElFormRule {
    required: boolean;
    trigger: string;
    validator: ElFormValidator;
  }
}
