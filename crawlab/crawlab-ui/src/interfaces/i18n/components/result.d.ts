interface LComponentsResult {
  form: {
    dataType: string;
  };
  types: {
    [key: string]: string;
  };
  dedup: {
    dialog: {
      fields: {
        title: string;
        placeholder: string;
      };
    };
    labels: {
      dedupType: string;
    };
    types: {
      [key: string]: string;
    };
  };
}
