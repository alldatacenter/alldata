export declare global {
  interface Export {
    id?: string;
    type?: ExportType;
    target?: string;
    // filter?: any;
    status?: string;
    start_ts?: string;
    end_ts?: string;
    file_name?: string;
    download_path?: string;
  }
}
