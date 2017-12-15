package mobi.hsz.idea.vcswatch.model;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import mobi.hsz.idea.vcswatch.model.Commit;
import mobi.hsz.idea.vcswatch.model.GitWatchService;
import mobi.hsz.idea.vcswatch.net.NetAccesser;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetCommitInfo implements Runnable {

    private final VirtualFile workingDirectory;
    private final GitWatchService gitWatchService;
    private static final String TEMPLATE = "%h %an ## %ad ## %s";
    private static final Pattern PATTERN = Pattern.compile("^(\\w+) (.*?) ## (\\d+) .*? ## (.*)$", Pattern.MULTILINE);

    public GetCommitInfo(@NotNull AbstractVcs vcs, @NotNull VirtualFile workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.gitWatchService = GitWatchService.getInstance(vcs.getProject());
    }

    @Override
    public void run() {
        if (NetAccesser.exec(workingDirectory.getPath(), "remote", "update") == null) {
            return;
        }

        ProcessOutput output = NetAccesser.exec(workingDirectory.getPath(),"log", "..@{u}", "--date=raw", "--pretty=format:" + TEMPLATE);
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

    private void addCommit(@NotNull Commit commit) {
        this.gitWatchService.add(commit);
    }

}