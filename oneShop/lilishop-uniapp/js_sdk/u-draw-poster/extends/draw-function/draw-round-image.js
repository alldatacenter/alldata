/** 绘制圆角图片原型方法 */
export default {
    name: 'drawRoundImage',
    handle: async (canvas, ctx, url, x, y, w, h, r = 15) => {
        var _a;
        ctx.save();
        (_a = ctx.setFillStyle) === null || _a === void 0 ? void 0 : _a.call(ctx, 'transparent');
        ctx.fillStyle = 'transparent';
        ctx.fillRoundRect(x, y, w, h, r);
        ctx.clip();
        const result = await ctx.drawImage(url, x, y, w, h);
        ctx.restore();
        return result;
    }
};
