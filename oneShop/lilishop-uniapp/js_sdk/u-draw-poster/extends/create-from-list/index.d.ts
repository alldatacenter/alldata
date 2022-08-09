import { DrawPosterUseOpts } from '../../utils/interface';
export interface CreateLayerOpts {
    background?: string;
    self?: boolean;
    line?: boolean;
    lineHeight?: number;
}
export interface DrawRowOpt {
    text?: string;
    font?: string;
    color?: string;
    center?: boolean;
    width?: number;
}
declare const _default: DrawPosterUseOpts;
/** 绘制表单扩展方法 */
export default _default;
