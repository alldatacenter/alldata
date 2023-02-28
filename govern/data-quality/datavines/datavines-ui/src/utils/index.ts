import { DV_WORKSPACE_ID, DV_STORAGE_LOGIN } from '@/utils/constants';
import shareData from '@/utils/shareData';

export function pickProps(source: Record<string, any>, props: string[]) {
    const target: Record<string, any> = {};
    props.forEach((propName) => {
        if (Object.prototype.hasOwnProperty.call(source, propName)) {
            target[propName] = source[propName];
        }
    });
    return target;
}

export const download = (blob: any, fileName?: string) => {
    const $fileName = fileName || `log-${new Date().getTime()}.txt`;
    const url = URL.createObjectURL(new Blob([blob], { type: 'application/txt' }));
    const link = document.createElement('a');
    link.style.display = 'none';
    link.href = url;
    link.setAttribute('download', $fileName);
    document.body.appendChild(link);
    link.click();
    URL.revokeObjectURL(url);
};

export const getDefaultWorkspaceId = () => {
    const loginInfo = shareData.sessionGet(DV_STORAGE_LOGIN) || {};
    if (loginInfo.id) {
        const workspaceId = shareData.sessionGet(`${DV_WORKSPACE_ID}_${loginInfo.id}`);
        return workspaceId;
    }
    return undefined;
};
