const handler = require('serve-handler');
const path = require('path');
const http = require('http');
const { port } = require('../package.json');

const server = http.createServer((request, response) => {
  // You pass two more arguments for config and middleware
  // More details here: https://github.com/vercel/serve-handler#options
  return handler(request, response, {
    public: path.join(__dirname, '../dist'),
    headers: [
      {
        source: '**/*.js',
        headers : [{
          key : 'Cache-Control',
          value : "no-cache"
        }]
      }
    ]
  });
})

server.listen(port, () => {
  console.log(`Running at http://localhost:${port}`);
});
