package mobi.hsz.idea.vcswatch.model;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**单例*/
public class GitWatchService {
    public static long DELAY = 600;
    private final ProjectLevelVcsManager vcsManager;
    private final ScheduledExecutorService scheduler;
    private final List<ScheduledFuture<?>> scheduledFutureList = ContainerUtil.newArrayList();
    private final List<OnCommitListener> onCommitListeners = ContainerUtil.newArrayList();
    private final Map<String, Commit> commits = ContainerUtil.newHashMap();


    private GitWatchService(@NotNull Project project) {
        this.vcsManager = ProjectLevelVcsManager.getInstance(project);
        this.scheduler = JobScheduler.getScheduler();
    }

    public static GitWatchService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, GitWatchService.class);
    }

    public void init() {
        stop();

        VcsRoot[] roots = vcsManager.getAllVcsRoots();
        for (VcsRoot root : roots) {

            GetCommitInfo request = new GetCommitInfo(root.getVcs(), root.getPath());
            if (request != null) {
                scheduledFutureList.add(scheduler.scheduleWithFixedDelay(request, 0, DELAY, TimeUnit.SECONDS));
            }
        }
    }

    public void stop() {
        for (ScheduledFuture<?> scheduledFuture : scheduledFutureList) {
            scheduledFuture.cancel(true);
        }
        scheduledFutureList.clear();
    }

    public void add(@NotNull Commit commit) {
        if (!this.commits.containsKey(commit.getId())) {
            this.commits.put(commit.getId(), commit);
            for (OnCommitListener listener : onCommitListeners) {
                listener.onCommit(commit);
            }
        }
    }

    public void setOnCommitListener(@NotNull OnCommitListener listener) {
        this.onCommitListeners.add(listener);
    }

    public void removeOnCommitListener(@NotNull OnCommitListener listener) {
        this.onCommitListeners.remove(listener);
    }

    public interface OnCommitListener {
        public void onCommit(@NotNull Commit commit);
    }
}
