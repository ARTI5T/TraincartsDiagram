package work.art1st.traincartsdiagram.commands;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.properties.TrainPropertiesStore;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import work.art1st.traincartsdiagram.TrainCartsDiagram;
import work.art1st.traincartsdiagram.display.DisplayConfig;
import work.art1st.traincartsgizmos.utils.ScreenUI;
import work.art1st.traincartsgizmos.utils.TrainUtil;

public class DisplayControl implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (args[0]) {
            case "reload" -> {
                if (args.length != 1) {
                    sender.sendMessage("Wrong parameters");
                    return false;
                }
                DisplayConfig.init();
                return true;
            }
            case "apply" -> {
                if (args.length != 4) {
                    sender.sendMessage("Wrong parameters");
                    return false;
                }
                MinecartGroup group = TrainPropertiesStore.getRelaxed(args[1]).getHolder();
                ScreenUI screenUI = new ScreenUI(DisplayConfig.getTrainConfig(TrainUtil.getTrainType(group)).getNode(args[2]));
                screenUI.applyTo(group, args[3]);
                return true;
            }
            default -> {
                sender.sendMessage("Unknown command");
            }
        }
        return false;
    }
}
