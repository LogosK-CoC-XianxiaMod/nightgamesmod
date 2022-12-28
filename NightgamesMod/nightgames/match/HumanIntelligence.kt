package nightgames.match

import nightgames.characters.Attribute
import nightgames.characters.Player
import nightgames.combat.HumanIntelligence
import nightgames.global.Global
import nightgames.gui.commandpanel.CommandPanelOption
import nightgames.items.Item
import nightgames.match.Encounter.IntrusionOption
import nightgames.match.actions.Move
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.function.Consumer

class HumanIntelligence(private val character: Player) : Intelligence {
    override fun move(possibleActions: Collection<Action.Instance>,
                      callback: Consumer<Action.Instance>) {
        possibleActions.stream()
                .filter { act: Action.Instance? -> act is Move.Instance }
                .map { act: Action.Instance -> act as Move.Instance }
                .filter { act: Move.Instance -> act.maybeDetectOccupancy(character[Attribute.Perception]) }
                .forEach { act: Move.Instance ->
                    character.message("You hear something in the <b>" + act.destination.name + "</b>.")
                    act.destination.isPinged = true
                }
        val optionChoices = possibleActions.map { action: Action.Instance ->
            CommandPanelOption(action.getName()) { callback.accept(action); Global.getMatch().resume() }
        }
        assert(!optionChoices.isEmpty())
        character.gui.presentOptions(optionChoices)
        Global.getMatch().pause()
    }

    override fun promptTrap(target: Participant, attackContinuation: Runnable, waitContinuation: Runnable) {
        character.message("Do you want to take the opportunity to ambush <b>" + target.character.getName() + "</b>?")
        assessOpponent(target)
        character.message("<br/>")
        val options = java.util.ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Attack " + target.character.getName(),
                encounterOption {
                    attackContinuation.run()
                    Global.getMatch().resume()
                }))
        options.add(CommandPanelOption("Wait",
                encounterOption {
                    waitContinuation.run()
                    Global.getMatch().resume()
                }))
        character.gui.presentOptions(options)
        Global.getMatch().pause()
    }

    override fun faceOff(opponent: Participant, fightContinuation: Runnable, fleeContinuation: Runnable, smokeContinuation: Runnable) {
        character.gui.message("You run into <b>" + opponent.character.nameDirectObject()
                + "</b> and you both hesitate for a moment, deciding whether to attack or retreat.")
        assessOpponent(opponent)
        character.gui.message("<br/>")
        val options = java.util.ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Fight",
                encounterOption {
                    fightContinuation.run()
                    Global.getMatch().resume()
                }))
        options.add(CommandPanelOption("Flee",
                encounterOption {
                    fleeContinuation.run()
                    Global.getMatch().resume()
                }))
        character.gui.presentOptions(options)
        Global.getMatch().pause()
    }

    override fun spy(opponent: Participant, ambushContinuation: Runnable, waitContinuation: Runnable) {
        character.gui.message("You spot <b>" + opponent.character.nameDirectObject()
                + "</b> but she hasn't seen you yet. You could probably catch her off guard, or you could remain hidden and hope she doesn't notice you.")
        assessOpponent(opponent)
        character.gui.message("<br/>")
        val options = java.util.ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Ambush",
                encounterOption {
                    ambushContinuation.run()
                    Global.getMatch().resume()
                }))
        options.add(CommandPanelOption("Wait",
                encounterOption {
                    waitContinuation.run()
                    Global.getMatch().resume()
                }))
        character.gui.presentOptions(options)
        Global.getMatch().pause()
    }

    override fun showerScene(target: Participant, ambushContinuation: Runnable, stealContinuation: Runnable, aphrodisiacContinuation: Runnable, waitContinuation: Runnable) {
        if (target.location.name == "Showers") {
            character.gui.message("You hear running water coming from the first floor showers. There shouldn't be any residents on this floor right now, so it's likely one "
                    + "of your opponents. You peek inside and sure enough, <b>" + target.character.subject()
                    + "</b> is taking a shower and looking quite vulnerable. Do you take advantage "
                    + "of her carelessness?")
        } else if (target.location.name == "Pool") {
            character.gui.message("You stumble upon <b>" + target.character.nameDirectObject()
                    + "</b> skinny dipping in the pool. She hasn't noticed you yet. It would be pretty easy to catch her off-guard.")
        }
        assessOpponent(target)
        character.gui.message("<br/>")
        val options = java.util.ArrayList<CommandPanelOption>()
        options.add(CommandPanelOption("Surprise Her",
                encounterOption {
                    ambushContinuation.run()
                    Global.getMatch().resume()
                }))
        if (!target.character.mostlyNude()) {
            options.add(CommandPanelOption("Steal Clothes",
                    encounterOption {
                        stealContinuation.run()
                        Global.getMatch().resume()
                    }))
        }
        if (character.has(Item.Aphrodisiac)) {
            options.add(CommandPanelOption("Use Aphrodisiac",
                    encounterOption {
                        aphrodisiacContinuation.run()
                        Global.getMatch().resume()
                    }))
        }
        options.add(CommandPanelOption("Do Nothing",
                encounterOption {
                    waitContinuation.run()
                    Global.getMatch().resume()
                }))
        character.gui.presentOptions(options)
        Global.getMatch().pause()
    }

    override fun intrudeInCombat(intrusionOptions: Set<IntrusionOption>, possibleMoves: List<Move.Instance>, actionCallback: Consumer<Action.Instance>, neitherContinuation: Runnable) {
        val listOptions = java.util.ArrayList(intrusionOptions)
        assert(listOptions.size == 2) { "No support for more than 2 combatants" }
        character.gui.message("You find <b>" + listOptions[0].targetCharacter.getName() + "</b> and <b>" + listOptions[1].targetCharacter.getName()
                + "</b> fighting too intensely to notice your arrival. If you intervene now, it'll essentially decide the winner.")
        character.gui.message("Then again, you could just wait and see which one of them comes out on top. It'd be entertaining,"
                + " at the very least. Alternatively, you could just leave them to it.")
        val options = listOptions.map { option: IntrusionOption ->
            CommandPanelOption("Help " + option.targetCharacter.getName()) {
                character.gui.watchCombat(option.combat); option.callback()
            }
        }.toMutableList()
        options.add(CommandPanelOption("Watch them fight") { event: ActionEvent? -> neitherContinuation.run() })
        options.addAll(possibleMoves.map { move: Move.Instance ->
                CommandPanelOption(move.getName()) { actionCallback.accept(move); Global.getMatch().resume() }
            }
        )
        Global.getMatch().pause()
        character.gui.presentOptions(options)
    }

    private fun assessOpponent(opponent: Participant) {
        val arousal: String
        val stamina: String
        if (character[Attribute.Perception] >= 6) {
            character.gui.message("She is level " + opponent.character.progression.level)
        }
        if (character[Attribute.Perception] >= 8) {
            character.gui.message("Her Power is " + opponent.character[Attribute.Power] + ", her Cunning is "
                    + opponent.character[Attribute.Cunning] + ", and her Seduction is "
                    + opponent.character[Attribute.Seduction])
        }
        opponent.state.sendAssessmentMessage(opponent, character)
        if (character[Attribute.Perception] >= 4) {
            arousal = if (opponent.character.arousal
                            .percent() > 70) {
                "horny"
            } else if (opponent.character.arousal
                            .percent() > 30) {
                "slightly aroused"
            } else {
                "composed"
            }
            stamina = if (opponent.character.stamina
                            .percent() < 50) {
                "tired"
            } else {
                "eager"
            }
            character.gui.message("She looks $stamina and $arousal.")
        }
    }

    private fun encounterOption(continuation: Runnable): ActionListener {
        return ActionListener { event: ActionEvent? -> continuation.run() }
    }

    override fun makeCombatIntelligence(): HumanIntelligence {
        return HumanIntelligence(character)
    }
}