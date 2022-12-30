cd ./buyer
yarn install&&yarn build
sh docker.sh

cd ../manager
yarn install&&yarn build
sh docker.sh

cd ../seller
yarn install&&yarn build
sh docker.sh

