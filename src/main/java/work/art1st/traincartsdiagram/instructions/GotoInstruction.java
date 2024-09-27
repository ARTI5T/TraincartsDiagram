package work.art1st.traincartsdiagram.instructions;

import work.art1st.traincartsdiagram.TrainCartsDiagram;

public class GotoInstruction extends AbstractInstruction {
    private String destination;
    @Override
    public void initialize(String[] args) {
        if (args.length == 2) {
            this.destination = args[1];
        } else {
            TrainCartsDiagram.getInstance().getLogger().warning("Invalid arguments while parsing GOTO instruction");
        }
    }

    @Override
    public void execute(StationInstructionParams params) {
        params.getTrain().getProperties().addDestinationToRoute(this.destination);
        TrainCartsDiagram.getInstance().getLogger().info("Adding destination " + this.destination + " to train " + params.getTrain().getProperties().getTrainName());
    }
}
