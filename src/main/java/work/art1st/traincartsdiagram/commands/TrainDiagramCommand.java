package work.art1st.traincartsdiagram.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import work.art1st.traincartsdiagram.TrainCartsDiagram;

public class TrainDiagramCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage("Wrong parameters");
            return false;
        }
        switch (args[0]) {
            case "reload" -> {
                if (TrainCartsDiagram.getInstance().loadDiagram()) {
                    sender.sendMessage("Reloaded diagram config");
                } else {
                    sender.sendMessage("Failed to reload diagram config");
                }
                return true;
            }
            case "stop" -> {
                TrainCartsDiagram.getInstance().stop();
                sender.sendMessage("Started");
                return true;
            }
            default -> sender.sendMessage("Unknown command");
        }
        return false;
    }
}
