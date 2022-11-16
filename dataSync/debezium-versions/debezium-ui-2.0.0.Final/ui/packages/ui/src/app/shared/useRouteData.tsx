import { useHistory, useLocation, useParams } from 'react-router-dom';

export function useRouteData<P, S>() {
  const location = useLocation();
  const history = useHistory();
  const params = useParams();

  return {
    history,
    location,
    params: (params || {}) as P,
    state: (location.state || {}) as S,
  };
}
