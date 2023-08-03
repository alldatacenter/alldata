/* eslint-disable react/no-danger */
import React from 'react';
import { Select, SelectProps } from 'antd';

const { Option } = Select;
type ISourceItem = {
    label?: string,
    value?: any,
    children?: ISourceItem[]
    [key: string]: any
}

interface CustomSelect extends SelectProps {
    source: ISourceItem[],
    sourceLabelMap?: string,
    sourceValueMap?: string,
    widgetChildProps?: any,
}

const CustomSelect = ({
    value, onChange, style, source, sourceLabelMap, sourceValueMap, widgetChildProps, ...rest
}: CustomSelect) => {
    const selectParams = {
        style: { width: '100%', ...style },
        ...rest,
    };
    return (
        <Select value={value} onChange={onChange} {...selectParams}>
            {
                (source || []).map((item, index) => {
                    const key = item.key || `${index}_${item.value}_${item.label}`;
                    let label = item[sourceLabelMap!] || item.label;
                    const isHtml = typeof label === 'string' && label[0] === '<';
                    if (isHtml) {
                        label = <span dangerouslySetInnerHTML={{ __html: label }} />;
                    }
                    return (
                        <Option
                            key={key}
                            value={item[sourceValueMap!] || item.value}
                            {...widgetChildProps}
                        >
                            {label}
                        </Option>
                    );
                })
            }
        </Select>
    );
};

export default CustomSelect;
