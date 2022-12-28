package nightgames.characters;

import nightgames.characters.corestats.ArousalStat;
import nightgames.characters.corestats.StaminaStat;
import nightgames.characters.corestats.WillpowerStat;

public class CoreStatsGrowth {
    static CoreStatsGrowth newDefault() {
        var stamina = new CoreStatGrowth<StaminaStat>(2, 2);
        var arousal = new CoreStatGrowth<ArousalStat>(4, 3);
        var willpower = new CoreStatGrowth<WillpowerStat>(1.0f, .25f);
        return new CoreStatsGrowth(stamina, arousal, willpower);
    }

    private final CoreStatGrowth<StaminaStat> stamina;
    private final CoreStatGrowth<ArousalStat> arousal;
    private final CoreStatGrowth<WillpowerStat> willpower;

    CoreStatsGrowth(CoreStatGrowth<StaminaStat> stamina,
        CoreStatGrowth<ArousalStat> arousal,
        CoreStatGrowth<WillpowerStat> willpower) {
        this.stamina = stamina;
        this.arousal = arousal;
        this.willpower = willpower;
    };

    void levelUp(Character c) {
        stamina.levelUp(c.stamina);
        arousal.levelUp(c.arousal);
        willpower.levelUp(c.willpower);
    }
}
