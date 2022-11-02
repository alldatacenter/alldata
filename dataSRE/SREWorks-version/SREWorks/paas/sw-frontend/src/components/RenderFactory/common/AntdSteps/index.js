import * as React from 'react';
import { Steps } from 'antd';

const Step = Steps.Step;

function AntdSteps(props) {
    return (<Steps size="small">
        {
            props.value.map(item =>
                <Step key={item.title} title={item.title} status={props.statusMap[item.status]} />
            )
        }
    </Steps>)
}

export default AntdSteps;



