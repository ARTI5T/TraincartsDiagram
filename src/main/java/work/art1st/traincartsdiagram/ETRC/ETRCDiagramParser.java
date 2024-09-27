package work.art1st.traincartsdiagram.ETRC;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import work.art1st.traincartsdiagram.trains.ManagedTrain;
import work.art1st.traincartsdiagram.TimeTableDiagram;
import work.art1st.traincartsdiagram.TimeUtil;
import work.art1st.traincartsdiagram.TrainCartsDiagram;
import work.art1st.traincartsdiagram.instructions.AbstractInstruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ETRCDiagramParser {
    @Getter
    public static class LineNotes {
        private String note;
    }
    public static class Line {
        private String name;
        private LineNotes notes;
        public void parseCode(Map<String, String> stationCodeMap) {
            for (String part : notes.note.split("\n")) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    stationCodeMap.put(keyValue[0], keyValue[1]);
                    TrainCartsDiagram.getInstance().getLogger().info("Adding station code: " + keyValue[0] + " -> " + keyValue[1]);
                }
            }
        }
    }
    @Getter
    private static class TimeTableNode {
        @SerializedName("ddsj")
        private String arrival;
        public long getArrival() {
            return asDaytimeTick(arrival);
        }
        @SerializedName("cfsj")
        private String departure;
        public long getDeparture() {
            return asDaytimeTick(departure);
        }
        @SerializedName("zhanming")
        private String station;
        private String track;
        public String getTrack() {
            return track.split(":")[0];
        }
        @SerializedName("note")
        private String instructions;
        public String getInstructions() {
            return instructions == null ? "" : instructions;
        }
        private static long asDaytimeTick(String time) {
            /* Converted into minecraft daytime (tick) */
            String[] hhmmss = time.split(":");
            return TimeUtil.convertFrom24hTicksToEquivTime((Integer.parseInt(hhmmss[0]) * 3600L + Integer.parseInt(hhmmss[1]) * 60L + Integer.parseInt(hhmmss[2])) * 5 / 18);
        }
        public long getStopTime() {
            return (asDaytimeTick(departure) - asDaytimeTick(arrival));
        }
    }
    @Getter
    private static class Train {
        @SerializedName("checi")
        private List<String> number;
        @SerializedName("zdz")
        @Getter
        private String bound;
        @SerializedName("type")
        @Getter
        private String operationType;
        public @NotNull String getNumber() {
            if (number == null || number.isEmpty()) {
                return "";
            }
            return number.get(0);
        }
        public @NotNull String getDirection() {
            if (number == null || number.isEmpty()) {
                return "";
            }
            return number.get(1);
        }
        private List<TimeTableNode> timetable;
    }
    @Getter
    private static class CircuitNode {
        @SerializedName("checi")
        private String number;
    }
    @Getter
    public static class Circuit {
        private String name;
        private String note;
        private List<CircuitNode> order;
        public String getCouple() {
            return note;
        }
    }
    private Line line;
    private List<Line> lines;
    private List<Train> trains;
    private List<Circuit> circuits;

    public TimeTableDiagram parse() {
        Map<String, String> stationCodeMap = new HashMap<>();
        Map<String, Train> trainMap = new HashMap<>();
        Map<String, ManagedTrain> managedTrainMap = new HashMap<>();
        HashMap<String, String> coupleToCircuit = new HashMap<>();
        line.parseCode(stationCodeMap);
        for (Line line : lines) {
            line.parseCode(stationCodeMap);
        }
        for (Train train : trains) {
            trainMap.put(train.getNumber(), train);
        }
        for (Circuit circuit : this.circuits) {
            if (circuit.getOrder().isEmpty() || circuit.getCouple().isEmpty()) {
                TrainCartsDiagram.getInstance().getLogger().warning("Invalid circuit: " + circuit.getName());
                continue;
            }
            coupleToCircuit.put(circuit.getCouple(), circuit.getOrder().get(0).getNumber());
            /* 遍历交路 */
            for (int i = 0; i < circuit.getOrder().size(); i++) {
                CircuitNode circuitNode = circuit.getOrder().get(i);
                TrainCartsDiagram.getInstance().getLogger().info("Parsing circuit: " + circuitNode.getNumber() + " using train number " + circuit.getNote());
                if (trainMap.containsKey(circuitNode.getNumber())) {
                    Train train = trainMap.get(circuitNode.getNumber());
                    /* 排除无效项 */
                    if (train.getTimetable().isEmpty()) {
                        TrainCartsDiagram.getInstance().getLogger().warning("Invalid train: " + train.getNumber());
                        continue;
                    }
                    /* 创建车站列表 */
                    List<ManagedTrain.StationInfo> stationList = new ArrayList<>();
                    for (TimeTableNode node : train.getTimetable()) {
                        ManagedTrain.StationInfo stationInfo = new ManagedTrain.StationInfo(
                                stationCodeMap.get(node.getStation()),
                                node.getStopTime(),
                                node.getDeparture(),
                                node.getTrack(),
                                AbstractInstruction.parse(node.getInstructions())
                        );
                        stationList.add(stationInfo);
                        TrainCartsDiagram.getInstance().getLogger().info("Adding station " + stationCodeMap.get(node.getStation()) + " leaving at " + node.getDeparture());
                    }
                    /* 设置下一车次 */
//                    if (!stationList.isEmpty() && i < circuit.getOrder().size() - 1) {
//                        ManagedTrain.StationInfo terminalStation = stationList.get(stationList.size() - 1);
//                        CircuitNode nextCircuitNode = circuit.getOrder().get(i + 1);
//                        terminalStation.instructions().add(new ResetCircuitNumberInstruction(nextCircuitNode.getNumber()));
//                    }
                    ManagedTrain managedTrain = new ManagedTrain(stationList, circuit.getCouple(), train.getNumber(), train.getDirection(), stationCodeMap.get(train.getBound()), train.getOperationType(), train.getTimetable().get(0).getDeparture());
                    /* 设置本车次的出发时间 */
                    //managedTrain.setStartTime(train.getTimetable().get(0).getDeparture());
                    managedTrainMap.put(circuitNode.getNumber(), managedTrain);
                }
            }
        }
        return new TimeTableDiagram(coupleToCircuit, managedTrainMap);
    }
}
