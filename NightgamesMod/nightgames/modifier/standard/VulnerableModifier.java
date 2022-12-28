package nightgames.modifier.standard;

import nightgames.characters.Player;
import nightgames.global.Global;
import nightgames.modifier.BaseModifier;
import nightgames.modifier.status.StatusModifier;
import nightgames.status.DurationStatus;
import nightgames.status.Hypersensitive;

public class VulnerableModifier extends BaseModifier {

    public static final String NAME = "vulnerable";

    public VulnerableModifier() {
        status = new StatusModifier(new Hypersensitive(DurationStatus.dummyNPCForInit), true);
    }

    @Override
    public int bonus() {
        return 75;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String intro() {
        return "<i>\"I've got a simple handicap for you tonight. You've probably come across some sensitization potions that temporarily enhance your sense of touch, right? "
                        + "There's a cream that has basically the same effect, but it'll last for several hours. The deal is that I'll apply the cream, making you much "
                        + "more vulnerable during the match, and you'll get an extra $" + bonus()
                        + " per victory. Interested?\"</i>";
    }

    @Override
    public String acceptance() {
        Player player = Global.getPlayer();
        if (player.hasDick() && !player.hasPussy()) {
            return "Lilly leads you into the men's bathroom to apply the hypersensitivity cream. She removes your clothing and starts to rub the cream onto your dick. The stuff works "
                        + "fast and you can't help letting out a quiet moan at her soft touch. She treats the process very clinically and seems almost bored to be handling your manhood. <i>\"I hope "
                        + "you don't take my lack of interest personally,\"</i> she says, as if reading your mind. <i>\"You seem nice, and I guess you're reasonably good looking. You just happen to be "
                        + "the wrong gender.\"</i> Ah, that explains a bit. That must make this more awkward for her than it would otherwise be. She shrugs. <i>\"I don't mind. I'm used to competing against "
                        + "men, and I have some pride in my technique.\"</i> As she says this, her hand motions turn into smooth, pleasurable strokes. <i>\"Besides, with the typical gender ratio in these "
                        + "games, I'm better off than the straight girls. Are you feeling any effect yet?\"</i> You certainly are. Between the cream and her skilled handjob, you can barely stay standing. "
                        + "She continues stroking you until you shoot your load into her hands. <i>\"That was quick. I'm going to assume the cream was effective rather than you having a fetish for girls "
                        + "who aren't attracted to you.\"</i>";
        } else if (player.hasDick() && player.hasPussy()) {
            return "Lilly leads you into the bathroom to apply the hypersensitivity cream. She removes your clothing and starts to rub the cream onto your dick and into the folds of your pussy. "
                        + "The stuff works fast and you can't help letting out a quiet moan at her soft touch. She treats the process clinically and seems almost bored when handling your penis. "
                        + "<i>\"I hope you don't take my lack of interest personally,\"</i> she says, as if reading your mind. <i>\"You seem nice, and I guess you're reasonably good looking. I'm "
                        + "simply not into phalluses.\"</i> Ah, that explains a bit. That must make this more awkward for her than it would otherwise be. She shrugs. <i>\"I don't mind. I'm used to "
                        + "competing against men, and I have some pride in my technique.\"</i> As she says this, one hand begins giving your shaft smooth, pleasurable strokes while the other gently "
                        + "caresses your nether lips. <i>\"Besides, with the typical gender ratio in these games, I'm better off than the straight girls. Are you feeling any effect yet?\"</i> "
                        + "You certainly are. Between the cream and her skilled handjob, you can barely stay standing. She continues stroking you until you shoot your load into her hands. "
                        + "<i>\"That was quick. I'm going to assume the cream was effective rather than you having a fetish for girls who aren't attracted to you.\"</i>";
        } else if (!player.hasDick() && player.hasPussy()) {
            return "Lilly leads you into the women's bathroom to apply the hypersensitivity cream. She removes your clothing and starts to rub the cream into the folds of your pussy. "
                        + "The stuff works fast and you can't help letting out a quiet moan at her soft touch. Lilly attempts to maintain her detached air, but you notice a faint flush in her "
                        + "cheeks as her fingers grow slick with your arousal.  She seems to be struggling with herself for a long moment before coming to a decision, swearing under "
                        + "her breath.  A sudden bolt of pleasure makes your knees weak as two of Lilly's fingers plunge into your depths.  You lean on her for support, your soft moans "
                        + "accompanying the lewd sounds of your pussy being skillfully pleasured.  In mere moments you cum in her arms, trembling. Lilly gives you a minute to recover "
                        + "before letting you regain your feet and turning to wash her hands.  <i>\"Given what I've seen of your endurance, the cream seems to be working,\"</i> she says, "
                        + "voice slightly hoarse.";
        } else {
            return "Lilly leads you into the bathroom to apply the hypersensitivity cream. She removes your clothing and starts to rub the cream into your asshole. "
                            + "The stuff works fast and you can't help letting out a quiet moan at her soft touch. She treats the process clinically and seems almost bored. "
                            + "<i>\"I hope you don't take my lack of interest personally,\"</i> she says, as if reading your mind. <i>\"You seem nice, and I guess you're reasonably good looking. I'm "
                            + "simply attracted to women.\"</i> Ah, that explains a bit. That must make this more awkward for her than it would otherwise be. She shrugs. <i>\"I don't mind. It's my job"
                            + "and I'm good at it.\"</i> As she says this a single finger slides into your ass. <i>\"Are you feeling any effect yet?\"</i> "
                            + "You certainly are. Between the cream and her skilled touch, you can barely stay standing. She continues fingering you until you climax in her hands. "
                            + "<i>\"That was quick. I'm going to assume the cream was effective rather than you having a fetish for girls who aren't attracted to you.\"</i>";
        }
    }

}
