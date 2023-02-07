export const getPredefinedColors = (): string[] => {
  return Object.values(getColors());
};

export const getColors = (): { [key: string]: string } => {
  return {
    'white': '#ffffff',
    'grey-1': '#ffffff',
    'grey-2': '#fafafa',
    'grey-3': '#f5f5f5',
    'grey-4': '#f0f0f0',
    'grey-5': '#d9d9d9',
    'grey-6': '#bfbfbf',
    'grey-7': '#8c8c8c',
    'grey-8': '#595959',
    'grey-9': '#434343',
    'grey-10': '#262626',
    'grey-11': '#1f1f1f',
    'grey-12': '#141414',
    'grey-13': '#000000',
    'black': '#000000',
    'red': '#f56c6c',
    'magenta': '#f759ab',
    'purple': '#9254de',
    'geek-blue': '#597ef7',
    'blue': '#409eff',
    'cyan': '#36cfc9',
    'green': '#67c23a',
    'lime-green': '#bae637',
    'yellow': '#ffec3d',
    'gold': '#ffc53d',
    'orange': '#e6a23c',
    'primary': '#409eff',
    'primary-plain': 'rgb(217, 236, 255)',
    'success': '#67c23a',
    'success-plain': 'rgb(225, 243, 216)',
    'warning': '#e6a23c',
    'warning-plain': 'rgb(250, 236, 216)',
    'danger': '#f56c6c',
    'danger-plain': 'rgb(253, 226, 226)',
    'info': '#303133',
    'info-medium': '#909399',
    'info-medium-light': 'rgb(200, 200, 205)',
    'info-light': 'rgb(233, 233, 235)',
    'info-plain': 'rgb(244, 244, 245)',
    'info-border': '#e6e6e6',
  };
};

export const getColorByKey = (key: string): string => {
  return getColors()[key];
};
