import { IconDefinition } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import React, { FC } from "react";
import { Col, FormGroup, Label } from "reactstrap";
import styled from "styled-components";

const LabelColumn = styled(Label)`
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-basis: 33%;
  flex: 1 1 auto;
`;

const InputColumn = styled(Col)`
  flex-basis: 67%;
  flex: 1 1 auto;
`;

export const FieldGroup: FC<Props> = props => (
  <FormGroup className="row">
    <LabelColumn className="col-sm-4">
      <FontAwesomeIcon icon={props.icon} fixedWidth={true} className="mr-2" />
      {props.label}
    </LabelColumn>
    <InputColumn sm="8">{props.children}</InputColumn>
  </FormGroup>
);

interface Props {
  label: string;
  icon: IconDefinition;
}
