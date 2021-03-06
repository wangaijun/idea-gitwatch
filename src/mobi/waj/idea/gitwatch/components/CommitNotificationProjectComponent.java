package mobi.waj.idea.gitwatch.components;

import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import com.intellij.openapi.vcs.update.AbstractCommonUpdateAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.impl.VcsLogContentProvider;
import com.intellij.vcs.log.impl.VcsLogManager;
import com.intellij.vcs.log.ui.VcsLogUiImpl;
import mobi.waj.idea.gitwatch.model.Commit;
import mobi.waj.idea.gitwatch.model.GitWatchService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

import javax.swing.event.HyperlinkEvent;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CommitNotificationProjectComponent implements ProjectComponent {
    private static final long DELAY = 1500;
    private final Project project;
    private final GitWatchService gitWatchService;
    private final ScheduledExecutorService scheduler;
    private final List<Commit> commits = ContainerUtil.newArrayList();
    private ScheduledFuture<?> scheduledFeature;

    private final Runnable notify = new Runnable() {
        @Override
        public void run() {
            if (commits.isEmpty()) {
                return;
            }

            Notifications.Bus.notify(new CommitNotification(project, commits), project);
            commits.clear();
        }
    };

    private final GitWatchService.OnCommitListener onCommitListener = new GitWatchService.OnCommitListener() {
        @Override
        public void onCommit(@NotNull Commit commit) {
            if (scheduledFeature != null) {
                scheduledFeature.cancel(true);
            }
            commits.add(commit);
            scheduledFeature = scheduler.schedule(notify, DELAY, TimeUnit.MILLISECONDS);
        }
    };

    public CommitNotificationProjectComponent(@NotNull Project project) {
        this.project = project;
        this.gitWatchService = GitWatchService.getInstance(project);
        this.scheduler = JobScheduler.getScheduler();
    }

    @Override
    public void projectOpened() {
        this.gitWatchService.setOnCommitListener(onCommitListener);
    }

    @Override
    public void projectClosed() {
        this.gitWatchService.removeOnCommitListener(onCommitListener);
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "GitWatch.CommitNotification";
    }

    private static class CommitNotification extends Notification {

        private static final String TITLE = "New commit:";
        private static final String TITLE_PLURAL = "New commits:";

        private final List<Commit> commits;

        private CommitNotification(@NotNull final Project project, @NotNull final List<Commit> commits) {
            super(
                    "VCS Watch",
                    commits.size() == 1 ? TITLE : TITLE_PLURAL,
                    "VCS Watch",
                    NotificationType.INFORMATION,
                    createListener(project)
            );

            this.commits = ContainerUtil.newArrayList(commits);
        }

        @NotNull
        @Override
        public String getContent() {
            List<String> messages = ContainerUtil.newArrayList();
            for (Commit commit : commits) {
                String time = new PrettyTime(Locale.ENGLISH).format(commit.getDate());
                String template = "<br/>" +
                        "  <b>%s</b><br/>" +
                        "  <small><a href=\"HASH:%s\">{1}</a> - <i>%s</i> by <b>%s</b></small>";
                messages.add(String.format(template,commit.getMessage(), commit.getId(), commit.getId(), time, commit.getUser()));
            }
            String str = "<a href=\"UPDATE\">Update Project</a><br/>";
            return str + StringUtil.join(messages, "<br/>");
        }

        private static NotificationListener.Adapter createListener(@NotNull final Project project) {
            return new NotificationListener.Adapter() {

                private static final String HASH = "HASH:";

                private static final String UPDATE = "UPDATE";

                private static final String UPDATE_ACTION_ID = "Vcs.UpdateProject";

                @Override
                protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                    if (StringUtil.equals(event.getDescription(), UPDATE)) {
                        update();
                        if (!notification.isExpired()) {
                            notification.expire();
                        }
                    } else if (StringUtil.startsWith(event.getDescription(), HASH)) {
                        jumpToReference(StringUtil.substringAfter(event.getDescription(), HASH));
                    }
                }

                private void update() {
                    final AnAction action = ActionManager.getInstance().getAction(UPDATE_ACTION_ID);
                    assert action instanceof AbstractCommonUpdateAction;
                    final AbstractCommonUpdateAction updateAction = (AbstractCommonUpdateAction) action;
                    ActionManager.getInstance().tryToExecute(
                            updateAction,
                            ActionCommand.getInputEvent(UPDATE_ACTION_ID),
                            null,
                            ActionPlaces.UNKNOWN,
                            true
                    );
                }

                private void jumpToReference(@Nullable final String hash) {
                    final VcsLogManager log = ServiceManager.getService(project, VcsLogManager.class);
                    if (log == null) {
                        return;
                    }

                    final ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
                    final ToolWindow window = windowManager.getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID);
                    ContentManager cm = window.getContentManager();
                    Content[] contents = cm.getContents();
                    for (Content content : contents) {
                        if (VcsLogContentProvider.TAB_NAME.equals(content.getDisplayName())) {
                            cm.setSelectedContent(content);
                        }
                    }

                    Runnable selectCommit = new Runnable() {
                        private boolean invokedLater = false;

                        @Override
                        public void run() {
                            VcsLogUiImpl logUi = log.getMainLogUi();
                            if (logUi != null) {
                                logUi.getVcsLog().jumpToReference(hash);
                            } else {
                                if (invokedLater) {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException ignored) {
                                    }
                                }
                                invokedLater = true;
                                windowManager.invokeLater(this);
                            }
                        }
                    };

                    if (!window.isVisible()) {
                        window.activate(selectCommit, true);
                    } else {
                        selectCommit.run();
                    }
                }
            };
        }

    }

}
