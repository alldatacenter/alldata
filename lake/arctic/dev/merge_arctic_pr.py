#!/usr/bin/env python3

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Utility for creating well-formed pull request merges and pushing them to Arctic
# For committers:
# Please check your local git envs via `git remote -v` which should
# netease	git@github.com:netease/arctic.git (fetch)
# netease	git@github.com:netease/arctic.git (push)
# origin	git@github.com:[ YOUR GITHUB USER NAME ]/arctic.git (fetch)
# origin	git@github.com:[ YOUR GITHUB USER NAME ]/arctic.git (push)

import json
import os
import re
import subprocess
import sys
from urllib.request import urlopen
from urllib.request import Request
from urllib.error import HTTPError

ARCTIC_HOME = os.environ.get("ARCTIC_HOME", os.getcwd())
PR_REMOTE_NAME = os.environ.get("PR_REMOTE_NAME", "netease")
PUSH_REMOTE_NAME = os.environ.get("PUSH_REMOTE_NAME", "netease")
GITHUB_OAUTH_KEY = os.environ.get("GITHUB_OAUTH_KEY")
GITHUB_API_BASE = "https://api.github.com/repos/netease/arctic"
BRANCH_PREFIX = "PR_TOOL"


def get_json(url):
    try:
        request = Request(url)
        if GITHUB_OAUTH_KEY:
            request.add_header('Authorization', 'token %s' % GITHUB_OAUTH_KEY)
        return json.load(urlopen(request))
    except HTTPError as e:
        if "X-RateLimit-Remaining" in e.headers and e.headers["X-RateLimit-Remaining"] == '0':
            print("Exceeded the GitHub API rate limit; see the instructions in " +
                  "dev/merge_arctic_pr.py to configure an OAuth token for making authenticated " +
                  "GitHub requests.")
        else:
            print("Unable to fetch URL, exiting: %s" % url, e)
        sys.exit(-1)


def fail(msg):
    print(msg)
    clean_up()
    sys.exit(-1)


def run_cmd(cmd):
    print(cmd)
    if isinstance(cmd, list):
        return subprocess.check_output(cmd).decode('utf-8')
    else:
        return subprocess.check_output(cmd.split(" ")).decode('utf-8')


def continue_maybe(prompt):
    result = input("\n%s (y/n): " % prompt)
    if result.lower() != "y":
        fail("Okay, exiting")


def clean_up():
    if 'original_head' in globals():
        print("Restoring head pointer to %s" % original_head)
        run_cmd("git checkout %s" % original_head)

        branches = run_cmd("git branch").replace(" ", "").split("\n")

        for branch in list(filter(lambda x: x.startswith(BRANCH_PREFIX), branches)):
            print("Deleting local branch %s" % branch)
            run_cmd("git branch -D %s" % branch)

def fix_title(text, num):
    if (re.search(r'^\[ARCTIC\s#[0-9]{3,6}\].*', text)):
        return text

    return '[ARCTIC #%s] %s' % (num, text)

# merge the requested PR and return the merge hash
def merge_pr(pr_num, target_ref, title, body, pr_repo_desc):
    pr_branch_name = "%s_MERGE_PR_%s" % (BRANCH_PREFIX, pr_num)
    target_branch_name = "%s_MERGE_PR_%s_%s" % (BRANCH_PREFIX, pr_num, target_ref.upper())
    run_cmd("git fetch %s pull/%s/head:%s" % (PR_REMOTE_NAME, pr_num, pr_branch_name))
    run_cmd("git fetch %s %s:%s" % (PUSH_REMOTE_NAME, target_ref, target_branch_name))
    run_cmd("git checkout %s" % target_branch_name)

    had_conflicts = False
    try:
        run_cmd(['git', 'merge', pr_branch_name, '--squash'])
    except Exception as e:
        msg = "Error merging: %s\nWould you like to manually fix-up this merge?" % e
        continue_maybe(msg)
        msg = "Okay, please fix any conflicts and 'git add' conflicting files... Finished?"
        continue_maybe(msg)
        had_conflicts = True

    commit_authors = run_cmd(['git', 'log', 'HEAD..%s' % pr_branch_name,
                             '--pretty=format:%an <%ae>']).split("\n")
    distinct_authors = sorted(set(commit_authors),
                              key=lambda x: commit_authors.count(x), reverse=True)
    primary_author = input(
        "Enter primary author in the format of \"name <email>\" [%s]: " %
        distinct_authors[0])
    if primary_author == "":
        primary_author = distinct_authors[0]
    else:
        # When primary author is specified manually, de-dup it from author list and
        # put it at the head of author list.
        distinct_authors = list(filter(lambda x: x != primary_author, distinct_authors))
        distinct_authors.insert(0, primary_author)

    commits = run_cmd(['git', 'log', 'HEAD..%s' % pr_branch_name,
                       '--pretty=format:%h [%an] %s']).split("\n\n")

    merge_message_flags = []

    merge_message_flags += ["-m", title]
    if body is not None:
        # We remove @ symbols from the body to avoid triggering e-mails
        # to people every time someone creates a public fork of Arctic.
        merge_message_flags += ["-m", body.replace("@", "")]

    committer_name = run_cmd("git config --get user.name").strip()
    committer_email = run_cmd("git config --get user.email").strip()

    if had_conflicts:
        message = "This patch had conflicts when merged, resolved by\nCommitter: %s <%s>" % (
            committer_name, committer_email)
        merge_message_flags += ["-m", message]

    # The string "Closes #%s" string is required for GitHub to correctly close the PR
    merge_message_flags += ["-m", "Closes #%s from %s." % (pr_num, pr_repo_desc)]

    for issueId in re.findall("ARCTIC #[0-9]{3,5}", title):
        merge_message_flags += ["-m", issueId.replace("ARCTIC", "Closes")]

    for c in commits:
        merge_message_flags += ["-m", c]

    authors = "Authored-by:" if len(distinct_authors) == 1 else "Lead-authored-by:"
    authors += " %s" % (distinct_authors.pop(0))
    if len(distinct_authors) > 0:
        authors += "\n" + "\n".join(["Co-authored-by: %s" % a for a in distinct_authors])
    authors += "\n" + "Signed-off-by: %s <%s>" % (committer_name, committer_email)

    merge_message_flags += ["-m", authors]

    run_cmd(['git', 'commit', '--author="%s"' % primary_author] + merge_message_flags)

    continue_maybe("Merge complete (local ref %s). Push to %s?" % (
        target_branch_name, PUSH_REMOTE_NAME))

    try:
        run_cmd('git push %s %s:%s' % (PUSH_REMOTE_NAME, target_branch_name, target_ref))
    except Exception as e:
        clean_up()
        fail("Exception while pushing: %s" % e)

    merge_hash = run_cmd("git rev-parse %s" % target_branch_name)[:8]
    clean_up()
    print("Pull request #%s merged!" % pr_num)
    print("Merge hash: %s" % merge_hash)
    return merge_hash


def cherry_pick(pr_num, merge_hash, default_branch):
    pick_ref = input("Enter a branch name [%s]: " % default_branch)
    if pick_ref == "":
        pick_ref = default_branch

    pick_branch_name = "%s_PICK_PR_%s_%s" % (BRANCH_PREFIX, pr_num, pick_ref.upper())

    run_cmd("git fetch %s %s:%s" % (PUSH_REMOTE_NAME, pick_ref, pick_branch_name))
    run_cmd("git checkout %s" % pick_branch_name)

    try:
        run_cmd("git cherry-pick -sx %s" % merge_hash)
    except Exception as e:
        msg = "Error cherry-picking: %s\nWould you like to manually fix-up this merge?" % e
        continue_maybe(msg)
        msg = "Okay, please fix any conflicts and finish the cherry-pick. Finished?"
        continue_maybe(msg)

    continue_maybe("Pick complete (local ref %s). Push to %s?" % (
        pick_branch_name, PUSH_REMOTE_NAME))

    try:
        run_cmd('git push %s %s:%s' % (PUSH_REMOTE_NAME, pick_branch_name, pick_ref))
    except Exception as e:
        clean_up()
        fail("Exception while pushing: %s" % e)

    pick_hash = run_cmd("git rev-parse %s" % pick_branch_name)[:8]
    clean_up()

    print("Pull request #%s picked into %s!" % (pr_num, pick_ref))
    print("Pick hash: %s" % pick_hash)
    return pick_ref

def get_current_ref():
    ref = run_cmd("git rev-parse --abbrev-ref HEAD").strip()
    if ref == 'HEAD':
        # The current ref is a detached HEAD, so grab its SHA.
        return run_cmd("git rev-parse HEAD").strip()
    else:
        return ref


def main():
    global original_head

    os.chdir(ARCTIC_HOME)
    original_head = get_current_ref()

    branches = get_json("%s/branches" % GITHUB_API_BASE)
    branch_names = list(filter(lambda x: x.startswith("0"), [x['name'] for x in branches]))
    # Assumes branch names can be sorted lexicographically
    latest_branch = sorted(branch_names, reverse=True)[0]

    pr_num = input("Which pull request would you like to merge? (e.g. 34): ")
    pr = get_json("%s/pulls/%s" % (GITHUB_API_BASE, pr_num))
    pr_events = get_json("%s/issues/%s/events" % (GITHUB_API_BASE, pr_num))

    url = pr["url"]
    title = pr["title"]
    title = fix_title(title, pr_num)
    body = re.sub(re.compile(r"<!--[^>]*-->\n?", re.DOTALL), "", pr["body"]).lstrip()
    target_ref = pr["base"]["ref"]
    user_login = pr["user"]["login"]
    base_ref = pr["head"]["ref"]
    pr_repo_desc = "%s/%s" % (user_login, base_ref)

    merge_commits = \
        [e for e in pr_events if e["event"] == "merged" and e["commit_id"]]

    if merge_commits:
        merge_hash = merge_commits[0]["commit_id"]
        message = get_json("%s/commits/%s" % (GITHUB_API_BASE, merge_hash))["commit"]["message"]

        print("Pull request %s has already been merged, assuming you want to backport" % pr_num)
        commit_is_downloaded = run_cmd(['git', 'rev-parse', '--quiet', '--verify',
                                        "%s^{commit}" % merge_hash]).strip() != ""
        if not commit_is_downloaded:
            fail("Couldn't find any merge commit for #%s, you may need to update HEAD." % pr_num)

        print("Found commit %s:\n%s" % (merge_hash, message))
        cherry_pick(pr_num, merge_hash, latest_branch)
        sys.exit(0)

    if not bool(pr["mergeable"]):
        msg = "Pull request %s is not mergeable in its current form.\n" % pr_num + \
            "Continue? (experts only!)"
        continue_maybe(msg)

    print("\n=== Pull Request #%s ===" % pr_num)
    print("title:\t%s\nsource:\t%s\ntarget:\t%s\nurl:\t%s\nbody:\n\n%s" %
          (title, pr_repo_desc, target_ref, url, body))
    continue_maybe("Proceed with merging pull request #%s?" % pr_num)

    merged_refs = [target_ref]

    merge_hash = merge_pr(pr_num, target_ref, title, body, pr_repo_desc)

    pick_prompt = "Would you like to pick %s into another branch?" % merge_hash
    while input("\n%s (y/n): " % pick_prompt).lower() == "y":
        merged_refs = merged_refs + [cherry_pick(pr_num, merge_hash, latest_branch)]

if __name__ == "__main__":
    import doctest
    (failure_count, test_count) = doctest.testmod()
    if failure_count:
        sys.exit(-1)
    try:
        main()
    except:
        clean_up()
        raise
