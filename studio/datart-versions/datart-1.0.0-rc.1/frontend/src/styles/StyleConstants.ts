// spacing
export const SPACE_UNIT = 4;
export const SPACE_TIMES = (multiple?: number) =>
  `${SPACE_UNIT * (multiple || 1)}px`;
export const SPACE = SPACE_TIMES(1); // 4
export const SPACE_XS = SPACE_TIMES(2); // 8
export const SPACE_SM = SPACE_TIMES(3); // 12
export const SPACE_MD = SPACE_TIMES(4); // 16
export const SPACE_LG = SPACE_TIMES(5); // 20
export const SPACE_XL = SPACE_TIMES(9); // 36 = 16 + 20
export const SPACE_XXL = SPACE_TIMES(14); // 56 = 20 + 36

// z-index
export const MINUS_LEVEL_1 = -1;
export const LEVEL_1 = 1;
export const LEVEL_5 = 5;
export const LEVEL_10 = 10;
export const LEVEL_20 = 20;
export const LEVEL_50 = 50;
export const LEVEL_100 = 100;
export const LEVEL_1000 = 1000;

export const LEVEL_DASHBOARD_EDIT_OVERLAY = LEVEL_50;

// base color
export const BLUE = '#1B9AEE';
export const GREEN = '#15AD31';
export const ORANGE = '#FA8C15';
export const YELLOW = '#FAD414';
export const RED = '#E62412';

/* gray
 *
 * as reference
 * G10 - body background
 * G20 - split line light
 * G30 - split line dark
 * G40 - border
 * G50 - disabled font
 * G60 - light font
 * G70 - secondary font
 * G80 - font
 */
export const WHITE = '#FFFFFF';
export const G10 = '#F5F8FA';
export const G20 = '#EFF2F5';
export const G30 = '#E4E6EF';
export const G40 = '#B5B5C3';
export const G50 = '#A1A5B7';
export const G60 = '#7E8299';
export const G70 = '#5E6278';
export const G80 = '#3F4254';
export const G90 = '#181C32';
export const BLACK = '#000000';

export const DG10 = '#1b1b29';
export const DG20 = '#2B2B40';
export const DG30 = '#323248';
export const DG40 = '#474761';
export const DG50 = '#565674';
export const DG60 = '#6D6D80';
export const DG70 = '#92929F';
export const DG80 = '#CDCDDE';
export const DG90 = '#FFFFFF';

// theme color
export const PRIMARY = BLUE;
export const INFO = PRIMARY;
export const SUCCESS = GREEN;
export const PROCESSING = BLUE;
export const ERROR = RED;
export const HIGHLIGHT = RED;
export const WARNING = ORANGE;
export const NORMAL = G40;

// font
export const FONT_SIZE_BASE = 16;

export const FONT_SIZE_LABEL = `${FONT_SIZE_BASE * 0.75}px`;
export const FONT_SIZE_SUBTITLE = `${FONT_SIZE_BASE * 0.8125}px`;
export const FONT_SIZE_BODY = `${FONT_SIZE_BASE * 0.875}px`;
export const FONT_SIZE_SUBHEADING = `${FONT_SIZE_BASE * 0.9375}px`;
export const FONT_SIZE_TITLE = `${FONT_SIZE_BASE}px`;
export const FONT_SIZE_HEADING = `${FONT_SIZE_BASE * 1.125}px`;
export const FONT_SIZE_ICON_SM = `${FONT_SIZE_BASE * 1.25}px`;
export const FONT_SIZE_ICON_MD = `${FONT_SIZE_BASE * 1.5}px`;
export const FONT_SIZE_ICON_LG = `${FONT_SIZE_BASE * 1.75}px`;
export const FONT_SIZE_ICON_XL = `${FONT_SIZE_BASE * 2}px`;
export const FONT_SIZE_ICON_XXL = `${FONT_SIZE_BASE * 2.25}px`;

export const LINE_HEIGHT_LABEL = `${FONT_SIZE_BASE * 1.25}px`;
export const LINE_HEIGHT_BODY = `${FONT_SIZE_BASE * 1.375}px`;
export const LINE_HEIGHT_TITLE = `${FONT_SIZE_BASE * 1.5}px`;
export const LINE_HEIGHT_HEADING = `${FONT_SIZE_BASE * 1.625}px`;
export const LINE_HEIGHT_ICON_SM = `${FONT_SIZE_BASE * 1.75}px`;
export const LINE_HEIGHT_ICON_MD = `${FONT_SIZE_BASE * 2}px`;
export const LINE_HEIGHT_ICON_LG = `${FONT_SIZE_BASE * 2.25}px`;
export const LINE_HEIGHT_ICON_XL = `${FONT_SIZE_BASE * 2.5}px`;
export const LINE_HEIGHT_ICON_XXL = `${FONT_SIZE_BASE * 2.75}px`;

export const FONT_FAMILY =
  '-apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji"';
export const CODE_FAMILY =
  '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace';
export const FONT_WEIGHT_LIGHT = 300;
export const FONT_WEIGHT_REGULAR = 400;
export const FONT_WEIGHT_MEDIUM = 500;
export const FONT_WEIGHT_BOLD = 600;

// border

export const BORDER_RADIUS = '4px';
