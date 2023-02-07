import {Directive, ref, h} from 'vue';
import {ExportTypeCsv} from '@/constants/export';
import ExportForm from '@/components/export/ExportForm.vue';
import {sendEvent} from '@/admin/umeng';
import {downloadData, translate} from '@/utils';
import {ElMessageBox, ElNotification, NotificationHandle} from 'element-plus';
import useExportService from '@/services/export/exportService';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';

// i18n
const t = translate;

// export service
const {
  postExport,
  getExport,
  getExportDownload,
} = useExportService();

const getTargetFromBinding = (value: string | Function): string => {
  let target: string;
  if (typeof value === 'string') {
    target = value;
  } else if (typeof value === 'function') {
    target = value();
  } else {
    return '';
  }
  return target;
};

const getConditionsFromBinding = (value?: FilterConditionData[] | Function): FilterConditionData[] => {
  let conditions: FilterConditionData[];
  if (Array.isArray(value)) {
    conditions = value;
  } else if (typeof value === 'function') {
    conditions = value();
  } else {
    return [];
  }
  return conditions;
};

const export_: Directive<HTMLElement, ExportDirective> = {
  mounted(el, binding) {
    const getTarget = (): string => {
      let target: string;
      if (typeof binding.value === 'string' || typeof binding.value === 'function') {
        target = getTargetFromBinding(binding.value);
      } else if (typeof binding.value === 'object') {
        target = getTargetFromBinding(binding.value.target);
      } else {
        return '';
      }
      return target;
    };

    const getConditions = (): FilterConditionData[] => {
      let conditions: FilterConditionData[] = [];
      if (typeof binding.value === 'object') {
        conditions = getConditionsFromBinding(binding.value.conditions);
      } else {
        return [];
      }
      return conditions;
    };

    // export cache
    const exportCache = new Map<string, Export>();

    // notifications
    const notifications = new Map<string, NotificationHandle>();

    // export polling interval cache
    const exportPollingIntervalCache = new Map<string, any>();

    // export type
    const exportType = ref<ExportType>(ExportTypeCsv);

    const pollAndDownload = async (exportId: string) => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      let exp = exportCache.get(exportId);
      if (!exp) {
        exportPollingIntervalCache.delete(exportId);
        return;
      }
      const res = await getExport(exp?.type, exportId);
      exp = res.data;
      if (exp?.status === 'running') {
        await pollAndDownload(exportId);
      } else {
        const n = notifications.get(exportId);
        const dataDownload = await getExportDownload(exportType.value, exportId);
        downloadData(dataDownload, exp?.file_name as string);
        n?.close();
        return;
      }
    };

    // on click export
    const onClickExport = async () => {
      // prompt export message box
      await ElMessageBox({
        title: t('common.actions.export'),
        message: h(ExportForm, {
          target: 'results_add_data',
          defaultType: exportType.value,
          onExportTypeChange: (value: ExportType) => {
            exportType.value = value;
          },
        }),
        boxType: 'prompt',
        showCancelButton: true,
        confirmButtonText: t('common.actions.export'),
        cancelButtonText: t('common.actions.cancel'),
      });

      // perform export
      const res = await postExport(exportType.value, getTarget(), getConditions());
      const exportId = res.data;
      if (!exportId) return;
      exportCache.set(exportId, {
        id: exportId,
        status: 'running',
        type: exportType.value,
      });

      // notification
      const notification = ElNotification({
        title: t('components.export.status.exporting'),
        message: h('div',
          {
            style: {
              display: 'flex',
              alignItems: 'center',
            },
          },
          [
            h(FontAwesomeIcon, {
              class: 'fa-spin',
              icon: ['fa', 'spinner'],
              spinning: true,
              style: {
                width: '14px',
                height: '14px',
                marginRight: '8px',
              }
            }),
            t('span', t(`components.export.exporting.${exportType.value}`)),
          ],
        ),
        duration: 0,
        showClose: false,
      });
      notifications.set(exportId, notification);

      // poll and download
      await pollAndDownload(exportId);

      sendEvent('click_spider_detail_actions_export');
    };

    el.addEventListener('click', onClickExport);
  }
};

export default export_;
