export declare interface ResultService {
  saveItem(...items: ResultItem[]): Promise<void>;

  saveItems(items: ResultItem[]): Promise<void>;

  save(items: ResultItem[]): Promise<void>;
}

export declare interface ResultItem {
  [key: string]: any;
}

export declare function saveItem(...items: ResultItem[]): void;

export declare function saveItems(items: ResultItem[]): void;
