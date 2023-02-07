<template>
  <div class="form-table">
    <cl-table
        :columns="columns"
        :data="data"
        hide-footer
    />
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, h, inject, PropType, Ref} from 'vue';
import {emptyArrayFunc} from '@/utils/func';
import FormTableField from '@/components/form/FormTableField.vue';
import {TABLE_COLUMN_NAME_ACTIONS} from '@/constants/table';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'FormTable',
  props: {
    data: {
      type: Array as PropType<TableData>,
      required: false,
      default: emptyArrayFunc,
    },
    fields: {
      type: Array as PropType<FormTableField[]>,
      required: false,
      default: emptyArrayFunc,
    }
  },
  emits: [
    'add',
    'clone',
    'delete',
    'field-change',
    'field-register',
  ],
  setup(props: FormTableProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const columns = computed<TableColumns>(() => {
      const {fields} = props;
      const formRules = inject<FormRuleItem[]>('form-rules');
      const columns = fields.map(f => {
        const {
          prop,
          label,
          width,
          fieldType,
          options,
          required,
          placeholder,
          disabled,
        } = f;
        return {
          key: prop,
          label,
          width,
          required,
          value: (row: BaseModel, rowIndex: number) => h(FormTableField, {
            form: row,
            formRules,
            prop,
            fieldType,
            options,
            required,
            placeholder,
            disabled: typeof disabled === 'function' ? disabled(row) : disabled,
            onChange: (value: any) => {
              emit('field-change', rowIndex, prop, value);
            },
            onRegister: (formRef: Ref) => {
              if (rowIndex < 0) return;
              emit('field-register', rowIndex, prop, formRef);
            },
          } as FormTableFieldProps, emptyArrayFunc),
        } as TableColumn;
      }) as TableColumns;
      columns.push({
        key: TABLE_COLUMN_NAME_ACTIONS,
        label: t('components.table.columns.actions'),
        width: '150',
        // fixed: 'right',
        buttons: [
          {
            type: 'primary',
            icon: ['fa', 'plus'],
            tooltip: t('common.actions.add'),
            onClick: (_, rowIndex) => {
              emit('add', rowIndex);
            },
          },
          {
            type: 'info',
            icon: ['fa', 'clone'],
            tooltip: t('common.actions.clone'),
            onClick: (_, rowIndex) => {
              emit('clone', rowIndex);
            },
          },
          {
            type: 'danger',
            icon: ['fa', 'trash-alt'],
            tooltip: t('common.actions.delete'),
            onClick: (_, rowIndex) => {
              emit('delete', rowIndex);
            },
          }
        ]
      });
      return columns;
    });

    return {
      columns,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
