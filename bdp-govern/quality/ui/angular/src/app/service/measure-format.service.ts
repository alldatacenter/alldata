import {Injectable} from "@angular/core";
import * as yaml from "js-yaml";

@Injectable()
export class MeasureFormatService {
  constructor() {
  }

  format(measure: any, format: Format) {
    switch (format) {
      case Format.json:
        return JSON.stringify(measure, null, 4);
      case Format.yaml:
        return yaml.dump(measure);

    }
  }

  parse(data: string, format: Format) {
    switch (format) {
      case Format.json:
        return JSON.parse(data);
      case Format.yaml:
        return yaml.load(data);

    }
  }

  determineFormat(data: string) {
    try {
      JSON.parse(data);
      return Format.json;
    } catch (e) {}
    try {
      if (yaml.load(data)) {
        return Format.yaml;
      }
    } catch (e) {}
    return null;
  }
}

export enum Format {
  json = 1,
  yaml = 2
}


