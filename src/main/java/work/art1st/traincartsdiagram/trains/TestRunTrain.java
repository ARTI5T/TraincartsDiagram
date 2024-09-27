package work.art1st.traincartsdiagram.trains;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import org.bukkit.World;
import work.art1st.traincartsdiagram.TimeUtil;
import work.art1st.traincartsdiagram.TrainCartsDiagram;
import work.art1st.traincartsdiagram.instructions.AbstractInstruction;

import java.util.List;

public class TestRunTrain extends ManagedTrain {
    public TestRunTrain(List<StationInfo> stationInfoList, MinecartGroup train) {
        super(stationInfoList, train.getProperties().getTrainName(), "TESTRUN_" + train.getProperties().getTrainName(), "", "", "SPECIAL_TESTRUN", 0);
    }

    @Override
    public long getStopTimeMs(String station, MinecartGroup train) {
        StationInfo info = this.getStationInfo(station);
        if (info != null && info.stopTime() > 0) {
            return 10 * 1000;
        }
        return 0;
    }

    private static String convertTime(World world) {
        long time = TimeUtil.convertFromEquivTimeTo24hTicks(TimeUtil.getTimetableEquivTime(world));
        long hours = time / 1000;
        long minutes = time % 1000 * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public void executeInstructions(AbstractInstruction.StationInstructionParams params) {
        TrainCartsDiagram.getInstance().getLogger().info("Arriving at station " + params.getStationInfo().name() + " at time " + convertTime(params.getTrain().getWorld()));
        super.executeInstructions(params);
    }

    @Override
    public void gotoNextStation(StationInfo stationInfo, MinecartGroup train) {
        TrainCartsDiagram.getInstance().getLogger().info("Departing from station " + stationInfo.name() + " at time " + convertTime(train.getWorld()));
        super.gotoNextStation(stationInfo, train);
    }
}
