package work.art1st.traincartsdiagram.trains;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import com.bergerkiller.bukkit.tc.properties.TrainPropertiesStore;
import com.bergerkiller.bukkit.tc.utils.LauncherConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.art1st.traincartsdiagram.TimeTableDiagram;
import work.art1st.traincartsdiagram.TimeUtil;
import work.art1st.traincartsdiagram.TrainCartsDiagram;
import work.art1st.traincartsdiagram.display.DelayedAction;
import work.art1st.traincartsdiagram.display.DisplayConfig;
import work.art1st.traincartsdiagram.instructions.AbstractInstruction;
import work.art1st.traincartsgizmos.utils.ScreenUI;
import work.art1st.traincartsgizmos.utils.TrainUtil;

import java.util.*;

public class ManagedTrain {
    public record StationInfo(String name, long stopTime, long departureTime, @NotNull String track, List<AbstractInstruction> instructions) {
    }
    private final Map<String, StationInfo> stations;
    private final List<StationInfo> stationList;
    @Getter
    private final String coupleNumber;
    @Getter
    private final String circuitNumber;
    @Getter
    private final String direction;
    @Getter
    private final String bound;
    @Getter
    private final String operationType;
    private final long startTime;
    private int currentStationIndex = 0;
    private ScreenUI frontDisplay;
    private ScreenUI sideDisplay;
    private ScreenUI lcdDisplay;

    public ManagedTrain(List<StationInfo> stationList, String coupleNumber, String circuitNumber, String direction, String bound, String operationType, long startTime) {
        this.stationList = stationList;
        this.coupleNumber = coupleNumber;
        this.circuitNumber = circuitNumber;
        this.direction = direction;
        this.bound = bound;
        this.operationType = operationType;
        this.stations = new HashMap<>();
        this.startTime = startTime;
        for (StationInfo info : stationList) {
            this.stations.put(info.name, info);
        }
    }
    public ManagedTrain() {
        this(new ArrayList<>(), "", "", "", "", "", -1);
    }
    public @Nullable StationInfo getStationInfo(String station) {
        return this.stations.get(station);
    }
    public long getStopTimeMs(String station, MinecartGroup train) {
        StationInfo info = this.getStationInfo(station);
        if (info != null && info.stopTime() > 0) {
            long scheduledStopTicks = info.departureTime() - TimeUtil.getTimetableEquivTime(train.getWorld());
            if (scheduledStopTicks > 0) {
                return Math.max(8 * 1000 /* TODO: minimum stop time */, scheduledStopTicks * 50);
            }
            return 8 * 1000 /* TODO: minimum stop time */;
        }
        return 0;
    }
    /**
     * 指令在停车后执行
     * @param params
     */
    public void executeInstructions(AbstractInstruction.StationInstructionParams params) {
        StationInfo info = params.getStationInfo();
        if (info != null) {
            if (info.instructions() != null) {
                for (AbstractInstruction instruction : info.instructions()) {
                    instruction.execute(params);
                }
            }
            this.gotoNextStation(info, params.getTrain());
        }
    }
    /**
     * 离开车站时执行
     * @param info
     * @param train
     * TODO: [BUG] lcdNextStation立即执行了，没有等待车门关闭
     */
    public void gotoNextStation(StationInfo info, MinecartGroup train) {
        if (info != null) {
            if (this.currentStationIndex >= this.stationList.size()) {
                return;
            }
            int index = this.stationList.indexOf(info);
            if (index != this.currentStationIndex) {
                TrainCartsDiagram.getInstance().getLogger().warning("Train " + this.coupleNumber + " is not at the expected station " + this.stationList.get(currentStationIndex).name() + " but at " + info.name() + "!");
                return;
            }
            this.currentStationIndex += 1;
            /* 似乎DestinationRoute和Destination是两个东西。设置了前者并不会自动设置Destination。 */
            TrainProperties properties = train.getProperties();
            if (index != -1 && index + 1 < this.stationList.size()) {
                StationInfo nextInfo = this.stationList.get(index + 1);
                properties.addDestinationToRoute(nextInfo.name() + "_" + nextInfo.track());
                TrainCartsDiagram.getInstance().getLogger().info("Train " + this.coupleNumber + " is scheduled to go to station " + nextInfo.name() + "_" + nextInfo.track());
            }
            if (!properties.hasDestination()) {
                String nextDestination = properties.getNextDestinationOnRoute();
                //properties.removeDestinationFromRoute(nextDestination);
                properties.setDestination(nextDestination);
                TrainCartsDiagram.getInstance().getLogger().info("Train " + this.coupleNumber + " is now going to " + nextDestination);
            }
            StationInfo nextStoppingStation = this.getNextStoppingStation(info);
            if (nextStoppingStation != null) {
                train.getActions().addAction(new DelayedAction(() -> {
                    this.lcdNextStation(train, info.name(), nextStoppingStation.name(), nextStoppingStation.track());
                }));
            }
        }
    }
    public @Nullable StationInfo getNextStoppingStation(StationInfo current) {
        int index = this.stationList.indexOf(current);
        while (index != -1 && index + 1 < this.stationList.size()) {
            StationInfo next = this.stationList.get(index + 1);
            if (next.stopTime() > 0) {
                return next;
            }
            index += 1;
        }
        return null;
    }
    public boolean isLastStation(String station) {
        return this.stationList.indexOf(this.getStationInfo(station)) == this.stationList.size() - 1;
    }
    /**
     * 约定：列车在start时已经位于第一个车站。此时还不知道开门方向。
     * 站后折返需增加一个虚拟车站
     * 调用bind的时候是周期的开始
     * TODO: 检查时刻表。关门时间由上一个车次决定，当前车次的有效信息只有bind和出发时间。
     */
    public void bind() {
        TrainCartsDiagram.getInstance().getLogger().info("Train " + this.coupleNumber + " of " + this.circuitNumber + " scheduled to start from " + this.stationList.get(0).name() + " at " + this.startTime);
        this.currentStationIndex = 0;
        if (this.stationList.size() >= 2 && this.startTime >= 0) {
            Bukkit.getScheduler().runTaskLater(TrainCartsDiagram.getInstance(), () -> {
                TrainCartsDiagram.getInstance().getLogger().info("Train " + this.coupleNumber + " is operating " + this.circuitNumber);
                TrainProperties prop = TrainPropertiesStore.getRelaxed(this.coupleNumber);
                if (prop == null) {
                    TrainCartsDiagram.getInstance().getLogger().warning("Train " + this.coupleNumber + " not found.");
                    TrainCartsDiagram.getInstance().getLogger().info(TrainPropertiesStore.getAll().toString());
                    return;
                }
                MinecartGroup train = prop.getHolder();
                StationInfo station = this.stationList.get(0);
                train.getActions().addActionWait(station.departureTime() - TimeUtil.getTimetableEquivTime(train.getWorld()));
                prop.clearDestination();
                prop.clearDestinationRoute();
                // Reset Display
                TrainUtil.initialize(train);
                String trainType = TrainUtil.getTrainType(train);
                this.frontDisplay = new ScreenUI(DisplayConfig.getTrainConfig(trainType).getNode("front"));
                this.sideDisplay = new ScreenUI(DisplayConfig.getTrainConfig(trainType).getNode("side"));
                this.lcdDisplay = new ScreenUI(DisplayConfig.getTrainConfig(trainType).getNode("lcd"));
                this.setFrontAndSideDisplay(train);
                this.lcdArrivingAtStation(train, station.name(), station.track());
                // Execute instructions
                TimeTableDiagram.getInstance().updateCircuit(train.getProperties().getTrainName(), this.circuitNumber);
                AbstractInstruction.StationInstructionParams params = new AbstractInstruction.StationInstructionParams(train, station, null);
                this.executeInstructions(params);
                // Launch
                LauncherConfig config = new LauncherConfig();
                config.setAcceleration(prop.getWaitAcceleration());
                train.get(0).getActions().addActionLaunch(config, prop.getSpeedLimit());
            },  this.startTime);
        }
    }
    protected void setFrontAndSideDisplay(MinecartGroup group) {
        String displayName;
        if (!this.operationType.startsWith("SPECIAL")) {
            displayName = this.operationType + "-" + this.bound;
        } else {
            displayName = this.operationType;
        }
        if (this.frontDisplay != null) {
            this.frontDisplay.applyTo(group, displayName);
        }
        if (this.sideDisplay != null) {
            this.sideDisplay.applyTo(group, displayName);
        }
    }

    /**
     * 注意：对于通过的车站，也会出现在StationName和track这里
     * @param group
     * @param currentStation
     * @param nextStoppingStation
     * @param track
     */
    public void lcdNextStation(MinecartGroup group, String currentStation, String nextStoppingStation, String track) {
        if (!this.operationType.startsWith("SPECIAL")) {
            String displayName = this.operationType + "-" + this.bound + "-FROM-" + currentStation + "-TO-" + nextStoppingStation + "-" + track;
            TrainCartsDiagram.getInstance().getLogger().info("Applying display name " + displayName);
            if (this.lcdDisplay != null) {
                this.lcdDisplay.applyTo(group, displayName);
            }
        }
    }
    public void lcdArrivingAtStation(MinecartGroup group, String stationName, String track) {
        if (!this.operationType.startsWith("SPECIAL")) {
            String displayName = this.operationType + "-" + this.bound + "-CURR-" + stationName + "-" + track;
            TrainCartsDiagram.getInstance().getLogger().info("Applying display name " + displayName);
            if (this.lcdDisplay != null) {
                this.lcdDisplay.applyTo(group, displayName);
            }
        }
    }
}
