package mobi.hsz.idea.vcswatch.net;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import git4idea.config.GitVcsApplicationSettings;
import mobi.hsz.idea.vcswatch.model.Commit;
import mobi.hsz.idea.vcswatch.model.GitWatchService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitWatchRequest implements Runnable {

    private final VirtualFile workingDirectory;
    private final GitWatchService gitWatchService;
    private static final String TEMPLATE = "%h %an ## %ad ## %s";
    private static final Pattern PATTERN = Pattern.compile("^(\\w+) (.*?) ## (\\d+) .*? ## (.*)$", Pattern.MULTILINE);

    public GitWatchRequest(@NotNull AbstractVcs vcs, @NotNull VirtualFile workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.gitWatchService = GitWatchService.getInstance(vcs.getProject());
    }

    @Override
    public void run() {
        if (exec("remote", "update") == null) {
            return;
        }

        ProcessOutput output = exec("log", "..@{u}", "--date=raw", "--pretty=format:" + TEMPLATE);
        if (output == null) {
            return;
        }

        Matcher matcher = PATTERN.matcher(output.getStdout());
        while (matcher.find()) {
            String id = matcher.group(1);
            String user = matcher.group(2);
            Date date = new Date(Long.valueOf(matcher.group(3)) * 1000);
            String message = matcher.group(4);

            addCommit(new Commit(id, user, date, message));
        }
    }

    @Nullable
    private ProcessOutput exec(@NotNull String... command) {
        try {
            final List<String> commands = ContainerUtil.newArrayList(getExecutable());
            ContainerUtil.addAll(commands, command);
            final ProcessOutput output = ExecUtil.execAndGetOutput(commands, workingDirectory.getPath());
            if (output.getExitCode() > 0 || output.getStdoutLines().size() == 0) {
                return null;
            }
            return output;
        } catch (ExecutionException ignored) {
            return null;
        }
    }

    private void addCommit(@NotNull Commit commit) {
        this.gitWatchService.add(commit);
    }

    @NotNull
    private String getExecutable() {
        return GitVcsApplicationSettings.getInstance().getPathToGit();
    }
}
