import {createApp} from '@/package';
// import {getDemoRoutes} from '@/demo/router';

(function () {
  // create app options
  const options = {} as CreateAppOptions;

  // demo
  // if (process.env.VUE_APP_DEMO) {
  //   options.initDemo = true;
  //   options.rootRoutes = (options.rootRoutes || [])?.concat(getDemoRoutes());
  // }

  // create app
  createApp(options)
    .then(app => {
      window._app = app;
    });
})();
