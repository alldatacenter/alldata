# DASHBOARD FOR BDP PLATFORM

开源大数据平台

## Environment Prepare

Install `node_modules`:

```bash
npm install
```

## Scripts

### Start project

```bash
npm start
```

### Build project

```bash
npm run build
```

### Check code style

```bash
npm run lint
```

You can also use script to auto fix some lint error:

```bash
npm run lint:fix
```

### Test code

```bash
npm test
```

## Deployment

### Test env

```bash
./run.sh build:test
```

### Prod env

```bash
./run.sh build:prod
```
