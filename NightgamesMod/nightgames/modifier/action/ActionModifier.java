package nightgames.modifier.action;

import nightgames.characters.Character;
import nightgames.match.Action;
import nightgames.match.Match;
import nightgames.modifier.ModifierCategory;
import nightgames.modifier.ModifierComponent;

import java.util.*;
import java.util.function.BiPredicate;

public abstract class ActionModifier implements ModifierCategory<ActionModifier>, ModifierComponent {
    public static final ActionModifierLoader loader = new ActionModifierLoader();
    public static final ActionModifierCombiner combiner = new ActionModifierCombiner();

    public Set<Action> bannedActions() {
        return Collections.emptySet();
    }

    public Map<Action, BiPredicate<Character, Match>> conditionalBans() {
        return Collections.emptyMap();
    }

    public boolean actionIsBanned(Action act, Character user, Match match) {
        return bannedActions().contains(act)
                        || conditionalBans().containsKey(act) && conditionalBans().get(act).test(user, match);
    }

    @Override public ActionModifier combine(ActionModifier next) {
        ActionModifier first = this;
        return new ActionModifier() {
            @Override
            public Set<Action> bannedActions() {
                Set<Action> actions = new HashSet<>(first.bannedActions());
                actions.addAll(next.bannedActions());
                return Collections.unmodifiableSet(actions);
            }

            @Override
            public Map<Action, BiPredicate<Character, Match>> conditionalBans() {
                Map<Action, BiPredicate<Character, Match>> actions = new HashMap<>(first.conditionalBans());
                actions.putAll(next.conditionalBans());
                return Collections.unmodifiableMap(actions);
            }

            @Override
            public String toString() {
                return first.toString() + next.toString();
            }

            public String name() {
                return first.name() + " then " + next.name();
            }
        };
    }

    @Override
    public abstract String toString();
}
