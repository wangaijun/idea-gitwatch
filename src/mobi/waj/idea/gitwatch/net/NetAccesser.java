package mobi.waj.idea.gitwatch.net;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.util.containers.ContainerUtil;
import git4idea.config.GitVcsApplicationSettings;
import mobi.waj.idea.gitwatch.model.GitWatchService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NetAccesser {
    @Nullable
    public static ProcessOutput exec(String path, @NotNull String... command) throws ExecutionException {
        String pathToGit = GitWatchService.getGitPath();
        if (pathToGit==null || pathToGit.trim().equals("")) {
//        GitVcsApplicationSettings settings = GitVcsApplicationSettings.getInstance();
//        String pathToGit = settings.getPathToGit();
            pathToGit = "C:\\Program Files\\Git\\cmd\\git.exe";
        }
        final List<String> commands = ContainerUtil.newArrayList(pathToGit);
        ContainerUtil.addAll(commands, command);
        final ProcessOutput output = ExecUtil.execAndGetOutput(commands, path);
        return output;
    }

}
