import { TWarnSLATableItem } from 'src/type/warning';
import { $http } from '@/http';

export const getSLAById = async (slaId: any) => {
    try {
        const res = (await $http.get<TWarnSLATableItem>(`/sla/${slaId}`)) || {};
        return res;
    } catch (error: any) {
    }
    return {};
};
