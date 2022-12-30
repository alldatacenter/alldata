import './NoPreviewFilterField.css';
import { ExpandableSection } from '@patternfly/react-core';
import React from 'react';

export interface INoPreviewFilterFieldProps {
  i18nShowFilter: string;
  i18nHideFilter: string;
}

export const NoPreviewFilterField: React.FunctionComponent<
  INoPreviewFilterFieldProps
> = (props) => {
  const [isExpanded, setIsExpanded] = React.useState<boolean>(false);
  const onToggle = (isExpandedVal: boolean) => {
    setIsExpanded(isExpandedVal);
  };
  return (
    <ExpandableSection
      toggleText={isExpanded ? props.i18nHideFilter : props.i18nShowFilter}
      onToggle={onToggle}
      isExpanded={isExpanded}
      className={'no-preview-filter-field_expandable'}
    >
      {props.children}
    </ExpandableSection>
  );
};
