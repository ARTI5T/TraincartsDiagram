package work.art1st.traincartsdiagram.instructions;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import lombok.Data;
import work.art1st.traincartsdiagram.trains.ManagedTrain;
import work.art1st.traincartsdiagram.TrainCartsDiagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractInstruction {
    @Data
    public static class StationInstructionParams {
        private MinecartGroup train;
        private ManagedTrain.StationInfo stationInfo;
        private boolean shouldReverse;
        private String doorSide;
        public StationInstructionParams(MinecartGroup train, ManagedTrain.StationInfo stationInfo, String doorSide) {
            this.train = train;
            this.stationInfo = stationInfo;
            this.shouldReverse = false;
            this.doorSide = doorSide;
        }
        public StationInstructionParams(MinecartGroup train, ManagedTrain.StationInfo stationInfo) {
            this(train, stationInfo, "");
        }
    }
    private static final Map<String, Class<? extends AbstractInstruction>> registry = new HashMap<>();
    static {
        registry.put("GOTO", GotoInstruction.class);
        registry.put("REV", ReverseInstruction.class);
        registry.put("ASSIGN", AssignInstruction.class);
    }
    public static List<AbstractInstruction> parse(String instructions) {
        TrainCartsDiagram.getInstance().getLogger().info("Instructions " + instructions);
        List<AbstractInstruction> instructionsList = new ArrayList<>();
        String[] parts = instructions.split(";");
        for (String part : parts) {
            String[] args = part.split(" ");
            String instructionName = args[0].toUpperCase();
            if (instructionName.isEmpty()) {
                continue;
            }
            if (registry.containsKey(instructionName)) {
                try {
                    AbstractInstruction instruction = registry.get(instructionName).getDeclaredConstructor().newInstance();
                    instruction.initialize(args);
                    instructionsList.add(instruction);
                    TrainCartsDiagram.getInstance().getLogger().info("Adding instruction " + instructionName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                TrainCartsDiagram.getInstance().getLogger().warning("Unknown instruction: " + instructionName);
            }
        }
        return instructionsList;
    }
    public abstract void initialize(String[] args);
    public abstract void execute(StationInstructionParams params);
}
