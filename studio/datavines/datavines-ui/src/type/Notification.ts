export interface NoticeDynamicItemOption {
    label: string;
    value: string;
    disabled: boolean;
}

export interface NoticeDynamicItemValidate {
    required: boolean;
    type: string;
    trigger: string;
}

export interface NoticeDynamicItem {
    field: string;
    type: string;
    title: string;
    value?: string;
    validate: NoticeDynamicItemValidate[];
    options?: NoticeDynamicItemOption[];
}
