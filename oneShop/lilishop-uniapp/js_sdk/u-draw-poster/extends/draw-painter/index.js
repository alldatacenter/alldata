export default {
    name: 'painter',
    handle: (dp, option) => {
        dp.canvas.width = option.width;
        dp.canvas.height = option.height;
        dp.draw(async (ctx) => {
            for (let i = 0; i < option.contents.length; i++) {
                ctx.save();
                const drawInfo = option.contents[i];
                const { left = 0, top = 0 } = drawInfo;
                if (drawInfo.type === 'rect') {
                    ctx.fillStyle = drawInfo.background || '#000000';
                    ctx.fillRoundRect(left, top, drawInfo.width, drawInfo.height, drawInfo.radius || 0);
                }
                if (drawInfo.type === 'image') {
                    await ctx.drawImageFit(drawInfo.src, {
                        objectFit: drawInfo.objectFit || 'cover',
                        intrinsicPosition: drawInfo.position || ['center', 'center'],
                        specifiedPosition: [left, top],
                        specifiedSize: {
                            width: drawInfo.width,
                            height: drawInfo.height
                        },
                        radius: drawInfo.radius
                    });
                }
                if (drawInfo.type === 'text') {
                    ctx.fillStyle = drawInfo.color || '#000000';
                    ctx.font = `\
          ${drawInfo.fontStyle || 'normal'} \
          ${drawInfo.fontWeight || 'normal'} \
          ${drawInfo.fontSize || 30} \
          ${drawInfo.fontFamily || 'serial'}\
          `;
                    ctx.fillText(drawInfo.content, left, top, drawInfo.width);
                }
                if (drawInfo.type === 'line-feed-text') {
                    ctx.fillStyle = drawInfo.color || '#000000';
                    ctx.font = `\
          ${drawInfo.fontStyle || 'normal'} \
          ${drawInfo.fontWeight || 'normal'} \
          ${drawInfo.fontSize || 30} \
          ${drawInfo.fontFamily || 'serial'}\
          `;
                    ctx.fillWarpText({
                        x: drawInfo.left,
                        y: drawInfo.top,
                        layer: drawInfo.lineClamp,
                        lineHeight: drawInfo.lineHeight,
                        maxWidth: drawInfo.width,
                        text: drawInfo.content
                    });
                }
                if (drawInfo.type === 'qr-code') {
                    if (typeof ctx.drawQrCode !== 'function') {
                        console.error('--- 当前未引入qr-code扩展, 将自动省略该二维码绘制 ---');
                        return false;
                    }
                    ctx.drawQrCode({
                        x: left,
                        y: top,
                        size: drawInfo.size,
                        text: drawInfo.content,
                        margin: drawInfo.margin || 5,
                        backgroundColor: drawInfo.backgroundColor || '#ffffff',
                        foregroundColor: drawInfo.foregroundColor || '#000000',
                    });
                }
                ctx.restore();
            }
        });
    }
};
