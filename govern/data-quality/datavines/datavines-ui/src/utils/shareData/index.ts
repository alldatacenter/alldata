const data: Record<string, any> = {};
const set = (key: string, val: any) => {
    data[key] = val;
};
const get = (key: string) => data[key];

const sessionSet = (key: string, val: any) => {
    const startTime = new Date().getTime();
    sessionStorage.setItem(key, JSON.stringify({ data: val, st: startTime }));
};

const sessionGet = <T = any>(key: string): T => {
    const val = window.sessionStorage.getItem(key);
    try {
        const parse = JSON.parse(val as any);
        return parse ? parse.data : undefined;
    } catch (err) {
        return undefined as any;
    }
};

const storageSet = (key: string, val: any) => {
    const startTime = new Date().getTime();
    localStorage.setItem(key, JSON.stringify({ data: val, st: startTime }));
};

const storageGet = <T = any>(key: string): T => {
    const val = localStorage.getItem(key);
    try {
        const parse = JSON.parse(val as any);
        return parse ? parse.data : undefined;
    } catch (err) {
        console.log(err);
        return undefined as any;
    }
};

const storageGetByTime = <T = any>(key: string, exp: number): T => {
    const val = localStorage.getItem(key);
    const parse = JSON.parse(val as any);
    if (parse) {
        if ((new Date().getTime() - parse.st) >= exp * 24 * 60 * 60 * 1000) {
            return undefined as any;
        }
        return parse.data || undefined;
    }
    return undefined as any;
};

const sessionGetByTime = <T = any>(key: string, exp: number): T => {
    const val = sessionStorage.getItem(key);
    const parse = JSON.parse(val as any);
    if (parse) {
        if ((new Date().getTime() - parse.st) >= exp * 60 * 1000) {
            return undefined as any;
        }
        return parse.data || undefined;
    }
    return undefined as any;
};
export default {
    set,
    get,
    sessionSet,
    sessionGet,
    storageSet,
    storageGet,
    storageGetByTime,
    sessionGetByTime,
};
