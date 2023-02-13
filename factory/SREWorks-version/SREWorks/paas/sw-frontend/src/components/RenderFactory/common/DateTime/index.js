import React from 'react';

import moment from 'moment';

function DateTime(props) {
    const format = props.format || 'YYYY-MM-DD HH:mm:ss';
    return (<span>{moment(props.value).format(format)}</span>);
}

export default DateTime;