package com.alibaba.tesla.appmanager.common.service.impl;

import com.alibaba.tesla.appmanager.autoconfig.ImageBuilderProperties;
import com.alibaba.tesla.appmanager.common.enums.RepoTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.common.util.CommandUtil;
import com.alibaba.tesla.appmanager.common.util.GitlabUtil;
import com.alibaba.tesla.appmanager.common.util.StringUtil;
import com.alibaba.tesla.appmanager.domain.dto.ContainerObjectDTO;
import com.alibaba.tesla.appmanager.domain.req.git.GitCloneReq;
import com.alibaba.tesla.appmanager.domain.req.git.GitFetchFileReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class GitServiceImpl implements GitService {

    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";

    private final ImageBuilderProperties imageBuilderProperties;

    public GitServiceImpl(ImageBuilderProperties imageBuilderProperties) {
        this.imageBuilderProperties = imageBuilderProperties;
    }

    /**
     * Clone 仓库到指定目录中
     *
     * @param logContent 日志 StringBuilder
     * @param request    镜像构建请求
     * @param dir        目标存储目录
     */
    @Override
    public void cloneRepo(StringBuilder logContent, GitCloneReq request, Path dir) {
        Path tmpDir = null;
        try {
            // 存在 repoPath 的时候，需要使用临时目录进行文件转移
            if (StringUtils.isNotEmpty(request.getRepoPath())) {
                tmpDir = Files.createTempDirectory("appmanager_image_builder_tmp_");
            } else {
                tmpDir = dir;
            }

            // Clone
            String repo = getAuthorizedRepo(request);
            String cloneCommand;
            if (StringUtils.isEmpty(request.getBranch())) {
                if (request.isKeepGitFiles()) {
                    cloneCommand = String.format("git clone --recursive %s %s", repo, tmpDir.toString());
                } else {
                    cloneCommand = String.format("git clone --recursive --depth 1 %s %s", repo, tmpDir.toString());
                }
            } else {
                if (request.isKeepGitFiles()) {
                    cloneCommand = String.format("git clone --recursive -b %s %s %s",
                            request.getBranch(), repo, tmpDir.toString());
                } else {
                    cloneCommand = String.format("git clone --recursive -b %s --depth 1 %s %s",
                            request.getBranch(), repo, tmpDir.toString());
                }
            }
            logContent.append(String.format("run command: %s\n", cloneCommand));
            logContent.append(CommandUtil.runLocalCommand(cloneCommand));

            // 如果指定了特定的 commit，那么切换到对应的 commit
            if (!StringUtils.isEmpty(request.getCommit())) {
                String resetCommitCommand = String.format("cd %s; git reset --hard %s", tmpDir, request.getCommit());
                logContent.append(String.format("run command: %s\n", resetCommitCommand));
                logContent.append(CommandUtil.runLocalCommand(resetCommitCommand));
            }

            // 删除 .git 临时文件
            if (!request.isKeepGitFiles()) {
                String rmInternalDirCommand = String.format("rm -rf %s/.git*", tmpDir);
                logContent.append(String.format("run command: %s\n", rmInternalDirCommand));
                logContent.append(CommandUtil.runLocalCommand(rmInternalDirCommand));
            }

            // 存在 repoPath 的时候，需要将 repoPath 对应的目录拷贝到 dir 实际对应的目录中
            if (StringUtils.isNotEmpty(request.getRepoPath())) {
                Path fromdir = tmpDir.resolve(request.getRepoPath());
                Path todir = dir.resolve(request.getRepoPath()).getParent();
                FileUtils.moveDirectoryToDirectory(fromdir.toFile(), todir.toFile(), true);
            }
        } catch (IOException e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot create temp directory", e);
        } finally {
            // 仅当使用了临时目录的时候，进行函数默认删除清理
            if (StringUtils.isNotEmpty(request.getRepoPath()) && tmpDir != null) {
                String tmpDirStr = tmpDir.toString();
                if (tmpDir.toFile().delete()) {
                    logContent.append(String.format("builder tmp dir has deleted %s", tmpDirStr));
                }
            }
        }
    }

    /**
     * 获取远端 Git 仓库中的指定文件内容
     *
     * @param logContent 日志 StringBuilder
     * @param request    抓取文件请求
     * @return 文件内容
     */
    @Override
    public String fetchFile(StringBuilder logContent, GitFetchFileReq request) {
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("appmanager_repo_fetch_file_");
            String branch = request.getBranch();
            String filepath = request.getFilePath();
            String repo = getAuthorizedRepo(GitCloneReq.builder()
                    .repo(request.getRepo())
                    .ciAccount(request.getCiAccount())
                    .ciToken(request.getCiToken())
                    .build());
            String command = String.format("cd %s; git clone -b %s --no-checkout --depth=1 --no-tags %s .; git restore " +
                "--staged %s; git checkout %s", tmpDir.toString(), branch, repo, filepath, filepath);
            logContent.append(String.format("run command: %s\n", command));
            logContent.append(CommandUtil.runLocalCommand(command));
            return new String(Files.readAllBytes(Paths.get(tmpDir.toString(), filepath)));
        } catch (IOException e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot create temp directory", e);
        } finally {
            if (tmpDir != null) {
                String tmpDirStr = tmpDir.toString();
                if (tmpDir.toFile().delete()) {
                    logContent.append(String.format("builder tmp dir has deleted %s", tmpDirStr));
                }
            }
        }
    }

    /**
     * 切换分支
     *
     * @param logContent 日志 StringBuilder
     * @param branch     分支
     * @param dir        目标存储目录
     */
    @Override
    public void checkoutBranch(StringBuilder logContent, String branch, Path dir) {
        String command = String.format("cd %s; git checkout %s", dir, branch);
        logContent.append(CommandUtil.runLocalCommand(command));
    }

    /**
     * 获取带授权 Token 的 Repo 地址
     *
     * @param request 镜像构建请求
     * @return Authorized Repo
     */
    private String getAuthorizedRepo(GitCloneReq request) {
        String repo = request.getRepo();
        String ciAccount = imageBuilderProperties.getDefaultCiAccount();
        String ciToken = imageBuilderProperties.getDefaultCiToken();
        if (!StringUtils.isEmpty(request.getCiAccount())) {
            ciAccount = request.getCiAccount();
        }
        if (!StringUtils.isEmpty(request.getCiToken())) {
            ciToken = request.getCiToken();
        }
        if (repo.startsWith(HTTP_PREFIX)) {
            String rest = StringUtil.trimStringByString(repo, HTTP_PREFIX);
            return String.format("%s%s:%s@%s", HTTP_PREFIX, ciAccount, ciToken, rest);
        } else if (repo.startsWith(HTTPS_PREFIX)) {
            String rest = StringUtil.trimStringByString(repo, HTTPS_PREFIX);
            return String.format("%s%s:%s@%s", HTTPS_PREFIX, ciAccount, ciToken, rest);
        } else {
            return String.format("http://%s:%s@gitlab-sc.alibaba-inc.com/%s", ciAccount, ciToken, repo);
        }
    }


    @Override
    public void createRepoList(List<ContainerObjectDTO> containerObjectList) {
        for (ContainerObjectDTO containerObjectDTO : containerObjectList) {
            if (BooleanUtils.isNotTrue(containerObjectDTO.getOpenCreateRepo())) {
                continue;
            }

            RepoTypeEnum repoTypeEnum = containerObjectDTO.getRepoType();
            containerObjectDTO.initRepo();

            String token = imageBuilderProperties.getDefaultCiToken();

            if (repoTypeEnum == RepoTypeEnum.THIRD_REPO) {
                token = containerObjectDTO.getCiToken();
            }

            GitlabUtil.createProject(containerObjectDTO.getRepoDomain(), containerObjectDTO.getRepoGroup(),
                    containerObjectDTO.getAppName(), token);
        }
    }
}
