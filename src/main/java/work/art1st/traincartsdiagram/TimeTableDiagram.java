package work.art1st.traincartsdiagram;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import lombok.Getter;
import lombok.Setter;
import work.art1st.traincartsdiagram.trains.ManagedTrain;

import java.util.HashMap;
import java.util.Map;

public class TimeTableDiagram {
    /* 运用号->运用计划 */
    private final Map<String, ManagedTrain> trains;
    /* 运用号->运用计划 */
    private final Map<String, ManagedTrain> nonScheduledTrains;
    /* 编组号->运用号，初始状态 */
    private final HashMap<String, String> _coupleToCircuit;
    /* 编组号->运用号 */
    private HashMap<String, String> currentCoupleToCircuit;
    private static final ManagedTrain defaultTrain = new ManagedTrain();
    @Getter
    @Setter
    private static TimeTableDiagram instance;

    @SuppressWarnings("unchecked")
    public TimeTableDiagram(HashMap<String, String> coupleToCircuit, Map<String, ManagedTrain> trains) {
        this._coupleToCircuit = coupleToCircuit;
        this.currentCoupleToCircuit = (HashMap<String, String>) coupleToCircuit.clone();
        this.trains = trains;
        this.nonScheduledTrains = new HashMap<>();
    }
    /* TODO: 跨日列车 */
    @SuppressWarnings("unchecked")
    public void reset() {
        TrainCartsDiagram.getInstance().getLogger().info("Resetting TimeTableDiagram");
        this.currentCoupleToCircuit = (HashMap<String, String>) this._coupleToCircuit.clone();
        for (ManagedTrain train : this.trains.values()) {
            train.bind();
        }
        this.nonScheduledTrains.clear();
    }
    public ManagedTrain getTrainFromGroup(MinecartGroup train) {
        String trainNumber = train.getProperties().getTrainName();
        if (this.currentCoupleToCircuit.containsKey(trainNumber)) {
            String circuitNumber = this.currentCoupleToCircuit.get(trainNumber);
            if (this.trains.containsKey(circuitNumber)) {
                return this.trains.get(circuitNumber);
            }
            if (this.nonScheduledTrains.containsKey(circuitNumber)) {
                return this.nonScheduledTrains.get(circuitNumber);
            }
        }
        return defaultTrain;
    }
    public void updateCircuit(String couple, String circuit) {
        this.currentCoupleToCircuit.put(couple, circuit);
    }
    public void addNonScheduledTrain(String circuit, ManagedTrain train) {
        this.nonScheduledTrains.put(circuit, train);
    }
}
