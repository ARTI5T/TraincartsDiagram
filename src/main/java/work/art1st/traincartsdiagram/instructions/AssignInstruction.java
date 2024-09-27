package work.art1st.traincartsdiagram.instructions;

import work.art1st.traincartsdiagram.TrainCartsDiagram;

public class AssignInstruction extends AbstractInstruction {
    private String newName;
    @Override
    public void initialize(String[] args) {
        if (args.length == 2) {
            this.newName = args[1];
        } else {
            TrainCartsDiagram.getInstance().getLogger().warning("Invalid arguments while parsing GOTO instruction");
        }
    }

    @Override
    public void execute(StationInstructionParams params) {
        params.getTrain().getProperties().setTrainName(this.newName);
    }
}
