/** 绘制填充圆角矩形方法 */
export default {
    name: 'strokeRoundRect',
    handle: (canvas, ctx, x, y, w, h, r) => {
        ctx.roundRect(x, y, w, h, r, false, true);
    }
};
