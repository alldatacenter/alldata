export const setTheme = (themeObj: Record<string, string>) => {
    const vars = Object.keys(themeObj).map((key) => `--${key}:${themeObj[key]}`).join(';');
    document.documentElement.setAttribute('style', vars);
};

export const useSetTheme = () => () => {
    setTheme({});
};
