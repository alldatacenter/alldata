const shell = require('shelljs');
//根据环境去采用不同的配置文件
if(shell.exec(`npm run build -e production daily`).code !== 0){
    console.log(`Error: npm run build -e production daily`);
    shell.exit(1);
} else{
    console.log(`Success:npm run build -e production daily`);
}