package work.art1st.traincartsdiagram.display;

import com.bergerkiller.bukkit.tc.actions.GroupAction;
import work.art1st.traincartsgizmos.utils.ScreenUI;

public class DelayedAction extends GroupAction {
    protected final Runnable runnable;

    public DelayedAction(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public boolean update() {
        this.runnable.run();
        return true;
    }
}
