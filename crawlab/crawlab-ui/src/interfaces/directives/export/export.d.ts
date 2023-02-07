export declare global {
  type ExportDirectiveTarget = string | (() => string);

  type ExportDirectiveConditions = FilterConditionData[] | (() => FilterConditionData[]);

  interface ExportDirectivePayload {
    target: ExportDirectiveTarget;
    conditions?: ExportDirectiveConditions;
  }

  type ExportDirective = ExportDirectiveTarget | ExportDirectivePayload;
}
