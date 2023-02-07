export declare global {
  interface LRouter {
    menuItems: {
      home: string;
      nodes: string;
      projects: string;
      spiders: string;
      schedules: string;
      tasks: string;
      users: string;
      tags: string;
      tokens: string;
      plugins: string;
      env: {
        deps: {
          title: string;
          settings: string;
          python: string;
          node: string;
        };
      };
      notification: string;
      misc: {
        disclaimer: string;
        mySettings: string;
      };
    };
  }
}
