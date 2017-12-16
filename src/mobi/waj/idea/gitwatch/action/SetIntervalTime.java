package mobi.waj.idea.gitwatch.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import mobi.waj.idea.gitwatch.model.GitWatchService;

public class SetIntervalTime extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String intervalStr = Messages.showInputDialog("Interval time(second):", "Please input(default 10)",Messages.getQuestionIcon());
        try {
            int interval = Integer.parseInt(intervalStr);
            GitWatchService.setIntervalSecond(interval);
        }
        catch (Exception e1){
            //do not do anything
        }
    }

}
