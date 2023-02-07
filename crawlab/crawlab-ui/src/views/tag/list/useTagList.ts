import {useStore} from 'vuex';
import {computed, h} from 'vue';
import useList from '@/layouts/content/list/list';
import NavLink from '@/components/nav/NavLink.vue';
import ColorPicker from '@/components/color/ColorPicker.vue';
import {getActionColumn} from '@/utils/table';
import {ACTION_ADD, ACTION_DELETE, ACTION_VIEW} from '@/constants/action';
import Tag from '@/components/tag/Tag.vue';
import {translate} from '@/utils/i18n';
import {sendEvent} from '@/admin/umeng';

// i18n
const t = translate;

const useTagList = () => {
  // store
  const ns = 'tag';
  const store = useStore<RootStoreState>();
  const {commit} = store;

  // nav actions
  const navActions = computed<ListActionGroup[]>(() => [
    {
      name: 'common',
      children: [
        {
          action: ACTION_ADD,
          id: 'add-btn',
          className: 'add-btn',
          buttonType: 'label',
          label: t('views.tags.navActions.new.label'),
          tooltip: t('views.tags.navActions.new.tooltip'),
          icon: ['fa', 'plus'],
          type: 'success',
          onClick: () => {
            commit(`${ns}/showDialog`, 'create');

            sendEvent('click_tag_list_new');
          }
        }
      ]
    }
  ]);

  // table columns
  const tableColumns = computed<TableColumns<Tag>>(() => [
    {
      key: 'name',
      label: t('views.tags.table.columns.name'),
      icon: ['fa', 'font'],
      width: '160',
      align: 'left',
      value: (row: Tag) => h(NavLink, {
        path: `/tags/${row._id}`,
        label: row.name,
      }),
    },
    {
      key: 'color',
      label: t('views.tags.table.columns.color'),
      icon: ['fa', 'palette'],
      width: '120',
      value: ({color}: Tag) => {
        return h(ColorPicker, {
          modelValue: color,
          disabled: true,
        });
      }
    },
    {
      key: 'col',
      label: t('views.tags.table.columns.model'),
      icon: ['fa', 'table'],
      width: '120',
      // value: ({color}: Tag) => {
      //   return h(Tag, {
      //     modelValue: color,
      //     disabled: true,
      //   });
      // }
    },
    {
      key: 'description',
      label: t('views.tags.table.columns.description'),
      icon: ['fa', 'comment-alt'],
      width: 'auto',
    },
    getActionColumn('/tags', ns, [ACTION_VIEW, ACTION_DELETE]),
  ]);

  // options
  const opts = {
    navActions,
    tableColumns,
  } as UseListOptions<Tag>;

  return {
    ...useList<Tag>(ns, store, opts),
  };
};

export default useTagList;
