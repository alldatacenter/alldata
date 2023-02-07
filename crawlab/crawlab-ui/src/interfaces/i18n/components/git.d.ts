interface LComponentsGit {
  form: {
    remoteUrl: string;
    currentBranch: string;
    authType: string;
    username: string;
    password: string;
    privateKey: string;
    autoPull: string;
  };
  common: {
    currentBranch: string;
    message: {
      success: {
        checkout: string;
        pull: string;
        commit: string;
      };
    };
    messageBox: {
      confirm: {
        pull: string;
      };
      prompt: {
        commit: {
          label: string;
          placeholder: string;
        };
      };
    };
    actions: {
      pull: string;
      commit: string;
    };
    status: {
      loading: {
        label: string;
        tooltip: string;
      };
    };
  };
  actions: {
    title: string;
    label: {
      pull: string;
      commit: string;
      checkout: string;
    };
    tooltip: {
      pull: string;
      commit: string;
      checkout: string;
    };
  };
  tabs: {
    remote: string;
    references: string;
    logs: string;
    changes: string;
    ignore: string;
  };
  checkout: {
    type: string;
    reference: string;
  };
  references: {
    type: {
      branch: string;
      tag: string;
    };
    table: {
      columns: {
        timestamp: string;
      };
    };
  };
  logs: {
    table: {
      columns: {
        reference: string;
        commitMessage: string;
        author: string;
        timestamp: string;
      };
    };
  };
  changes: {
    status: {
      untracked: string;
      modified: string;
      added: string;
      deleted: string;
      renamed: string;
      copied: string;
      updatedButUnmerged: string;
    };
    table: {
      columns: {
        changedFile: string;
        status: string;
      };
    };
  };
  ignore: {
    table: {
      columns: {
        file: string;
      };
    };
  };
}
