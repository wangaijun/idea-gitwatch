package mobi.hsz.idea.vcswatch.model;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import mobi.hsz.idea.vcswatch.net.NetAccesser;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetCommitInfo implements Runnable {

    private final VirtualFile workingDirectory;
    private final GitWatchService gitWatchService;

    public GetCommitInfo(@NotNull AbstractVcs vcs, @NotNull VirtualFile workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.gitWatchService = GitWatchService.getInstance(vcs.getProject());
    }

    @Override
    public void run() {
        try {
            NetAccesser.exec(workingDirectory.getPath(), "remote", "update");
            ProcessOutput output = NetAccesser.exec(workingDirectory.getPath(),"log", "..@{u}", "--date=raw", "--pretty=format:" + "%h %an ## %ad ## %s");

            Matcher matcher = Pattern.compile("^(\\w+) (.*?) ## (\\d+) .*? ## (.*)$", Pattern.MULTILINE).matcher(output.getStdout());
            while (matcher.find()) {
                String id = matcher.group(1);
                String user = matcher.group(2);
                Date date = new Date(Long.valueOf(matcher.group(3)) * 1000);
                String message = matcher.group(4);

                this.gitWatchService.add(new Commit(id, user, date, message));
            }
        } catch (ExecutionException e) {
            Messages.showErrorDialog("访问Git服务器出现错误:"+e.getLocalizedMessage(),"错误信息");
        }
    }

}
