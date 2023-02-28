export type errorDynamicItem = {
    field: string;
    props: errorDynamicItemProps;
    type: string;
    title: string;
    value?: string,
    validate?: errorDynamicValidate[];
}

type errorDynamicItemProps = {
    type: string;
    placeholder: string;
    rows: number;
    disabled: boolean;
    size: string;
}

type errorDynamicValidate = {
    required: boolean;
    message: string;
    type: string;
    trigger: string;
}
