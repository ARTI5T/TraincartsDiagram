package work.art1st.traincartsdiagram;

import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import work.art1st.traincartsdiagram.ETRC.ETRCDiagramParser;
import work.art1st.traincartsdiagram.commands.DisplayControl;
import work.art1st.traincartsdiagram.commands.TestRunCommand;
import work.art1st.traincartsdiagram.commands.TrainDiagramCommand;
import work.art1st.traincartsdiagram.display.DisplayConfig;

import java.io.*;
import java.util.Objects;

public final class TrainCartsDiagram extends JavaPlugin {

    @Getter
    private static TrainCartsDiagram instance;
    private static final SignActionManagedStation managedStation = new SignActionManagedStation();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Objects.requireNonNull(Bukkit.getPluginCommand("tcdiagram")).setExecutor(new TrainDiagramCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("testrun")).setExecutor(new TestRunCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("tcdisplay")).setExecutor(new DisplayControl());
        SignAction.register(managedStation);
        this.loadDiagram();
        DisplayConfig.init();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        SignAction.unregister(managedStation);
        this.stop();
    }

    public boolean loadDiagram() {
        File diagramFile = new File(getDataFolder(), "diagram.json");
        if (!diagramFile.exists()) {
            diagramFile.getParentFile().mkdirs();
            this.getLogger().warning("Diagram file not found.");
            return false;
        }
        try {
            ETRCDiagramParser parser = (new Gson()).fromJson(new JsonReader(new InputStreamReader(new FileInputStream(diagramFile), "UTF-8")), ETRCDiagramParser.class);
            TimeTableDiagram diagram = parser.parse();
            TimeTableDiagram.setInstance(diagram);
            Bukkit.getScheduler().cancelTasks(this);
            /* TODO: 处理世界名称 */
            Bukkit.getScheduler().runTaskTimer(this, diagram::reset, 24000 - Objects.requireNonNull(getServer().getWorld("world")).getTime(), TimeUtil.getTimePeriod());
        } catch (FileNotFoundException e) {
            this.getLogger().warning("Diagram file not found.");
            return false;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public void stop() {
        Bukkit.getScheduler().cancelTasks(this);
    }
}
