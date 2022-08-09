import { Canvas, DrawPosterCanvasCtx, CreateImagePathOptions, DrawPosterBuildOpts, DrawPosterUseOpts, drawPosterExtends, DrawPosterUseCtxOpts } from "./utils/interface";
declare type DrawPosterInstanceType = InstanceType<typeof DrawPoster> & drawPosterExtends;
declare class DrawPoster {
    canvas: Canvas;
    ctx: DrawPosterCanvasCtx;
    canvasId: string;
    loading: boolean;
    debugging: boolean;
    loadingText: string;
    createText: string;
    [key: string]: any;
    private executeOnions;
    private stopStatus;
    private drawType;
    /** 构建器, 构建返回当前实例, 并挂载多个方法 */
    constructor(canvas: Canvas, ctx: DrawPosterCanvasCtx, canvasId: string, loading: boolean, debugging: boolean, loadingText: string, createText: string, tips: boolean);
    /** 提示器, 传入消息与数据 */
    private debuggingLog;
    /** 传入挂载配置对象, 添加扩展方法 */
    static use: (opts: DrawPosterUseOpts) => void;
    /** 传入挂载配置对象, 添加绘画扩展方法 */
    static useCtx: (opts: DrawPosterUseCtxOpts) => void;
    /** 构建绘制海报矩形方法, 传入canvas选择器或配置对象, 返回绘制对象 */
    static build: (options: string | DrawPosterBuildOpts, tips?: boolean) => Promise<DrawPosterInstanceType>;
    /** 构建多个绘制海报矩形方法, 传入选择器或配置对象的数组, 返回多个绘制对象 */
    static buildAll: (optionsAll: (string | DrawPosterBuildOpts)[]) => Promise<{
        [key: string]: DrawPosterInstanceType;
    }>;
    /** 绘制器, 接收执行器函数, 添加到绘制容器中 */
    draw: (execute: (ctx: DrawPosterCanvasCtx) => Promise<any> | void) => void;
    /** 等待创建绘画, 成功后清空绘制器容器 */
    awaitCreate: () => Promise<boolean[]>;
    /** 创建canvas本地地址 @returns {string} 本地地址 */
    createImagePath: (baseOptions?: CreateImagePathOptions) => Promise<string>;
    /** 停止当前绘画, 调用则停止当前绘画堆栈的绘画 */
    stop: () => void;
}
export default DrawPoster;
