/// <reference types="@dcloudio/types" />
import DrawPoster from "../draw-poster";
import { ImageFitOption } from '../extends/draw-function/draw-image-fit';
import { CreateLayerOpts, DrawRowOpt } from "../extends/create-from-list";
import { PainterContainerOption } from "../extends/draw-painter";
/** 绘制容器 */
export declare type Execute = Array<() => Promise<boolean>>;
export interface drawPosterExtends {
    from: {
        height: number;
        padding: number;
        margin: number;
    };
    createLayer: (afferOpts: CreateLayerOpts, rowList: DrawRowOpt[]) => number;
    setFromOptions: (opts: Partial<{
        height: number;
        padding: number;
        margin: number;
    }>) => void;
    gcanvas: {
        WeexBridge: any;
        Image: any;
        enable: (el: any, options: {
            bridge?: any;
            debug?: boolean;
            disableAutoSwap?: any;
            disableComboCommands?: any;
        }) => Canvas;
    };
    painter: (option: PainterContainerOption) => void;
}
/** 构建器配置 */
export interface DrawPosterBuildOpts {
    /** 查询选择器; 注意不需要加# */
    selector: string;
    /** 选取组件范围 */
    componentThis?: any;
    /** 绘制类型为2d绘制, 默认开启, 在微信小程序的时候动态加载 */
    type2d?: boolean;
    /** 是否在绘制时进行加载提示 */
    loading?: boolean;
    /** 当存在绘制图片时, 等待绘画完毕的时间（秒）仅App中生效
     *
     *  具体查看文档说明：https://github.com/TuiMao233/uni-draw-poster
     */
    drawImageTime?: number;
    /** 是否开启调试模式 */
    debugging?: boolean;
    /** 加载提示文字 */
    loadingText?: string;
    /** 创建图片提示文字 */
    createText?: string;
    /** 是否启动gcanvas(nvue) */
    gcanvas?: boolean;
}
/** 绘制换行配置 */
export interface FillWarpTextOpts {
    text: string;
    maxWidth?: number;
    lineHeight?: number;
    layer?: number;
    x?: number;
    y?: number;
    splitText?: string;
    notFillText?: boolean;
}
/** 绘制二维码配置 */
export interface DrawQrCodeOpts {
    text: string;
    x?: number;
    y?: number;
    size?: number;
    margin?: number;
    backgroundColor?: string;
    foregroundColor?: string;
}
/** 绘制换行, 单行信息 */
export interface FillWarpTextItemInfo {
    text: string;
    y: number;
    x: number;
}
/** 绘制画笔 */
export interface DrawPosterCanvasCtx extends UniApp.CanvasContext {
    [key: string]: any;
    createImageData: () => ImageData;
    textAlign: CanvasTextDrawingStyles["textAlign"];
    textBaseline: CanvasTextDrawingStyles["textBaseline"];
    transform: CanvasTransform["transform"];
    /** 绘制图片原型 */
    drawImageProto: UniApp.CanvasContext['drawImage'];
    /** 当前绘制类型 */
    drawType: 'context' | 'type2d';
    /** 等待绘制图片
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    drawImage(url: string, dx?: number | undefined, dy?: number | undefined, dWidth?: number | undefined, dHeigt?: number | undefined, sx?: number | undefined, sy?: number | undefined, sWidth?: number | undefined, sHeight?: number | undefined): Promise<boolean>;
    /** 绘制圆角图片
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    drawRoundImage(url: string, x: number, y: number, w: number, h: number, r?: number): Promise<boolean>;
    /** 绘制 Object-Fit 模式图片
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    drawImageFit(url: string, opts?: ImageFitOption): Promise<boolean>;
    /** 绘制换行字体
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    fillWarpText(options: FillWarpTextOpts): Array<FillWarpTextItemInfo>;
    /** 绘制圆角矩形（原型）
     *
     */
    roundRect(x: number, y: number, w: number, h: number, r: number, fill?: boolean, stroke?: boolean): void;
    /** 绘制圆角矩形（填充）
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    fillRoundRect(x: number, y: number, w: number, h: number, r: number): void;
    /** 绘制圆角矩形（边框）
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    strokeRoundRect(x: number, y: number, w: number, h: number, r: number): void;
    /** 绘制二维码
     *
     * 说明文档: https://tuimao233.gitee.io/mao-blog/my-extends/u-draw-poste
     */
    drawQrCode(options: DrawQrCodeOpts): void;
}
/** Canvas2d实例 */
export interface Canvas {
    width: number;
    height: number;
    getContext(contextType: "2d" | "webgl"): DrawPosterCanvasCtx | WebGLRenderingContext;
    createImage(): {
        src: string;
        width: number;
        height: number;
        onload: () => void;
        onerror: () => void;
    };
    requestAnimationFrame(callback: Function): number;
    cancelAnimationFrame(requestID: number): void;
    createImageData(): ImageData;
    createPath2D(path: Path2D): Path2D;
    toDataURL(type: string, encoderOptions: number): string;
}
/** 创建图片路径配置项 */
export interface CreateImagePathOptions {
    x?: number;
    y?: number;
    width?: number;
    height?: number;
    destWidth?: number;
    destHeight?: number;
}
/** 绘制实例扩展配置 */
export interface DrawPosterUseOpts {
    name: string;
    init?: (dp: InstanceType<typeof DrawPoster>) => void;
    handle: (dp: InstanceType<typeof DrawPoster>, ...args: any[]) => any;
    createImage?: (dp: InstanceType<typeof DrawPoster>) => void;
    [key: string]: any;
}
/** 绘制画笔实例扩展配置 */
export interface DrawPosterUseCtxOpts {
    name: string;
    init?: (canvas: Canvas, ctx: DrawPosterCanvasCtx) => void;
    handle: (canvas: Canvas, ctx: DrawPosterCanvasCtx, ...args: any[]) => any;
    [key: string]: any;
}
