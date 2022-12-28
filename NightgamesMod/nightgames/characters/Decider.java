package nightgames.characters;

import nightgames.areas.Area;
import nightgames.characters.custom.effect.CustomEffect;
import nightgames.combat.Combat;
import nightgames.daytime.Daytime;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.match.Action;
import nightgames.match.MatchType;
import nightgames.match.actions.*;
import nightgames.match.ftc.FTCMatch;
import nightgames.match.ftc.Hunter;
import nightgames.match.ftc.Prey;
import nightgames.skills.Skill;
import nightgames.skills.Tactics;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Decider {
    private static void addAllSkillsWithPriority(ArrayList<WeightedSkill> priority, HashSet<Skill> skills,
                    float weight) {
        for (Skill s : skills) {
            priority.add(new WeightedSkill(weight, s));
        }
    }

    /**Creates an ArrayList of weighted Skills to aid in the decisionmaking process. It goes through all available skills and sorts them into lists. 
     * 
     * The method then checks the character's current mood and assigns weights based upon skill type.
     * 
     * @param available
     * 
     * @param c
     * 
     * @param character
     * 
     * @return
     * Returns an ArrayList of weightedSkills.
     * 
     * */
    public static ArrayList<WeightedSkill> parseSkills(HashSet<Skill> available, Combat c, NPC character) {
        HashSet<Skill> damage = new HashSet<Skill>();
        HashSet<Skill> pleasure = new HashSet<Skill>();
        HashSet<Skill> fucking = new HashSet<Skill>();
        HashSet<Skill> position = new HashSet<Skill>();
        HashSet<Skill> debuff = new HashSet<Skill>();
        HashSet<Skill> recovery = new HashSet<Skill>();
        HashSet<Skill> calming = new HashSet<Skill>();
        HashSet<Skill> summoning = new HashSet<Skill>();
        HashSet<Skill> stripping = new HashSet<Skill>();
        HashSet<Skill> misc = new HashSet<Skill>();
        ArrayList<WeightedSkill> priority = new ArrayList<WeightedSkill>();
        for (Skill a : available) {
            if (a.type(c) == Tactics.damage) {
                damage.add(a);
            } else if (a.type(c) == Tactics.pleasure) {
                pleasure.add(a);
            } else if (a.type(c) == Tactics.fucking) {
                fucking.add(a);
            } else if (a.type(c) == Tactics.positioning) {
                position.add(a);
            } else if (a.type(c) == Tactics.debuff) {
                debuff.add(a);
            } else if (a.type(c) == Tactics.recovery) {
                recovery.add(a);
            } else if (a.type(c) == Tactics.calming) {
                calming.add(a);
            } else if (a.type(c) == Tactics.summoning) {
                summoning.add(a);
            } else if (a.type(c) == Tactics.stripping) {
                stripping.add(a);
            } else if (a.type(c) == Tactics.misc) {
                misc.add(a);
            }
        }
        switch (character.mood) {
            
            //Characters that are confident will gain position, strip and debuff the other guy - they do things equally.
            case confident:
                // i can do whatever i want
                addAllSkillsWithPriority(priority, position, 1.0f);
                addAllSkillsWithPriority(priority, stripping, 1.0f);
                addAllSkillsWithPriority(priority, debuff, 1.0f);
                addAllSkillsWithPriority(priority, pleasure, 1.0f);
                addAllSkillsWithPriority(priority, fucking, 1.0f);
                addAllSkillsWithPriority(priority, damage, 1.0f);
                addAllSkillsWithPriority(priority, summoning, .5f);
                addAllSkillsWithPriority(priority, misc, 1f);
                break;
            //Characters that are angry will prioritize doing damage and gaining position.
            case angry:
                addAllSkillsWithPriority(priority, damage, 2.5f);
                addAllSkillsWithPriority(priority, position, 2.0f);
                addAllSkillsWithPriority(priority, debuff, 2.0f);
                addAllSkillsWithPriority(priority, stripping, 1.0f);
                addAllSkillsWithPriority(priority, pleasure, 0.0f);
                addAllSkillsWithPriority(priority, misc, 1f);
                addAllSkillsWithPriority(priority, summoning, 0f);
                break;
            //Characters that are nervous prioritize summoning, debuffing, and recovering from bad situations.
            case nervous:
                addAllSkillsWithPriority(priority, summoning, 2.0f);
                addAllSkillsWithPriority(priority, debuff, 2.0f);
                addAllSkillsWithPriority(priority, calming, 2.0f);
                addAllSkillsWithPriority(priority, recovery, 2.0f);
                addAllSkillsWithPriority(priority, position, 1.0f);
                addAllSkillsWithPriority(priority, damage, .5f);
                addAllSkillsWithPriority(priority, pleasure, 0.0f);
                addAllSkillsWithPriority(priority, misc, 1f);
                break;
            //characters that are desperate will prioritize calming down and recovering.
            case desperate:
                // and probably a bit confused
                addAllSkillsWithPriority(priority, calming, 4.0f);
                addAllSkillsWithPriority(priority, recovery, 4.0f);
                addAllSkillsWithPriority(priority, debuff, 4.0f);
                addAllSkillsWithPriority(priority, misc, 3.0f);
                addAllSkillsWithPriority(priority, position, 2.0f);
                addAllSkillsWithPriority(priority, damage, 2.0f);
                addAllSkillsWithPriority(priority, pleasure, 1.0f);
                addAllSkillsWithPriority(priority, fucking, 1.0f);
                break;
            //characters that are horny will prioritize fucking and stripping, doing pleasure and gaining position.
            case horny:
                addAllSkillsWithPriority(priority, fucking, 5.0f);
                addAllSkillsWithPriority(priority, stripping, 1.0f);
                addAllSkillsWithPriority(priority, pleasure, 1.0f);
                addAllSkillsWithPriority(priority, position, 1.0f);
                addAllSkillsWithPriority(priority, debuff, 0f);
                addAllSkillsWithPriority(priority, misc, 1f);
                break;
            //Characters that feel dominant will prioritize gaining and keeping dominant positions and fucking and stripping.
            case dominant:
                addAllSkillsWithPriority(priority, position, 3.0f);
                addAllSkillsWithPriority(priority, fucking, 2.0f);
                addAllSkillsWithPriority(priority, stripping, 2.0f);
                addAllSkillsWithPriority(priority, pleasure, 2.0f);
                addAllSkillsWithPriority(priority, debuff, 2.0f);
                addAllSkillsWithPriority(priority, damage, 2.0f);
                addAllSkillsWithPriority(priority, summoning, 1.0f);
                addAllSkillsWithPriority(priority, misc, 1f);
                break;
        }
        /*
         * if(character.getArousal().percent()>85||character.getStamina().percent()<10){ priority.add(recovery); priority.add(damage); priority.add(pleasure); } if((c.stance.penetration(character)&&c.stance.dom(character))||c.stance.enumerate()==Stance.sixnine||(c.stance.dom(character)&&c.stance.enumerate()==Stance.behind)){ priority.add(pleasure); priority.add(pleasure); priority.add(damage); priority.add(recovery); } if(!target.canAct()){ priority.add(stripping); priority.add(pleasure); priority.add(summoning); priority.add(position); } else if(!target.nude()&&(target.getArousal().percent()>60||target.getStamina().percent()<50)){ priority.add(stripping); priority.add(pleasure); priority.add(damage); priority.add(position); } else if(c.stance.dom(character)){ priority.add(pleasure); priority.add(stripping); priority.add(summoning); priority.add(damage); priority.add(position); priority.add(debuff); } else{ priority.add(summoning); priority.add(pleasure); priority.add(debuff); priority.add(damage); priority.add(position); priority.add(stripping); priority.add(recovery); }
         */ return priority;
    }

    private static Optional<Action.Instance> searchForAction(Collection<Action.Instance> available, NPC character, Predicate<Action.Instance> predicate) {
        var action = available.stream().filter(predicate).findAny();
        if (action.isEmpty()) {
            var bestMove = bestMove(character, character.location(), predicate);
            if (bestMove.isPresent()) {
                action = available.stream().filter(act -> act.equals(bestMove.get())).findAny();
            }
        }
        return action;
    }
    
    /**This method parses the actions available to the character and returns an action.*/
    public static Action.Instance parseMoves(Collection<Action.Instance> available, Collection<Area> radar, NPC character) {
        var thisParticipant = Global.getMatch().findParticipant(character);
        HashSet<Action.Instance> enemy = new HashSet<>();
        HashSet<Action.Instance> onlyWhenSafe = new HashSet<>();
        HashSet<Action.Instance> utility = new HashSet<>();
        HashSet<Action.Instance> tactic = new HashSet<>();
        if (character.mostlyNude()) {
            var resupplyAction = searchForAction(available, character, act -> act instanceof Resupply.Instance);
            if (resupplyAction.isPresent()) {
                return resupplyAction.get();
            }
        }
        if (character.arousal.percent() >= 40 && !character.location().humanPresent() && radar.isEmpty()) {
            var masturbateAction = available.stream().filter(act -> act instanceof Masturbate.Instance).findAny();
            if (masturbateAction.isPresent()) {
                return masturbateAction.get();
            }
        }
        if (character.stamina.percent() <= 60 || character.arousal.percent() >= 30) {
            var batheAction = searchForAction(available, character, act -> act instanceof Bathe.Instance);
            if (batheAction.isPresent()) {
                return batheAction.get();
            }
        }
        if (character.get(Attribute.Science) >= 1 && !character.has(Item.Battery, 10)) {
            var rechargeAction = searchForAction(available, character, act -> act instanceof Recharge.Instance);
            if (rechargeAction.isPresent()) {
                return rechargeAction.get();
            }
        }
        FTCMatch match;
        if (Global.getMatch().getType() == MatchType.FTC) {
            match = (FTCMatch) Global.getMatch();
            if (thisParticipant instanceof Prey && match.getFlagHolder() == null) {
                var action = searchForAction(available, character,
                        act -> act instanceof Move.Instance && ((Move.Instance) act).getDestination().name.equals("Central Camp"));
                if (action.isPresent()) {
                    return action.get();
                }
            } else if (thisParticipant instanceof Hunter && character.has(Item.Flag)) {
                return searchForAction(available, character, act -> act instanceof Resupply.Instance)
                        .orElseThrow(() -> new RuntimeException(String.format("Found no resupply for %s.", thisParticipant.getCharacter().getTrueName())));
            }
        }
        for (var act : available) {
            if (act instanceof Move.Instance && radar.contains(((Move.Instance) act).getDestination())) {
                enemy.add(act);
            } else if (act instanceof Bathe.Instance
                    || act instanceof Craft.Instance
                    || act instanceof Scavenge.Instance
                    || act instanceof Hide.Instance
                    || act instanceof SetTrap.Instance
                    || act instanceof nightgames.match.actions.Wait.Instance
                    // TODO: The next two I do NOT understand
                    // If two weeks go by an I haven't figured out why they're here, remove them.
                    // Written 2020-03-03
                    || (act instanceof Move.Instance && ((Move.Instance) act).getDestination().name.equals("Engineering"))
                    || (act instanceof Move.Instance && ((Move.Instance) act).getDestination().name.equals("Dining"))
                    || act instanceof Disguise.Instance) {
                onlyWhenSafe.add(act);
            } else {
                utility.add(act);
            }
        }
        
        if (character.plan == Plan.hunting && !enemy.isEmpty()) {
            tactic.addAll(enemy);
        }
        if (!character.location().humanPresent()) {
            tactic.addAll(onlyWhenSafe);
        }
        tactic.addAll(utility);
        if (tactic.isEmpty()) {
            tactic.addAll(available);
        }
        // give disguise some priority when just picking something random
        if (Global.random(5) == 0) {
            var maybeDisguise = tactic.stream().filter(a -> a instanceof Disguise.Instance).findAny();
            if (maybeDisguise.isPresent()) {
                return maybeDisguise.get();
            }
        }
        Action.Instance[] actions = tactic.toArray(new Action.Instance[tactic.size()]);
        if (actions.length == 0) return null;
        return actions[Global.random(actions.length)];
    }

    /**This method determines what a character decides to do during the day.*/
    public static void visit(Character self) {
        if (Global.checkCharacterDisabledFlag(self)) {
            return;
        }
        int max = 0;
        Character bff = null;
        if (!self.attractions.isEmpty()) {
            for (String key : self.attractions.keySet()) {
                Character friend = Global.getCharacterByType(key);
                if (self.getAttraction(friend) > max && !friend.human()) {
                    max = self.getAttraction(friend);
                    bff = friend;
                }
            }
            if (bff != null) {
                self.gainAffection(bff, Global.random(3) + 1);
                bff.gainAffection(self, Global.random(3) + 1);
                switch (Global.random(3)) {
                    case 0:
                        Daytime.train(self, bff, Attribute.Power);
                    case 1:
                        Daytime.train(self, bff, Attribute.Cunning);
                    default:
                        Daytime.train(self, bff, Attribute.Seduction);
                }
            }
        }
    }

    //TODO: Document this method.
    public static Skill prioritizeNew(Character self, List<WeightedSkill> plist, Combat c) {
        if (plist.isEmpty()) {
            return null;
        }
        // The higher, the better the AI will plan for "rare" events better
        final int RUN_COUNT = 5;
        // Decrease to get an "easier" AI. Make negative to get a suicidal AI.
        final double RATING_FACTOR = 0.02f;

        // Starting fitness
        Character other = c.getOpponentCharacter(self);
        double selfFit = self.getFitness(c);
        double otherFit = self.getOtherFitness(c, other);

        // Now simulate the result of all actions
        ArrayList<WeightedSkill> moveList = new ArrayList<>();
        double sum = 0;
        for (WeightedSkill wskill : plist) {
            // Run it a couple of times
            double rating, raw_rating = 0;
            if (wskill.skill.type(c) == Tactics.fucking && self.has(Trait.experienced)) {
                wskill.weight += 1.0;
            }
            if (wskill.skill.type(c) == Tactics.damage && self.has(Trait.sadist)) {
                wskill.weight += 1.0;
            }
            for (int j = 0; j < RUN_COUNT; j++) {
                raw_rating += rateMove(self, wskill.skill, c, selfFit, otherFit);
            }

            if (self instanceof NPC) {
                wskill.weight += ((NPC)self).ai.getAiModifiers().modAttack(wskill.skill.getClass());
            }
            // Sum up rating, add to map
            rating = (double) Math.pow(2, RATING_FACTOR * raw_rating + wskill.weight + wskill.skill.priorityMod(c)
                            + Global.getMatch().getCondition().getSkillModifier().encouragement(wskill.skill, c, self));
            sum += rating;
            moveList.add(new WeightedSkill(sum, raw_rating, rating, wskill.skill));
        }
        if (sum == 0 || moveList.size() == 0) {
            return null;
        }
        // Select
        double s = Global.randomdouble() * sum;
        for (WeightedSkill entry : moveList) {
            if (entry.weight > s) {
                return entry.skill;
            }
        }
        return moveList.get(moveList.size() - 1).skill;
    }

    //TODO: Document this method.
    private static double rateMove(Character self, Skill skill, Combat c, double selfFit, double otherFit) {
        return rateAction(self, c, selfFit, otherFit, (combat, selfCopy, other) -> {
            skill.setSelf(selfCopy);
            skill.resolve(combat, other);
            skill.setSelf(self);
            return true;
        });
    }

    //TODO: Document this method
    private static Character getCopyFromCombat(Combat c, Combat clonedCombat, Character self) {
        if (c.getP1Character() == self) {
            return clonedCombat.getP1Character();
        } else if (c.getP2Character() == self) {
            return clonedCombat.getP2Character();
        } else if (c.getPetCombatants().contains(self)) {
            return clonedCombat.getPetCombatants().stream().filter(other -> other.equals(self)).findAny().get();
        } else {
            throw new IllegalArgumentException("Tried to use a badly cloned combat");
        }
    }

    
    public static double rateAction(Character skillUser, Combat c, double selfFit, double otherFit, CustomEffect effect) {
        return rateActionWithObserver(skillUser, skillUser, c.getOpponentCharacter(skillUser), c, selfFit, otherFit, effect);
    }

    /**Clones the combat and returns a rating.
     * TODO: Document this method properly.
     * */
    public static double rateActionWithObserver(Character skillUser, Character fitnessObserver, Character target,
                    Combat c, double selfFit, double otherFit, CustomEffect effect) {
        // Clone ourselves a new combat... This should clone our characters, too
        Combat c2;
        c2 = c.clone();

        Global.debugSimulation += 1;
        Character newSkillUser = getCopyFromCombat(c, c2, skillUser);
        Character newObserver = getCopyFromCombat(c, c2, fitnessObserver);
        Character newOpponent = c2.getOpponentCharacter(newSkillUser);
        Character newTarget = getCopyFromCombat(c, c2, target);

        effect.execute(c2, newSkillUser, newTarget);
        Global.debugSimulation -= 1;
        double selfFitnessDelta = newObserver.getFitness(c) - selfFit;
        double otherFitnessDelta = newObserver.getOtherFitness(c, newOpponent) - otherFit;
        return selfFitnessDelta - otherFitnessDelta;
    }

    // finds the best Move to get to an Area with an Action that satisfies the predicate
    public static Optional<Move.Instance> bestMove(Character c, Area initial, Predicate<Action.Instance> predicate) {
        var p = Global.getMatch().findParticipant(c);
        if (initial.possibleActions(p).stream().anyMatch(predicate)) {
            throw new RuntimeException("current room already satisfies predicate");
        }
        ArrayDeque<Area> queue = new ArrayDeque<>();
        List<Area> vector = new ArrayList<>();
        HashMap<Area, Area> parents = new HashMap<>();
        queue.push(initial);
        vector.add(initial);
        Area last = null;
        while (!queue.isEmpty()) {
            Area t = queue.pop();
            parents.put(t, last);
            var possibleActions = t.possibleActions(p);
            var possibleMoves = possibleActions.stream()
                    .filter(action -> action instanceof Move.Instance)
                    .map(action -> (Move.Instance) action)
                    .collect(Collectors.toUnmodifiableSet());
            var adjacent = possibleMoves.stream()
                    .map(Move.Instance::getDestination)
                    .collect(Collectors.toSet());
            if (possibleActions.stream().anyMatch(predicate)) {
                while (!adjacent.contains(t)) {
                    t = parents.get(t);
                }
                final var nextDest = t;
                return possibleMoves.stream()
                        .filter(move -> move.getDestination().equals(nextDest))
                        .findAny();
            }
            adjacent.stream()
                    .filter(Predicate.not(vector::contains))
                    .forEach(area -> {
                        vector.add(area);
                        queue.push(area);
                    });
            last = t;
        }
        return Optional.empty();
    }
}