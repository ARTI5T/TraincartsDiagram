package work.art1st.traincartsdiagram;

import lombok.Getter;
import org.bukkit.World;

public class TimeUtil {
    @Getter
    private static long timePeriod = 3 * 24000L;

    /* 转换为新的周期 */
    public static long getTimetableEquivTime(World world) {
        return world.getGameTime() % timePeriod;
    }

    public static long convertFrom24hTicksToEquivTime(long ticks24h) {
        return ticks24h * 3;
    }

    public static long convertFromEquivTimeTo24hTicks(long equivTime) {
        return equivTime / 3;
    }
}
