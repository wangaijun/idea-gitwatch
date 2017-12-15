package mobi.hsz.idea.vcswatch.requests;

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsRoot;

import git4idea.GitVcs;
import mobi.hsz.idea.vcswatch.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.1
 */
public class VcsWatchRequestFactory {

    private VcsWatchRequestFactory() {
    }

    @Nullable
    public static GitWatchRequest create(@NotNull VcsRoot root) {
        AbstractVcs vcs = root.getVcs();
        if (vcs == null || root.getPath() == null) {
            return null;
        }

        if (Utils.isPluginEnabled("Git4Idea") && vcs instanceof GitVcs) {
            return new GitWatchRequest(vcs, root.getPath());
        }

        return null;
    }
}
