package work.art1st.traincartsdiagram.commands;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import work.art1st.traincartsdiagram.trains.ManagedTrain;
import work.art1st.traincartsdiagram.trains.TestRunTrain;
import work.art1st.traincartsdiagram.TimeTableDiagram;

import java.util.ArrayList;
import java.util.List;

public class TestRunCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only player can execute this command.");
            return true;
        }
        Entity vehicle = ((Player) sender).getVehicle();
        if (vehicle == null) {
            sender.sendMessage("You are not in a vehicle.");
            return true;
        }
        MinecartMember<?> member = MinecartMemberStore.getFromEntity(vehicle);
        if (member == null) {
            sender.sendMessage("You are not in a train.");
            return true;
        }
        MinecartGroup group = member.getGroup();
        List<ManagedTrain.StationInfo> stationInfoList = new ArrayList<>();
        for (String station:
             args) {
            String[] s = station.split(":");
            if (s[0].startsWith("-")) {
                stationInfoList.add(new ManagedTrain.StationInfo(s[0].substring(1), 0, 0, s.length > 1 ? s[1] : "", new ArrayList<>()));
            } else {
                stationInfoList.add(new ManagedTrain.StationInfo(s[0], 10, 0, s.length > 1 ? s[1] : "", new ArrayList<>()));
            }
        }
        TestRunTrain train = new TestRunTrain(stationInfoList, group);
        TimeTableDiagram.getInstance().addNonScheduledTrain(train.getCircuitNumber(), train);
        TimeTableDiagram.getInstance().updateCircuit(train.getCoupleNumber(), train.getCircuitNumber());
        //train.setStartTime(0);
        train.bind();
        return true;
    }
}
