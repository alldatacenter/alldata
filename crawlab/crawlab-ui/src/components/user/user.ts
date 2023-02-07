import {computed, readonly} from 'vue';
import {Store} from 'vuex';
import useForm from '@/components/form/form';
import useUserService from '@/services/user/userService';
import {getDefaultFormComponentData} from '@/utils/form';
import {FORM_FIELD_TYPE_INPUT, FORM_FIELD_TYPE_INPUT_PASSWORD, FORM_FIELD_TYPE_SELECT,} from '@/constants/form';
import {getModeOptions} from '@/utils/task';
import {ROLE_ADMIN, ROLE_NORMAL} from '@/constants/user';
import {ElMessage, ElMessageBox} from 'element-plus';
import {sendEvent} from '@/admin/umeng';
import {translate} from '@/utils/i18n';

// i18n
const t = translate;

// form component data
const formComponentData = getDefaultFormComponentData<User>();

const useUser = (store: Store<RootStoreState>) => {
  // store
  const ns = 'user';
  const state = store.state[ns];

  // options for default mode
  const modeOptions = getModeOptions();

  // batch form fields
  const batchFormFields = computed<FormTableField[]>(() => [
    {
      prop: 'username',
      label: 'Username',
      width: '150',
      fieldType: FORM_FIELD_TYPE_INPUT,
      placeholder: 'Username',
      required: true,
    },
    {
      prop: 'password',
      label: 'Password',
      width: '150',
      placeholder: 'Password',
      fieldType: FORM_FIELD_TYPE_INPUT_PASSWORD,
      required: true,
    },
    {
      prop: 'email',
      label: 'Email',
      width: '150',
      fieldType: FORM_FIELD_TYPE_INPUT,
      placeholder: 'Email',
    },
    {
      prop: 'role',
      label: 'Role',
      width: '150',
      placeholder: 'Role',
      fieldType: FORM_FIELD_TYPE_SELECT,
      options: [
        {label: 'Admin', value: ROLE_ADMIN},
        {label: 'Normal', value: ROLE_NORMAL},
      ],
      required: true,
    },
  ]);

  // form rules
  const formRules = readonly<FormRules>({
    password: {
      trigger: 'blur',
      validator: ((_, value: string, callback) => {
        const invalidMessage = 'Invalid password. Length must be no less than 5.';
        if (0 < value.length && value.length < 5) return callback(invalidMessage);
        return callback();
      }),
    },
  });

  // all user select options
  const allUserSelectOptions = computed<SelectOption[]>(() => state.allList.map(d => {
    return {
      label: d.username,
      value: d._id,
    };
  }));

  // on change password
  const onChangePasswordFunc = async (id?: string) => {
    if (!id) return;

    const {value} = await ElMessageBox.prompt(
      t('components.user.messageBox.prompt.changePassword'),
      t('components.user.form.changePassword'),
      {
        inputType: 'password',
        inputPlaceholder: t('components.user.form.newPassword'),
        inputValidator: (value: string) => {
          return value?.length < 5 ? t('components.user.rules.invalidPassword') : true;
        },
        confirmButtonClass: 'edit-user-password-confirm-btn',
        cancelButtonClass: 'edit-user-password-cancel-btn',
      });

    sendEvent('click_user_form_change_password');

    await store.dispatch(`${ns}/changePassword`, {id, password: value});
    await ElMessage.success(t('common.message.success.save'));
  };

  const rolesOptions: SelectOption[] = [
    {label: t('components.user.role.admin'), value: ROLE_ADMIN},
    {label: t('components.user.role.normal'), value: ROLE_NORMAL},
  ];

  return {
    ...useForm('user', store, useUserService(store), formComponentData),
    modeOptions,
    batchFormFields,
    formRules,
    allUserSelectOptions,
    onChangePasswordFunc,
    rolesOptions,
  };
};

export default useUser;
