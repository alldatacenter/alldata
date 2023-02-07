type NotificationStoreModule = BaseModule<NotificationStoreState, NotificationStoreGetters, NotificationStoreMutations, NotificationStoreActions>;

interface NotificationStoreState extends BaseStoreState<NotificationSetting> {
  triggersList: string[];
}

type NotificationStoreGetters = BaseStoreGetters<NotificationSetting>;

interface NotificationStoreMutations extends BaseStoreMutations<NotificationSetting> {
  setTriggersList: StoreMutation<NotificationStoreState, string[]>;
  resetTriggersList: StoreMutation<NotificationStoreState>;
  setTriggersEnabled: StoreMutation<NotificationStoreState, string[]>;
  resetTriggersEnabled: StoreMutation<NotificationStoreState>;
  setTemplateTitle: StoreMutation<NotificationStoreState, string>;
  resetTemplateTitle: StoreMutation<NotificationStoreState>;
  setTemplateContent: StoreMutation<NotificationStoreState, string>;
  resetTemplateContent: StoreMutation<NotificationStoreState>;
}

interface NotificationStoreActions extends BaseStoreActions<NotificationSetting> {
  getTriggersList: StoreAction<NotificationStoreState>;
}
