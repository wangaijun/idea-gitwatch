package mobi.waj.idea.gitwatch.model;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.util.containers.ContainerUtil;
import git4idea.GitVcs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**单例*/
public class GitWatchService {
    public static long DELAY = 60;
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

    public void start() {
        stop();

        VcsRoot[] roots = vcsManager.getAllVcsRoots();
        for (VcsRoot root : roots) {
            AbstractVcs vcs = root.getVcs();
//            if (!(vcs instanceof GitVcs)) continue;
            GetCommitInfoOper request = new GetCommitInfoOper(vcs, root.getPath());
            if (request != null) {
                new Thread(request).start();
                scheduledFutureList.add(scheduler.scheduleWithFixedDelay(request, 0, DELAY, TimeUnit.SECONDS));
            }
        }
        if (scheduledFutureList.size()!=1){
            throw new RuntimeException("scheduledFutureList.size():"+scheduledFutureList.size());
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
        void onCommit(@NotNull Commit commit);
    }
}