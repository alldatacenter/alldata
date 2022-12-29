import { Alert } from 'app/components/Alert';
import { AuthorizationStatus } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { getUserInfoByToken } from 'app/slice/thunks';
import { useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { request2 } from 'utils/request';

export const ActivationPage = () => {
  const [status, setStatus] = useState<AuthorizationStatus>(
    AuthorizationStatus.Initialized,
  );
  const history = useHistory();
  const dispatch = useDispatch();
  const t = useI18NPrefix('authorization');

  const activateAndLogin = useCallback(
    async (token: string) => {
      try {
        const { data } = await request2<string>(`/users/active?token=${token}`);

        if (!data) {
          throw new Error();
        }

        dispatch(
          getUserInfoByToken({
            token: data,
            resolve: () => {
              history.replace('/');
            },
            reject: () => {
              setStatus(AuthorizationStatus.Error);
            },
          }),
        );
      } catch (error) {
        setStatus(AuthorizationStatus.Error);
      }
    },
    [dispatch, history],
  );

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const token = searchParams.get('activation_token');

    if (token) {
      setStatus(AuthorizationStatus.Pending);
      activateAndLogin(token);
    }
  }, [activateAndLogin]);

  return (
    <Alert
      status={status}
      pendingTitle={t('activating')}
      pendingMessage={t('activatingDesc')}
      errorTitle={t('activatingError')}
    />
  );
};
