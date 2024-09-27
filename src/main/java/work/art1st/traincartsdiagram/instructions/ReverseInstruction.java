package work.art1st.traincartsdiagram.instructions;

import work.art1st.traincartsdiagram.TrainCartsDiagram;

public class ReverseInstruction extends AbstractInstruction {
    @Override
    public void initialize(String[] args) {

    }

    @Override
    public void execute(StationInstructionParams params) {
        params.setShouldReverse(true);
        TrainCartsDiagram.getInstance().getLogger().info("Reversing train " + params.getTrain().getProperties().getTrainName());
    }
}
