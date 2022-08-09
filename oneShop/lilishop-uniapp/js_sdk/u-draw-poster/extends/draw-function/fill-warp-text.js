/** 绘制换行字体原型方法 */
export default {
    name: 'fillWarpText',
    handle: (canvas, ctx, config) => {
        const newConfig = config = Object.assign({ maxWidth: 100, layer: 2, lineHeight: Number(ctx.font.replace(/[^0-9.]/g, '')), x: 0, y: Number(ctx.font.replace(/[^0-9.]/g, '')) / 1.2, splitText: '', notFillText: false }, config);
        const { text, splitText, maxWidth, layer, lineHeight, notFillText, x, y } = newConfig;
        // 当字符串为空时, 抛出错误
        if (!text) {
            throw Error('warpFillText Error: text is empty string');
        }
        // 分割所有单个字符串
        const chr = text.split(splitText);
        // 存入的每行字体的容器
        let row = [];
        // 判断字符串
        let timp = '';
        if (splitText) {
            row = chr;
        }
        else {
            // 遍历所有字符串, 填充行容器
            for (let i = 0; i < chr.length; i++) {
                // 当超出行列时, 停止执行遍历, 节省计算时间
                if (row.length > layer) {
                    break;
                }
                if (ctx.measureText(timp).width < maxWidth) {
                    // 如果超出长度, 添加进row数组
                    timp += chr[i];
                }
                else {
                    // 如超出一行长度, 则换行, 并清除容器
                    i--;
                    row.push(timp);
                    timp = '';
                }
            }
            // 如有剩下字体, 则在最后时添加一行
            if (timp) {
                row.push(timp);
            }
            // 如果数组长度大于指定行数
            if (row.length > layer) {
                row = row.slice(0, layer);
                // 结束的索引
                const end = layer - 1;
                for (let i = 0; i < row[end].length; i++) {
                    const currentWidth = ctx.measureText(`${row[end]}...`).width;
                    if (currentWidth > maxWidth) {
                        // 加上... 当前宽度大于最大宽度时, 去除一位字符串
                        const strEnd = row[end].length - 1;
                        row[end] = row[end].slice(0, strEnd);
                    }
                    else {
                        row[end] += '...';
                        break;
                    }
                }
            }
        }
        // 储存并返回绘制信息
        const drawInfos = row.map((item, index) => {
            const info = {
                text: item,
                y: y + index * lineHeight,
                x: x,
            };
            // 默认执行绘制信息
            if (!notFillText) {
                ctx.fillText(info.text, info.x, info.y);
            }
            return info;
        });
        return drawInfos;
    }
};
