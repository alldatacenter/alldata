type UserStoreModule = BaseModule<UserStoreState, UserStoreGetters, UserStoreMutations, UserStoreActions>;

interface UserStoreState extends BaseStoreState<User> {
  me?: User;
}

interface UserStoreGetters extends BaseStoreGetters<User> {
  me: StoreGetter<UserStoreState, User | undefined>;
}

interface UserStoreMutations extends BaseStoreMutations<User> {
  setMe: StoreMutation<UserStoreState, User>;
  resetMe: StoreMutation<UserStoreState>;
}

interface UserStoreActions extends BaseStoreActions<User> {
  changePassword: StoreAction<UserStoreState, { id: string; password: string }>;
  getMe: StoreAction<UserStoreState>;
  postMe: StoreAction<UserStoreState, User>;
}
