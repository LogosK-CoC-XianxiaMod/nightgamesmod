package nightgames.match;

import nightgames.actions.Action;
import nightgames.actions.Move;
import nightgames.areas.Area;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.State;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.status.Stsflag;

import java.util.*;
import java.util.stream.Collectors;

public class Participant {
    private Character character;
    private int score = 0;
    private int roundsToWait = 0;

    // Participants this participant has defeated recently.  They are not valid targets until they resupply.
    private Set<Participant> invalidTargets = new HashSet<>();

    public Participant(Character c) {
        this.character = c;
    }

    Participant(Participant p) {
        try {
            this.character = this.getCharacter().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        this.score = p.score;
        this.invalidTargets = p.invalidTargets;
        this.roundsToWait = p.roundsToWait;
    }

    public Character getCharacter() {
        return character;
    }

    int getScore() {
        return score;
    }

    void incrementScore(int i) {
        score += i;
    }

    void defeated(Participant p) {
        assert !invalidTargets.contains(p);
        invalidTargets.add(p);
        incrementScore(1);
    }

    public Participant copy() {
        return new Participant(this);
    }

    void allowTarget(Participant p) {
        invalidTargets.remove(p);
    }

    public void place(Area loc) {
        character.location.set(loc);
        loc.place(this);
        if (loc.name.isEmpty()) {
            throw new RuntimeException("empty location");
        }
    }

    public boolean canStartCombat(Participant p2) {
        return character.eligible(p2.character);
    }

    public interface ActionCallback {
        Action.Aftermath execute(Action a);
    }


    private Collection<Item> craftItems() {
        int roll = Global.random(15);
        if (character.check(Attribute.Cunning, 25)) {
            if (roll == 9) {
                return List.of(Item.Aphrodisiac, Item.DisSol);
            } else if (roll >= 5) {
                return List.of(Item.Aphrodisiac);
            } else {
                return List.of(Item.Lubricant, Item.Sedative);
            }
        } else if (character.check(Attribute.Cunning, 20)) {
            if (roll == 9) {
                return List.of(Item.Aphrodisiac);
            } else if (roll >= 7) {
                return List.of(Item.DisSol);
            } else if (roll >= 5) {
                return List.of(Item.Lubricant);
            } else if (roll >= 3) {
                return List.of(Item.Sedative);
            } else {
                return List.of(Item.EnergyDrink);
            }
        } else if (character.check(Attribute.Cunning, 15)) {
            if (roll == 9) {
                return List.of(Item.Aphrodisiac);
            } else if (roll >= 8) {
                return List.of(Item.DisSol);
            } else if (roll >= 7) {
                return List.of(Item.Lubricant);
            } else if (roll >= 6) {
                return List.of(Item.EnergyDrink);
            }
        } else {
            if (roll >= 7) {
                return List.of(Item.Lubricant);
            } else if (roll >= 5) {
                return List.of(Item.Sedative);
            }
        }
        return List.of();
    }

    private Collection<Item> searchItems() {
        int roll = Global.random(10);
        switch (roll) {
            case 9:
                return List.of(Item.Tripwire, Item.Tripwire);
            case 8:
                return List.of(Item.ZipTie, Item.ZipTie, Item.ZipTie);
            case 7:
                return List.of(Item.Phone);
            case 6:
                return List.of(Item.Rope);
            case 5:
                return List.of(Item.Spring);
            default:
                return List.of();
        }
    }


    public void move() {
        character.displayStateMessage(character.location.get().getTrap(this));
        var possibleActions = new ArrayList<Action>();
        possibleActions.addAll(character.location.get().possibleActions(this));
        possibleActions.addAll(character.getItemActions());
        possibleActions.addAll(Global.getMatch().getAvailableActions());
        possibleActions.removeIf(a -> !a.usable(this));
        if (character.state == State.combat) {
            if (!character.location.get().fight.battle()) {
                Global.getMatch().resume();
            }
            return;
        } else if (roundsToWait > 0) {
            roundsToWait--;
            return;
        } else if (this.character.is(Stsflag.enthralled)) {
            character.handleEnthrall(act -> act.execute(this));
            return;
        } else if (character.state == State.shower || character.state == State.lostclothes) {
            character.bathe();
            character.state = State.ready;
            return;
        } else if (character.state == State.crafting) {
            character.craft(craftItems());
            character.state = State.ready;
            return;
        } else if (character.state == State.searching) {
            character.search(searchItems());
            character.state = State.ready;
            return;
        } else if (character.state == State.resupplying) {
            character.resupply();
            return;
        } else if (character.state == State.webbed) {
            character.state = State.ready;
            return;
        } else if (character.state == State.masturbating) {
            character.masturbate();
            character.state = State.ready;
            return;
        }
        character.move(possibleActions, character.location.get().encounter(this), act -> act.execute(this));
    }

    public void flee(Area area) {
        var options = character.location.get().possibleActions(this);
        var destinations = options.stream()
                .filter(action -> action instanceof Move)
                .map(action -> (Move) action)
                .map(Move::getDestination)
                .collect(Collectors.toList());
        var destination = destinations.get(Global.random(destinations.size()));
        character.notifyFlight(destination);
        character.travel(destination);
        area.endEncounter();
    }

    public void waitRounds(int i) {
        roundsToWait += i;
    }

    public void endOfMatchRound() {
        character.endOfMatchRound();
    }

    public Area getLocation() { return character.location(); }
}
