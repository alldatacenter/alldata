import { useHistory } from 'react-router';
export const useToScheduleDetails = () => {
  const history = useHistory();
  return {
    toDetails: (orgId: string, scheduleId?: string) => {
      if (scheduleId) {
        history.push(`/organizations/${orgId}/schedules/${scheduleId}`);
      } else {
        history.replace(`/organizations/${orgId}/schedules`);
      }
    },
  };
};
