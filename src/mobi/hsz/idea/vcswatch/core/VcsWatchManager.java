package mobi.hsz.idea.vcswatch.core;

import com.intellij.concurrency.JobScheduler;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.util.containers.ContainerUtil;
import git4idea.GitVcs;
import mobi.hsz.idea.vcswatch.model.Commit;
import mobi.hsz.idea.vcswatch.model.GitWatchRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Manager that checks for the changes in the registered VCS repositories.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.1
 */
public class VcsWatchManager {

    /**
     * TODO: don't use static delay
     */
    public static long DELAY = 600;

    /**
     * Project VCS manager.
     */
    private final ProjectLevelVcsManager vcsManager;

    /**
     * An {@link java.util.concurrent.ExecutorService} that can schedule commands.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * List of the scheduled futures.
     */
    private final List<ScheduledFuture<?>> scheduledFutureList = ContainerUtil.newArrayList();

    /**
     * {@link OnCommitListener} event listeners list.
     */
    private final List<OnCommitListener> onCommitListeners = ContainerUtil.newArrayList();

    /**
     * Commits map.
     */
    private final Map<String, Commit> commits = ContainerUtil.newHashMap();


    private VcsWatchManager(@NotNull Project project) {
        this.vcsManager = ProjectLevelVcsManager.getInstance(project);
        this.scheduler = JobScheduler.getScheduler();
    }

    public static VcsWatchManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, VcsWatchManager.class);
    }

    @Nullable
    public static GitWatchRequest create(@NotNull VcsRoot root) {
        AbstractVcs vcs = root.getVcs();
        if (vcs == null || root.getPath() == null) {
            return null;
        }

        IdeaPluginDescriptor p = PluginManager.getPlugin(PluginId.getId("Git4Idea"));
        if (p instanceof IdeaPluginDescriptorImpl && p.isEnabled() && vcs instanceof GitVcs) {
            return new GitWatchRequest(vcs, root.getPath());
        }

        return null;
    }

    /**
     * Fetches available project VCS roots and starts listening.
     */
    public void init() {
        stop();

        VcsRoot[] roots = vcsManager.getAllVcsRoots();
        for (VcsRoot root : roots) {
            GitWatchRequest request = create(root);
            if (request != null) {
                scheduledFutureList.add(scheduler.scheduleWithFixedDelay(request, 0, DELAY, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * Cancels all elements from {#link #scheduledFutureList} and clears the list.
     */
    public void stop() {
        for (ScheduledFuture<?> scheduledFuture : scheduledFutureList) {
            scheduledFuture.cancel(true);
        }
        scheduledFutureList.clear();
    }

    /**
     * Adds new commit to the {@link #commits} stack if doesn't exist.
     *
     * @param commit to add
     */
    public void add(@NotNull Commit commit) {
        if (!this.commits.containsKey(commit.getId())) {
            this.commits.put(commit.getId(), commit);
            for (OnCommitListener listener : onCommitListeners) {
                listener.onCommit(commit);
            }
        }
    }

    /**
     * Adds new {@link OnCommitListener}.
     *
     * @param listener to add
     */
    public void setOnCommitListener(@NotNull OnCommitListener listener) {
        this.onCommitListeners.add(listener);
    }

    /**
     * Removes {@link OnCommitListener}.
     *
     * @param listener to remove
     */
    public void removeOnCommitListener(@NotNull OnCommitListener listener) {
        this.onCommitListeners.remove(listener);
    }

    /** Listener that is fired when new {@link Commit} appears. */
    public interface OnCommitListener {
        public void onCommit(@NotNull Commit commit);
    }

}
