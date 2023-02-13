import { Alert } from 'app/components/Alert';
import { AuthorizationStatus } from 'app/constants';
import { getUserInfoByToken } from 'app/slice/thunks';
import { StorageKeys } from 'globalConstants';
import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import persistence from 'utils/persistence';

export const AuthorizationPage = () => {
  const [status, setStatus] = useState<AuthorizationStatus>(
    AuthorizationStatus.Initialized,
  );
  const [errorMessage, setErrorMessage] = useState('');
  const dispatch = useDispatch();
  const history = useHistory();

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const token = searchParams.get('authorization_token');
    const errorMessage = searchParams.get('error_message');

    if (token) {
      setStatus(AuthorizationStatus.Pending);

      dispatch(
        getUserInfoByToken({
          token,
          resolve: () => {
            // share page oauth login redirect
            const redirectUrl = persistence.session.get(
              StorageKeys.AuthRedirectUrl,
            );
            if (redirectUrl) {
              persistence.session.remove(StorageKeys.AuthRedirectUrl);
              window.location.href = redirectUrl;
            } else {
              history.replace('/');
            }
          },
          reject: () => {
            setStatus(AuthorizationStatus.Error);
          },
        }),
      );
    }

    if (errorMessage) {
      setStatus(AuthorizationStatus.Error);
      setErrorMessage(errorMessage);
    }
  }, [dispatch, history]);

  return <Alert status={status} errorMessage={errorMessage} />;
};
