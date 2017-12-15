package mobi.hsz.idea.vcswatch.components;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.util.messages.MessageBusConnection;
import mobi.hsz.idea.vcswatch.model.GitWatchService;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ProjectComponent} implementation that watches for the VCS roots changes in the project.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.1
 */
public class GitWatchProjectComponent implements ProjectComponent {

    /** Current project. */
    private final Project project;

    /** Project VCS watch manager. */
    private final GitWatchService gitWatchService;

    /** Project message bus. */
    private MessageBusConnection messageBus;

    private final VcsListener vcsListener = new VcsListener() {
        @Override
        public void directoryMappingChanged() {
            gitWatchService.start();
        }
    };

    public GitWatchProjectComponent(@NotNull Project project) {
        this.project = project;
        this.gitWatchService = GitWatchService.getInstance(project);
    }

    @Override
    public void projectOpened() {
        if (messageBus == null) {
            messageBus = project.getMessageBus().connect();
        }
        messageBus.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, vcsListener);
    }

    @Override
    public void projectClosed() {
        if (messageBus != null) {
            messageBus.disconnect();
        }
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
        return "VcsWatch.VcsWatch";
    }

}
