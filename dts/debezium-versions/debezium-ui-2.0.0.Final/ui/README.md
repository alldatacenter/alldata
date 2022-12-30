

# Debezium UI

This monorepo uses Lerna and npm Workspaces. 
React-based Single Page Application code based on Patternfly 4 in present inside [ui](./packages/ui) folder.

## Requirements
* node (version 16.x.x or higher) and npm (version 8.x.x or higher).

## Development workflow

This will install all dependencies in each project, build them, and symlink them via Lerna

```sh
npm install
```

Run a full build
```sh
npm run build
```

Start the development server
```sh
npm run start
```

## Internationalization
This project uses [react-i18next](https://react.i18next.com/) for internationalization. Check out the existing examples in the code or the documentation for more information on how to use it.

You should run
```sh
npm run i18n
```
after you internationalize strings in order to generate the required files.

Namespaces other than 'public' must be added to [i18n.ts](./packages/ui/src/i18n/i18n.ts) on line 31.

If you want to add an additional language, you need to add locale to line 51 in [i18next-parser.config.js](./packages/ui/i18next-parser.config.js) (managed by the parser).

## Testing the project
Testing is also just a command away:

```sh
npm run unit:test
```
This command runs Jest, an incredibly useful testing utility, against all files whose extensions end in `.test.ts`.
Like with the `npm run start` command, Jest will automatically run as soon as it detects changes.
If you'd like, you can run `npm start` and `npm run unit:test` side by side so that you can preview changes and test them simultaneously.

## Configurations
* [TypeScript Config](./packages/ui/tsconfig.json)
* [Webpack Config](./packages/ui/webpack.common.js)