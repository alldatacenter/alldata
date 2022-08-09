export declare type ObjectFit = "contain" | "cover";
export declare type ObjectPosition = ["left" | "center" | "right", "top" | "center" | "bottom"];
export interface Size {
    width: number;
    height: number;
}
/**
 * 用于计算图片的宽高比例
 * @see https://drafts.csswg.org/css-images-3/#sizing-terms
 *
 * ## 名词解释
 * ### intrinsic dimensions
 * 图片本身的尺寸
 *
 * ### specified size
 * 用户指定的元素尺寸
 *
 * ### concrete object size
 * 应用了 `objectFit` 之后图片的显示尺寸
 *
 * ### default object size
 */
export declare function calculateConcreteRect(style: {
    /** @see https://developer.mozilla.org/zh-CN/docs/Web/CSS/object-fit */
    objectFit?: ObjectFit;
    /** @see https://developer.mozilla.org/zh-CN/docs/Web/CSS/object-position */
    intrinsicPosition?: ObjectPosition;
    specifiedPosition?: [number, number];
}, intrinsicSize: Size, specifiedSize: Size): {
    sx: number;
    sy: number;
    sw: number;
    sh: number;
    dx: number;
    dy: number;
    dw: number;
    dh: number;
};
