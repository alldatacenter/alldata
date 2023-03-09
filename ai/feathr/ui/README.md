# Feathr Feature Store UI

This directory hosts Feathr Feature Store UI code.

## Development Getting Started

### Prerequisites

1. Install latest [Node](https://nodejs.org/en/) v16.x. Run `node --version` to verify installed Node versions.
2. Recommended for Visual Studio Code users: install following two extensions to enable auto code formatting support.
   - [ESLint](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint)
   - [Prettier - Code formatter](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)

### Build and run locally

Each command in this section should be run from the root directory of the repository.

Open terminal, go to root of this repository and run following commands.

```
cd ui
npm install
npm start
```

This should launch [http://localhost:3000](http://localhost:3000) on your web browser. The page will reload when you make code changes and save.

#### [Optional] Override configurations for local development

- **Point to a different backend endpoint**: by default, UI talks to live backend API at https://feathr-sql-registry.azurewebsites.net. To point to a custom backend API (e.g. running locally), create a .env.local in this directory and set REACT_APP_API_ENDPOINT, for example:

```
REACT_APP_API_ENDPOINT=http://localhost:8080
```

- **Use different authentication settings**: by default, UI authenticates with an Azure AD application with multiple tenants authentication enabled. To change to use a different Azure AD application, create a .env.local in this directory and set REACT_APP_AZURE_CLIENT_ID and REACT_APP_AZURE_TENANT_ID, for example:

```
REACT_APP_AZURE_CLIENT_ID=<REPLACE_WITH_YOUR_AZURE_CLIENT_ID>
REACT_APP_AZURE_TENANT_ID=<REPLACE_WITH_YOUR_TENANT_ID>
```

### Deploying

- For static file based deployment, run `npm run build` and upload `build/` to your server.
- For docker image based deployment, run `docker -t <image_name> .` to build image and push to your container registry.

### Code Linting & Formatting

Following tools are used to lint and format code:
  * [**eslint**](https://eslint.org/)
  * [**prettier**](https://prettier.io/)

#### Linting

If ESLint plugin is installed, vscode will pick up configuration from [.eslintrc](.eslintrc) and automatically lint the code on save. To lint code for entire code base, simply run:

```
npm run lint-eslint
```

This command will automatically fix all problems that can be fixed, and list the rest problems requires manual fix.

#### Formatting with Prettier

Prettier is an opinionated code formatter for Typescript. It removes all original styling and ensures that all outputted code conforms to a consistent style. If Prettier is installed, vscode will pick up configuration from [.prettierrc](.prettierrc) file and automatically format code on save. To format code for entire code base, simply run:

```
npm run format
```

In Feathr UI, `npx prettier` is already registered as a npm task.
```
"format": "npx prettier --write src/**"
```

#### Formatting automatically on commit

[Husky](https://github.com/typicode/husky) is used to lint commit changes as a git hook. Prettier is configured to run on staged files in husky git hook. This prevents anything with formatting errors to be committed.

### Project Structure

```
src/
  api         // rest client
  components  // shared react components
  models      // api data model, view model, etc
  pages       // a view on the page, can be routed by url path
  router      // url path and page mapping
```
