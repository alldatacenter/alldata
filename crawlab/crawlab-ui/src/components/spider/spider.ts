import {useRoute} from 'vue-router';
import {computed} from 'vue';
import {Store} from 'vuex';
import useForm from '@/components/form/form';
import useSpiderService from '@/services/spider/spiderService';
import {getDefaultFormComponentData} from '@/utils/form';
import {
  FORM_FIELD_TYPE_INPUT,
  FORM_FIELD_TYPE_INPUT_TEXTAREA,
  FORM_FIELD_TYPE_INPUT_WITH_BUTTON,
  FORM_FIELD_TYPE_SELECT
} from '@/constants/form';
import useProject from '@/components/project/project';
import useRequest from '@/services/request';
import {FILTER_OP_CONTAINS} from '@/constants/filter';
import {getModeOptions} from '@/utils/task';
import {translate} from '@/utils/i18n';

const {
  getList,
} = useRequest();

// form component data
const formComponentData = getDefaultFormComponentData<Spider>();

const useSpider = (store: Store<RootStoreState>) => {
  // i18n
  const t = translate;

  // options for default mode
  const modeOptions = getModeOptions();

  // use project
  const {
    allProjectSelectOptions,
  } = useProject(store);

  // batch form fields
  const batchFormFields = computed<FormTableField[]>(() => [
    {
      prop: 'name',
      label: t('components.spider.form.name'),
      width: '150',
      placeholder: t('components.spider.form.name'),
      fieldType: FORM_FIELD_TYPE_INPUT,
      required: true,
    },
    {
      prop: 'cmd',
      label: t('components.spider.form.command'),
      width: '200',
      placeholder: t('components.spider.form.command'),
      fieldType: FORM_FIELD_TYPE_INPUT_WITH_BUTTON,
      required: true,
    },
    {
      prop: 'param',
      label: t('components.spider.form.param'),
      width: '200',
      placeholder: t('components.spider.form.param'),
      fieldType: FORM_FIELD_TYPE_INPUT_WITH_BUTTON,
    },
    {
      prop: 'mode',
      label: t('components.spider.form.defaultMode'),
      width: '200',
      fieldType: FORM_FIELD_TYPE_SELECT,
      options: modeOptions,
      required: true,
    },
    {
      prop: 'project_id',
      label: t('components.spider.form.project'),
      width: '200',
      fieldType: FORM_FIELD_TYPE_SELECT,
      options: allProjectSelectOptions.value,
    },
    {
      prop: 'description',
      label: t('components.spider.form.description'),
      width: '200',
      fieldType: FORM_FIELD_TYPE_INPUT_TEXTAREA,
    },
  ]);

  // route
  const route = useRoute();

  // spider id
  const id = computed(() => route.params.id);

  // fetch data collections
  const fetchDataCollection = async (query: string) => {
    const conditions = [{
      key: 'name',
      op: FILTER_OP_CONTAINS,
      value: query,
    }] as FilterConditionData[];
    const res = await getList(`/data/collections`, {conditions});
    return res.data;
  };

  // fetch data collection suggestions
  const fetchDataCollectionSuggestions = (query: string, cb: Function) => {
    fetchDataCollection(query)
      .then(data => {
        cb(data?.map((d: DataCollection) => {
          return {
            _id: d._id,
            value: d.name,
          };
        }));
      });
  };

  return {
    ...useForm('spider', store, useSpiderService(store), formComponentData),
    batchFormFields,
    id,
    modeOptions,
    fetchDataCollection,
    fetchDataCollectionSuggestions,
  };
};

export default useSpider;
