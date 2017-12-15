package mobi.hsz.idea.vcswatch.net;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.util.containers.ContainerUtil;
import git4idea.config.GitVcsApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NetAccesser {
    @Nullable
    public static ProcessOutput exec(String path, @NotNull String... command) throws ExecutionException {
        final List<String> commands = ContainerUtil.newArrayList(GitVcsApplicationSettings.getInstance().getPathToGit());
        ContainerUtil.addAll(commands, command);
        final ProcessOutput output = ExecUtil.execAndGetOutput(commands, path);
        return output;
    }

}
