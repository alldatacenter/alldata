import { DrawPosterBuildOpts } from "./utils/interface";
import DrawPoster from "./draw-poster";
import drawQrCode from "./extends/draw-qr-code/index";
import createFromList from './extends/create-from-list/index';
import drawPainter from './extends/draw-painter/index';
declare const useDrawPoster: (options: string | DrawPosterBuildOpts) => Promise<DrawPoster & import("./utils/interface").drawPosterExtends>;
declare const useDrawPosters: (optionsAll: (string | DrawPosterBuildOpts)[]) => Promise<{
    [key: string]: DrawPoster & import("./utils/interface").drawPosterExtends;
}>;
export { DrawPoster, useDrawPoster, useDrawPosters, drawQrCode, drawPainter, createFromList };
export default DrawPoster;
