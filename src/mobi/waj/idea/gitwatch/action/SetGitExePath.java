package mobi.waj.idea.gitwatch.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import mobi.waj.idea.gitwatch.model.GitWatchService;

public class SetGitExePath extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String path = Messages.showInputDialog("Path to your git exe:", "Please input",Messages.getQuestionIcon());
        GitWatchService.setGitPath(path);
    }
}
