const path = require('path');

module.exports = {
  webpack:{
    alias:{
      '@pages':path.resolve(__dirname,'src/pages'),
      "@components":path.resolve(__dirname,"src/components"),
      "@utils":path.resolve(__dirname,"src/utils"),
      "@router":path.resolve(__dirname,"src/router"),
      "@assets":path.resolve(__dirname,"src/assets")
    }
  }
}
