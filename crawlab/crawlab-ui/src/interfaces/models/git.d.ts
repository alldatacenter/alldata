export declare global {
  interface GitChange {
    path?: string;
    name?: string;
    is_dir?: boolean;
    staging?: string;
    worktree?: string;
    extra?: string;
    children?: GitChange[];
  }

  interface GitLog {
    hash?: string;
    msg?: string;
    branch?: string;
    author_name?: string;
    author_email?: string;
    timestamp?: string;
    refs?: GitRef[];
  }

  interface GitRef {
    type?: string;
    name?: string;
    hash?: string;
  }

  interface GitData {
    current_branch?: string;
    branches?: GitRef[];
    changes?: GitChange[];
    logs?: GitLog[];
    tags?: GitRef[];
    ignore?: string[];
    git?: Git;
  }

  interface Git extends BaseModel {
    url?: string;
    auth_type?: string;
    username?: string;
    password?: string;
    current_branch?: string;
    auto_pull?: boolean;
  }
}
