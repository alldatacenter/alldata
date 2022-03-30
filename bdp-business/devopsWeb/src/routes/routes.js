// Hook for server
// if (typeof require.ensure !== 'function') {
//     require.ensure = function(dependencies, callback) {
//         callback(require)
//     }
// }


const routes = {
        path: '/',
        component: require('views/Index').default,
    }

export default routes
