import useRequest from '@/services/request';

const {
  get,
  post,
} = useRequest();

const useExportService = () => {
  const postExport = async (type: ExportType, target: string, conditions?: FilterConditionData[]) => {
    return await post<string>(`/export/${type}`, undefined, {
      target,
      conditions: JSON.stringify(conditions || ''),
    });
  };

  const getExport = async (type: ExportType, id: string) => {
    return await get(`/export/${type}/${id}`);
  };

  const getExportDownload = async (type: ExportType, id: string) => {
    return await get(`/export/${type}/${id}/download`) as string;
  };

  return {
    postExport,
    getExport,
    getExportDownload,
  };
};

export default useExportService;
