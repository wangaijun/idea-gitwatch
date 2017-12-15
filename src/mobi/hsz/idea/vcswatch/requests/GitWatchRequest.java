package mobi.hsz.idea.vcswatch.requests;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import git4idea.config.GitVcsApplicationSettings;
import mobi.hsz.idea.vcswatch.core.Commit;
import mobi.hsz.idea.vcswatch.core.VcsWatchManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitWatchRequest implements Runnable {

    /** VCS working directory. */
    private final VirtualFile workingDirectory;

    /** Project VCS watch manager. */
    private final VcsWatchManager vcsWatchManager;

    /**
     * Executes passed command with specified {@link #workingDirectory}.
     *
     * @param command Command to execute
     * @return {@link ProcessOutput} result or <code>null</code> if failed
     */
    @Nullable
    protected ProcessOutput exec(@NotNull String... command) {
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

    /**
     * Adds new {@link Commit} to the {@link VcsWatchManager} registry.
     *
     * @param commit to add
     */
    public void addCommit(@NotNull Commit commit) {
        this.vcsWatchManager.add(commit);
    }



    private static final String TEMPLATE = "%h %an ## %ad ## %s";
    private static final Pattern PATTERN = Pattern.compile("^(\\w+) (.*?) ## (\\d+) .*? ## (.*)$", Pattern.MULTILINE);

    public GitWatchRequest(@NotNull AbstractVcs vcs, @NotNull VirtualFile workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.vcsWatchManager = VcsWatchManager.getInstance(vcs.getProject());
    }

    @NotNull
    protected String getExecutable() {
        return GitVcsApplicationSettings.getInstance().getPathToGit();
    }

    @Override
    public void run() {
        // Update information about ahead commits. If nothing is returned, repository has no remote.
        if (exec("remote", "update") == null) {
            return;
        }

        // Check logs. If nothing is returned, there are not commits to pull.
        ProcessOutput output = exec("log", "..@{u}", "--date=raw", "--pretty=format:" + TEMPLATE);
        if (output == null) {
            return;
        }

        // Parse logs.
        Matcher matcher = PATTERN.matcher(output.getStdout());
        while (matcher.find()) {
            String id = matcher.group(1);
            String user = matcher.group(2);
            Date date = new Date(Long.valueOf(matcher.group(3)) * 1000);
            String message = matcher.group(4);

            addCommit(new Commit(id, user, date, message));
        }
    }

}
