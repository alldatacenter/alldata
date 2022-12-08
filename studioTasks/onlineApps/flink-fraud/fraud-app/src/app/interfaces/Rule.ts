import { RefObject } from "react";

export interface Rule {
  id: number;
  rulePayload: string;
  ref: RefObject<HTMLDivElement>;
}

export interface RulePayload {
  aggregateFieldName: string;
  aggregatorFunctionType: string;
  groupingKeyNames: string[];
  limit: number;
  limitOperatorType: string;
  windowMinutes: number;
  ruleState: string;
}
