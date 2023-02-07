export declare global {
  interface FilterSelectOptionsRemote {
    colName: string;
    value?: string;
    label?: string;
  }

  interface FilterSelectProps {
    label?: string;
    placeholder?: string;
    filterable?: boolean;
    options?: SelectOption[];
    optionsRemote?: FilterSelectOptionsRemote;
  }
}
