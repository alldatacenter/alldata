import * as dfucs from "./extends/draw-function/index";
import DrawPoster from "./draw-poster";
import drawQrCode from "./extends/draw-qr-code/index";
import createFromList from './extends/create-from-list/index';
import drawPainter from './extends/draw-painter/index';
DrawPoster.useCtx(dfucs.drawImage);
DrawPoster.useCtx(dfucs.fillWarpText);
DrawPoster.useCtx(dfucs.roundRect);
DrawPoster.useCtx(dfucs.fillRoundRect);
DrawPoster.useCtx(dfucs.strokeRoundRect);
DrawPoster.useCtx(dfucs.drawRoundImage);
DrawPoster.useCtx(dfucs.drawImageFit);
const useDrawPoster = async (options) => {
    const dp = await DrawPoster.build(options);
    return dp;
};
const useDrawPosters = async (optionsAll) => {
    const dps = await DrawPoster.buildAll(optionsAll);
    return dps;
};
export { DrawPoster, useDrawPoster, useDrawPosters, drawQrCode, drawPainter, createFromList };
export default DrawPoster;
