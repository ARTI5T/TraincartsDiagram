package work.art1st.traincartsdiagram;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.pathfinding.PathPredictEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.LauncherConfig;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.block.BlockFace;
import work.art1st.traincartsdiagram.instructions.AbstractInstruction;
import work.art1st.traincartsdiagram.trains.ManagedTrain;
import work.art1st.traincartsgizmos.utils.TrainUtil;

import java.util.Objects;

public class SignActionManagedStation extends SignAction {
    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("m-station");
    }

    @Override
    public void execute(SignActionEvent event) {
        if (!event.hasGroup() || !event.isAction(SignActionType.GROUP_ENTER, SignActionType.GROUP_LEAVE)) {
            return;
        }
        long stopTime = this.getStopTimeMs(event);
        MinecartGroup group = event.getGroup();
        ManagedTrain train = TimeTableDiagram.getInstance().getTrainFromGroup(group);
        String station = event.getLine(2);
        SignActionType actionType = Objects.requireNonNull(event.getAction());
        if (actionType.equals(SignActionType.GROUP_ENTER)) {
            TrainCartsDiagram.getInstance().getLogger().info("Train " + event.getGroup().getProperties().getTrainName() + " enters station " + station + " track " + event.getLine(3));
            if (stopTime > 0) {
                group.stop();
                group.getActions().clear();
                /* Switch Display */
                ManagedTrain.StationInfo info = train.getStationInfo(station);
                if (info != null) {
                    train.lcdArrivingAtStation(group, info.name(), info.track());
                }
                /* Wait time */
                group.getActions().addActionWait(stopTime).addTag(this.getTag(event));
                /* Execute instructions */
                String[] split = event.getLine(1).split(" ");
                String doorSide = split.length > 1 ? split[1].toUpperCase() : "";
                AbstractInstruction.StationInstructionParams params = new AbstractInstruction.StationInstructionParams(group, info, doorSide);
                train.executeInstructions(params);
                /* Open door */
                switch (doorSide) {
                    case "L" -> {
                        TrainUtil.openDoor(group, true, false, (double) stopTime / 1000);
                    }
                    case "R" -> {
                        TrainUtil.openDoor(group, false, true, (double) stopTime / 1000);
                    }
                    case "LR" -> {
                        TrainUtil.openDoor(group, true, true, (double) stopTime / 1000);
                    }
                }
                /* Set launch */
                if (!train.isLastStation(station)) {
                    LauncherConfig config = new LauncherConfig();
                    config.setAcceleration(event.getGroup().getProperties().getWaitAcceleration());

                    BlockFace launchDirection = event.getCartEnterFace();
                    if (params.isShouldReverse()) {
                        TrainUtil.switchDirection(event.getGroup());
                        launchDirection = launchDirection.getOppositeFace();
                    }

                    event.getMember().getActions().addActionLaunch(launchDirection, config, event.getGroup().getProperties().getSpeedLimit()).addTag(this.getTag(event));
                } else {
                    if (params.isShouldReverse()) {
                        TrainUtil.switchDirection(event.getGroup());
                    }
                }
            } else if (stopTime == 0) {
                train.gotoNextStation(train.getStationInfo(station), group);
            }
            //info.getMember().getActions().addActionLaunch(info.getCartEnterFace(), 2.0, info.getGroup().getAverageForce()).addTag(this.getTag(info));
            //TrainCartsDiagram.getInstance().getLogger().info("ManagedStation: " + station + " stopTime: " + stopTime + " speedLimit: " + info.getGroup().getProperties().getSpeedLimit() + " waitAcceleration: " + info.getGroup().getProperties().getWaitAcceleration());
        }
    }

    @Override
    public boolean build(SignChangeActionEvent event) {
        return SignBuildOptions.create()
                .setPermission(Permission.BUILD_STATION)
                .setName("train m-station")
                .setDescription("Managed station")
                .handle(event);
    }

    @Override
    public void predictPathFinding(SignActionEvent info, PathPredictEvent prediction) {
        if (this.getStopTimeMs(info) > 0) {
            prediction.addSpeedLimit(0.014);
        }
    }

    public long getStopTimeMs(SignActionEvent info) {
        MinecartGroup group = info.getGroup();
        String station = info.getLine(2);
        String track = info.getLine(3);
//        if (track.isEmpty() ? group.getProperties().matchTag(station) : group.getProperties().matchTag(station + ":" + track)) {
//            group.getProperties().removeTags(station, station + ":" + track);
//            return 10 * 1000;
//        }
        ManagedTrain train = TimeTableDiagram.getInstance().getTrainFromGroup(group);
        long stopTime = train.getStopTimeMs(station, group);
        if (stopTime <= 0) {
            return 0;
        }
        /* 第4行指定停车匹配条件 <股道>[方向] */
        if (!info.getLine(3).isEmpty()) {
            if (Objects.requireNonNull(train.getStationInfo(station)).track().equals(track)) {
                return stopTime;
            } else {
                /* 股道/方向错误，不需要执行nextStation逻辑 */
                return -1;
            }
        }
        return stopTime;
    }

    public String getTag(SignActionEvent info) {
        return StringUtil.blockToString(info.getBlock());
    }
}
