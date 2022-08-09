import {
  faArrowUp,
  faCalculator,
  faClock,
  faFont,
  faInfoCircle,
  faLaptopCode,
  faLayerGroup,
} from "@fortawesome/free-solid-svg-icons";
import Axios from "axios";
import getFormData from "get-form-data";
import { isArray, pick } from "lodash/fp";
import React, { createRef, FC, FormEvent, useState, MouseEvent } from "react";
import CreatableSelect from "react-select/creatable";
import { Alert, Button, Input, Modal, ModalBody, ModalFooter, ModalHeader } from "reactstrap";
import { Rule, RulePayload } from "../interfaces/";
import { FieldGroup } from "./FieldGroup";

const headers = { "Content-Type": "application/json" };

const pickFields = pick([
  "aggregateFieldName",
  "aggregatorFunctionType",
  "groupingKeyNames",
  "limit",
  "limitOperatorType",
  "ruleState",
  "windowMinutes",
]);

type ResponseError = {
  error: string;
  message: string;
} | null;

const sampleRules: {
  [n: number]: RulePayload;
} = {
  1: {
    aggregateFieldName: "paymentAmount",
    aggregatorFunctionType: "SUM",
    groupingKeyNames: ["payeeId", "beneficiaryId"],
    limit: 20000000,
    limitOperatorType: "GREATER",
    windowMinutes: 43200,
    ruleState: "ACTIVE",
  },
   2: {
     aggregateFieldName: "paymentAmount",
     aggregatorFunctionType: "SUM",
     groupingKeyNames: ["beneficiaryId"],
     limit: 10000000,
     limitOperatorType: "GREATER_EQUAL",
     windowMinutes: 1440,
     ruleState: "ACTIVE",
   },
  3: {
    aggregateFieldName: "COUNT_WITH_RESET_FLINK",
    aggregatorFunctionType: "SUM",
    groupingKeyNames: ["paymentType"],
    limit: 100,
    limitOperatorType: "GREATER_EQUAL",
    windowMinutes: 1440,
    ruleState: "ACTIVE",
  },

};

const keywords = ["beneficiaryId", "payeeId", "paymentAmount", "paymentType"];
const aggregateKeywords = ["paymentAmount", "COUNT_FLINK", "COUNT_WITH_RESET_FLINK"];

const MySelect = React.memo(CreatableSelect);

export const AddRuleModal: FC<Props> = props => {
  const [error, setError] = useState<ResponseError>(null);

  const handleClosed = () => {
    setError(null);
    props.onClosed();
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const data = pickFields(getFormData(e.target)) as RulePayload;
    data.groupingKeyNames = isArray(data.groupingKeyNames) ? data.groupingKeyNames : [data.groupingKeyNames];

    const rulePayload = JSON.stringify(data);
    const body = JSON.stringify({ rulePayload });

    setError(null);
    Axios.post<Rule>("/api/rules", body, { headers })
      .then(response => props.setRules(rules => [...rules, { ...response.data, ref: createRef<HTMLDivElement>() }]))
      .then(props.onClosed)
      .catch(setError);
  };

  const postSampleRule = (ruleId: number) => (e: MouseEvent) => {
    const rulePayload = JSON.stringify(sampleRules[ruleId]);
    const body = JSON.stringify({ rulePayload });

    Axios.post<Rule>("/api/rules", body, { headers })
      .then(response => props.setRules(rules => [...rules, { ...response.data, ref: createRef<HTMLDivElement>() }]))
      .then(props.onClosed)
      .catch(setError);
  };

  return (
    <Modal
      isOpen={props.isOpen}
      onClosed={handleClosed}
      toggle={props.toggle}
      backdropTransition={{ timeout: 75 }}
      modalTransition={{ timeout: 150 }}
      size="lg"
    >
      <form onSubmit={handleSubmit}>
        <ModalHeader toggle={props.toggle}>Add a new Rule</ModalHeader>
        <ModalBody>
          {error && <Alert color="danger">{error.error + ": " + error.message}</Alert>}
          <FieldGroup label="ruleState" icon={faInfoCircle}>
            <Input type="select" name="ruleState" bsSize="sm">
              <option value="ACTIVE">ACTIVE</option>
              <option value="PAUSE">PAUSE</option>
              <option value="DELETE">DELETE</option>
            </Input>
          </FieldGroup>

          <FieldGroup label="aggregatorFunctionType" icon={faCalculator}>
            <Input type="select" name="aggregatorFunctionType" bsSize="sm">
              <option value="SUM">SUM</option>
              <option value="AVG">AVG</option>
              <option value="MIN">MIN</option>
              <option value="MAX">MAX</option>
            </Input>
          </FieldGroup>

          <FieldGroup label="aggregateFieldName" icon={faFont}>
            <Input name="aggregateFieldName" type="select" bsSize="sm">
              {aggregateKeywords.map(k => (
                <option key={k} value={k}>
                  {k}
                </option>
              ))}
            </Input>
          </FieldGroup>

          <FieldGroup label="groupingKeyNames" icon={faLayerGroup}>
            <MySelect
              isMulti={true}
              name="groupingKeyNames"
              className="react-select"
              classNamePrefix="react-select"
              options={keywords.map(k => ({ value: k, label: k }))}
            />
          </FieldGroup>

          <FieldGroup label="limitOperatorType" icon={faLaptopCode}>
            <Input type="select" name="limitOperatorType" bsSize="sm">
              <option value="EQUAL">EQUAL (=)</option>
              <option value="NOT_EQUAL">NOT_EQUAL (!=)</option>
              <option value="GREATER_EQUAL">GREATER_EQUAL (>=)</option>
              <option value="LESS_EQUAL">LESS_EQUAL ({"<="})</option>
              <option value="GREATER">GREATER (>)</option>
              <option value="LESS">LESS ({"<"})</option>
            </Input>
          </FieldGroup>
          <FieldGroup label="limit" icon={faArrowUp}>
            <Input name="limit" bsSize="sm" type="number" />
          </FieldGroup>
          <FieldGroup label="windowMinutes" icon={faClock}>
            <Input name="windowMinutes" bsSize="sm" type="number" />
          </FieldGroup>
        </ModalBody>
        <ModalFooter className="justify-content-between">
          <div>
            <Button color="secondary" onClick={postSampleRule(1)} size="sm" className="mr-2">
              Sample Rule 1
            </Button>
            <Button color="secondary" onClick={postSampleRule(2)} size="sm" className="mr-2">
              Sample Rule 2
            </Button>
            <Button color="secondary" onClick={postSampleRule(3)} size="sm" className="mr-2">
              Sample Rule 3
            </Button>
          </div>
          <div>
            <Button color="secondary" onClick={handleClosed} size="sm" className="mr-2">
              Cancel
            </Button>
            <Button type="submit" color="primary" size="sm">
              Submit
            </Button>
          </div>
        </ModalFooter>
      </form>
    </Modal>
  );
};

interface Props {
  toggle: () => void;
  isOpen: boolean;
  onClosed: () => void;
  setRules: (fn: (rules: Rule[]) => Rule[]) => void;
}
