# Contributing to Koordinator

Welcome to Koordinator! Koordinator consists several repositories under the organization. We encourage you to help out
by reporting issues, improving documentation, fixing bugs, or adding new features.

Please also take a look at our code of conduct, which details how contributors are expected to conduct themselves as
part of the Koordinator community.

We made Koordinator open-source to empower developers to fix and extend the product to better meet their needs. Nothing
thrills us more than people so passionate about the product that they're willing to spend their own time to learn the
codebase and give back to the community. We created this doc, so we can support contributors in a way that doesn't
sacrifice precious bandwidth that we use to serve our users and otherwise meet our community goals.

## Reporting issues

To be honest, we regard every user of Koordinator as a very kind contributor. After experiencing Koordinator, you may
have some feedback for the project. Then feel free to open an issue.

There are lots of cases when you could open an issue:

- bug report
- feature request
- performance issues
- feature proposal
- feature design
- help wanted
- doc incomplete
- test improvement
- any questions on project
- and so on

Also we must remind that when filing a new issue, please remember to remove the sensitive data from your post. Sensitive
data could be password, secret key, network locations, private business data and so on.

## Code and doc contribution

Every action to make Koordinator better is encouraged. On GitHub, every improvement for Koordinator could be via a PR (
short for pull request).

- If you find a typo, try to fix it!
- If you find a bug, try to fix it!
- If you find some redundant codes, try to remove them!
- If you find some test cases missing, try to add them!
- If you could enhance a feature, please DO NOT hesitate!
- If you find code implicit, try to add comments to make it clear!
- If you find code ugly, try to refactor that!
- If you can help to improve documents, it could not be better!
- If you find document incorrect, just do it and fix that!
- ...

### Workspace Preparation

To put forward a PR, we assume you have registered a GitHub ID. Then you could finish the preparation in the following
steps:

1. **Fork** Fork the repository you wish to work on. You just need to click the button Fork in right-left of project
   repository main page. Then you will end up with your repository in your GitHub username.
2. **Clone** your own repository to develop locally. Use `git clone https://github.com/<your-username>/<project>.git` to
   clone repository to your local machine. Then you can create new branches to finish the change you wish to make.
3. **Set remote** upstream to be `https://github.com/koordinator-sh/<project>.git` using the following two commands:

```bash
git remote add upstream https://github.com/koordinator-sh/<project>.git
git remote set-url --push upstream no-pushing
```

Adding this, we can easily synchronize local branches with upstream branches.

4. **Create a branch** to add a new feature or fix issues

Update local working directory:

```bash
cd <project>
git fetch upstream
git checkout main
git rebase upstream/main
```

Create a new branch:

```bash
git checkout -b <new-branch>
```

Make any change on the new-branch then build and test your codes.

### PR Description

PR is the only way to make change to Koordinator project files. To help reviewers better get your purpose, PR
description could not be too detailed. We encourage contributors to follow
the [PR template](./.github/pull_request_template.md) to finish the pull request.

### Developing Environment

As a contributor, if you want to make any contribution to Koordinator project, we should reach an agreement on the
version of tools used in the development environment. Here are some dependents with specific version:

- Golang : v1.17+
- Kubernetes: v1.20+

### Developing guide

There's a `Makefile` in the root folder which describes the options to build and install. Here are some common ones:

```bash
# Generate code (e.g., apis, clientset, informers) and manifests (e.g., CRD, RBAC YAML files)
make generate manifests

# Build the koord-manager and koordlet binary
make build

# Run the unit tests
make test
```

### Proposals

If you are going to contribute a feature with new API or needs significant effort, please submit a proposal
in [./docs/proposals/](./docs/proposals) first.

## Engage to help anything

We choose GitHub as the primary place for Koordinator to collaborate. So the latest updates of Koordinator are always
here. Although contributions via PR is an explicit way to help, we still call for any other ways.

- reply to other's issues if you could;
- help solve other user's problems;
- help review other's PR design;
- help review other's codes in PR;
- discuss about Koordinator to make things clearer;
- advocate Koordinator technology beyond GitHub;
- write blogs on Koordinator and so on.

In a word, **ANY HELP IS CONTRIBUTION**.

## Joining the community

Follow these instructions if you want to:

- Become a member of the Koordinator GitHub org (see below)
- Become part of the Koordinator build cop or release teams
- Be recognized as an individual or organization contributing to Koordinator

### Joining the Koordinator GitHub Org

Before asking to join the community, we ask that you first make a small number of contributions to demonstrate your
intent to continue contributing to Koordinator.

- **Note**: Anyone can contribute to Koordinator, adding yourself as a member in the organization is not a mandatory
  step.

There are a number of ways to contribute to Koordinator:

- Submit PRs
- File issues reporting bugs or providing feedback
- Answer questions on Slack or GitHub issues

- **Note**: This only counts GitHub related ways of contributing

When you are ready to join

- [Open an issue](https://github.com/koordinator-sh/koordinator/issues/new?assignees=&labels=area%2Fgithub-membership&template=membership.yml&title=REQUEST%3A+New+membership+for+<your-GH-handle>)
  against the **koordinator-sh/koordinator** repo
- Make sure that the list of contributions included is representative of your work on the project.
- Mention 2 existing reviewers who are sponsoring your membership.
- After the request is approved, an admin will send you an invitation.
    - This is a manual process that's generally run a couple of times a week.
    - If a week passes without receiving an invitation reach out on DingTalk or Slack.
