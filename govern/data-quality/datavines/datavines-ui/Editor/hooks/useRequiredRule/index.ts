import { useIntl } from 'react-intl';

export default () => {
    const intl = useIntl();
    return [{
        required: true,
        message: intl.formatMessage({ id: 'dv_metric_required_tip' }),
    }];
};
