package work.art1st.traincartsdiagram.display;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import work.art1st.traincartsdiagram.TrainCartsDiagram;

public class DisplayConfig {
    private static ConfigurationNode config;
    public static void init() {
        FileConfiguration c = new FileConfiguration(TrainCartsDiagram.getInstance(), "display.yml");
        c.load();
        config = c;
    }

    public static ConfigurationNode getTrainConfig(String trainType) {
        return config.getNode(trainType);
    }
}
