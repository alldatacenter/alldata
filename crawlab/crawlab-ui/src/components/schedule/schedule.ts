import {computed, readonly, watch} from 'vue';
import {Store} from 'vuex';
import useForm from '@/components/form/form';
import useScheduleService from '@/services/schedule/scheduleService';
import {getDefaultFormComponentData} from '@/utils/form';
import {
  FORM_FIELD_TYPE_INPUT,
  FORM_FIELD_TYPE_INPUT_WITH_BUTTON,
  FORM_FIELD_TYPE_SELECT,
  FORM_FIELD_TYPE_SWITCH,
} from '@/constants/form';
import {parseExpression} from 'cron-parser';
import {getModeOptions} from '@/utils/task';
import useSpider from '@/components/spider/spider';
import {translate} from '@/utils/i18n';

// i18n
const t = translate;

// form component data
const formComponentData = getDefaultFormComponentData<Schedule>();

const useSchedule = (store: Store<RootStoreState>) => {
  // store
  const ns = 'schedule';
  const state = store.state[ns];

  const {
    allListSelectOptions: allSpiderListSelectOptions,
    allDict: allSpiderDict,
  } = useSpider(store);

  // form
  const form = computed<Schedule>(() => state.form);

  // options for default mode
  const modeOptions = getModeOptions();

  // readonly form fields
  const readonlyFormFields = computed<string[]>(() => state.readonlyFormFields);

  // batch form fields
  const batchFormFields = computed<FormTableField[]>(() => [
    {
      prop: 'name',
      label: t('components.schedule.form.name'),
      width: '150',
      fieldType: FORM_FIELD_TYPE_INPUT,
      placeholder: t('components.schedule.form.name'),
      required: true,
    },
    {
      prop: 'spider_id',
      label: t('components.schedule.form.spider'),
      width: '150',
      placeholder: t('components.schedule.form.spider'),
      fieldType: FORM_FIELD_TYPE_SELECT,
      options: allSpiderListSelectOptions.value,
      disabled: () => readonlyFormFields.value.includes('spider_id'),
      required: true,
    },
    {
      prop: 'cron',
      label: t('components.schedule.form.cron'),
      width: '150',
      fieldType: FORM_FIELD_TYPE_INPUT,
      placeholder: t('components.schedule.form.cron'),
      required: true,
    },
    {
      prop: 'cmd',
      label: t('components.schedule.form.command'),
      width: '200',
      placeholder: t('components.schedule.form.command'),
      fieldType: FORM_FIELD_TYPE_INPUT_WITH_BUTTON,
    },
    {
      prop: 'param',
      label: t('components.schedule.form.param'),
      width: '200',
      placeholder: t('components.schedule.form.param'),
      fieldType: FORM_FIELD_TYPE_INPUT_WITH_BUTTON,
    },
    {
      prop: 'mode',
      label: t('components.schedule.form.defaultMode'),
      width: '200',
      fieldType: FORM_FIELD_TYPE_SELECT,
      options: modeOptions,
      required: true,
    },
    {
      prop: 'enabled',
      label: t('components.schedule.form.enabled'),
      width: '80',
      fieldType: FORM_FIELD_TYPE_SWITCH,
      required: true,
    },
  ]);

  // form rules
  const formRules = readonly<FormRules>({
    cron: {
      trigger: 'blur',
      validator: ((_, value: string, callback) => {
        const invalidMessage = t('components.schedule.rules.message.invalidCronExpression');
        if (!value) return callback(invalidMessage);
        if (value.trim().split(' ').length != 5) return callback(invalidMessage);
        try {
          parseExpression(value);
          callback();
        } catch (e: any) {
          callback(e.message);
        }
      }),
    },
  });

  // all schedule select options
  const allScheduleSelectOptions = computed<SelectOption[]>(() => state.allList.map(d => {
    return {
      label: d.name,
      value: d._id,
    };
  }));

  watch(() => form.value?.spider_id, () => {
    if (!form.value?.spider_id) return;
    const spider = allSpiderDict.value.get(form.value?.spider_id);
    if (!spider) return;
    const payload = {...form.value} as Schedule;
    if (spider.cmd) payload.cmd = spider.cmd;
    if (spider.param) payload.param = spider.param;
    if (spider.mode) payload.mode = spider.mode;
    if (spider.node_ids?.length) payload.node_ids = spider.node_ids;
    if (spider.node_tags?.length) payload.node_tags = spider.node_tags;
    store.commit(`${ns}/setForm`, payload);
  });

  return {
    ...useForm('schedule', store, useScheduleService(store), formComponentData),
    modeOptions,
    batchFormFields,
    formRules,
    allScheduleSelectOptions,
  };
};

export default useSchedule;
