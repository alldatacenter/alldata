// Hook for server
// if (typeof require.ensure !== 'function') {
//     require.ensure = function(dependencies, callback) {
//         callback(require)
//     }
// }


const routes = {
        path: '/',
        component: require('views/Index').default,
        // indexRoute: {
        //     getComponent(nextState, callback) {
        //         require.ensure([], require => {
        //             callback(null, require('views/Index').default)
        //         }, 'index')
        //     }
        // },
        // childRoutes: [{
        //     path: 'explore',
        //     getComponent(nextState, callback) {
        //         require.ensure([], require => {
        //             callback(null, require('./explore/containers/App').default)
        //         }, 'explore')
        //     }
        // }, {
        //     path: 'about',
        //     getComponent(nextState, callback) {
        //         require.ensure([], require => {
        //             callback(null, require('./about/containers/App').default)
        //         }, 'about')
        //     }
        // }]
    }

export default routes
