#代码目录
code_path=$PWD

git checkout master
git pull

cd ${code_path}/manager
rm -rf ./dist
yarn install
yarn build

cd ${code_path}/seller
rm -rf ./dist
yarn install
yarn build

cd ${code_path}/buyer
rm -rf ./dist
yarn install
yarn build