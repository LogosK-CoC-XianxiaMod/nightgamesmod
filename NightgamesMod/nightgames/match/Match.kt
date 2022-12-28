package nightgames.match

import nightgames.areas.*
import nightgames.areas.Area.Companion.addDoor
import nightgames.characters.Attribute
import nightgames.characters.Character
import nightgames.characters.Trait
import nightgames.global.Flag
import nightgames.global.Global
import nightgames.match.Action.Ready
import nightgames.match.actions.*
import nightgames.match.actions.ResupplyNormal.EscapeRoute
import nightgames.match.defaults.DefaultPostmatch
import nightgames.modifier.BaseModifier
import nightgames.status.addiction.Addiction
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import java.awt.Rectangle
import java.time.LocalTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Collectors

open class Match protected constructor(participants: Set<Participant>?, protected var map: Map<String, Area>, condition: BaseModifier) {
    interface Trigger {
        fun fire(m: Match?)
    }

    var rawTime: LocalTime
        protected set
    @JvmField
    protected var participants: Set<Participant>
    private var pause: Boolean
    var condition: BaseModifier
        protected set
    private val beforeRoundTriggers: MutableList<Trigger> = ArrayList(java.util.List.of(Challenge.SpawnTrigger()))
    private var roundIterator: Iterator<Participant>
    fun start() {
        Global.getPlayer().addictions.forEach(Consumer { addiction ->
            addiction.startNight()?.let { status ->
                findParticipant(Global.getPlayer()).character.addNonCombat(Status(status))
            }
        })
        Global.gui().startMatch()
        participants.forEach(Consumer { participant: Participant ->
            val combatant = participant.character
            Global.gainSkills(combatant)
            Global.learnSkills(combatant)
            combatant.addictions.forEach(Consumer { addiction ->
                addiction.startNight()?.let { status -> combatant.addNonCombat(Status(status)) }
            })
            combatant.matchPrep(this)
            combatant.stamina.renew()
            combatant.arousal.renew()
            combatant.mojo.renew()
            combatant.willpower.renew()
            if (combatant.getPure(Attribute.Science) > 0) {
                combatant.chargeBattery()
            }
            manageConditions(participant)
        })
        placeCharacters()
        round()
    }

    open val type: MatchType?
        get() = MatchType.NORMAL
    val availableActions: Set<Action>
        get() = Global.getActions()

    protected fun manageConditions(p: Participant) {
        condition.handleOutfit(p.character)
        condition.handleItems(p)
        condition.handleStatus(p.character)
        condition.handleTurn(p.character, this)
    }

    private fun placeCharacters() {
        val areaList: Deque<Area?> = ArrayDeque()
        areaList.add(map["Dorm"])
        areaList.add(map["Engineering"])
        areaList.add(map["Liberal Arts"])
        areaList.add(map["Dining"])
        areaList.add(map["Union"])
        areaList.add(map["Bridge"])
        areaList.add(map["Library"])
        areaList.add(map["Tunnel"])
        areaList.add(map["Workshop"])
        areaList.add(map["Pool"])
        participants.forEach(Consumer { participant: Participant ->
            if (participant.character.has(Trait.immobile)) {
                participant.place(map["Courtyard"]!!)
            } else {
                participant.place(areaList.pop()!!)
            }
        })
    }

    init {
        rawTime = startTime
        this.participants = HashSet(participants)
        pause = false
        this.condition = condition
        roundIterator = Collections.emptyIterator()
    }

    fun round() {
        while (!rawTime.isBefore(startTime.plusHours(3))) {
            if (!roundIterator.hasNext()) {
                // prepare next round
                roundIterator = participants.iterator()
                beforeRoundTriggers.forEach(Consumer { trigger: Trigger -> trigger.fire(this) })
                rawTime = rawTime.plusMinutes(5)
            }
            areas.forEach(Consumer { area: Area -> area.isPinged = false })
            while (roundIterator.hasNext()) {
                val participant = roundIterator.next()
                Global.gui().refresh()
                participant.endOfMatchRound()
                participant.timePasses()
                manageConditions(participant)
                participant.move()
                if (pause) {
                    return
                }
            }
        }
        end()
    }

    protected open fun afterEnd() {}
    private fun decideWinner(): Optional<Character> {
        return participants.stream()
                .max(Comparator.comparing(Participant::score))
                .map(Participant::character)
    }

    private fun giveWinnerPrize(winner: Character, output: StringBuilder) {
        winner.modMoney(winner.prize() * 5)
        output.append(Global.capitalizeFirstLetter(winner.subject()))
                .append(" won the match, earning an additional $")
                .append(winner.prize() * 5)
                .append("<br/>")
        if (!winner.human()) {
            output.append(winner.victoryLiner(null, null))
                    .append("<br/>")
        }
    }

    private fun calculateReward(combatant: Participant, output: StringBuilder): Int {
        val reward = AtomicInteger()
        participants.forEach(Consumer { participant: Participant ->
            val other = participant.character
            while (combatant.character.has(other.trophy)) {
                combatant.character.consume(other.trophy, 1, false)
                reward.addAndGet(other.prize())
            }
        })
        if (combatant.character.human()) {
            output.append("You received $")
                    .append(reward.get())
                    .append(" for turning in your collected trophies.<br/>")
        }
        for (c in combatant.challenges) {
            if (c.done) {
                val r = c.reward() + c.reward() * 3 * combatant.character.progression.rank
                reward.addAndGet(r)
                if (combatant.character.human()) {
                    output.append("You received $")
                            .append(r)
                            .append(" for completing a ")
                            .append(c.describe())
                }
            }
        }
        return reward.get()
    }

    private fun end() {
        participants.forEach(Consumer { obj: Participant -> obj.finishMatch() })
        Global.gui().clearText()
        val sb = StringBuilder("Tonight's match is over.<br/><br/>")
        val winner = decideWinner()
        val player = Global.getPlayer()
        participants.stream().forEachOrdered { p: Participant ->
            val combatant = p.character
            sb.append(scoreString(combatant, p.score, "in total"))
            sb.append("<br/>")
            combatant.modMoney(p.score * combatant.prize())
            combatant.modMoney(calculateReward(p, sb))
            p.challenges.clear()
            p.state = Ready()
            condition.undoItems(combatant)
            combatant.change()
        }
        val playerParticipant = findParticipant(player)
        sb.append("<br/>You earned $")
                .append(playerParticipant.score * player.prize())
                .append(" for scoring ")
                .append(playerParticipant.score)
                .append(" points.<br/>")
        val bonus = playerParticipant.score * condition.bonus()
        player.modMoney(bonus)
        if (bonus > 0) {
            sb.append("You earned an additional $")
                    .append(bonus)
                    .append(" for accepting the handicap.<br/>")
        }
        winner.ifPresent { w: Character -> giveWinnerPrize(w, sb) }
        if (winner.filter { obj: Character -> obj.human() }
                        .isPresent) {
            Global.flag(Flag.victory)
        }
        val potentialDates = participants.stream()
                .map(Participant::character)
                .filter { c: Character -> c.getAffection(player) >= 15 }
                .collect(Collectors.toSet())
        if (potentialDates.isEmpty()) {
            Global.gui().message("You walk back to your dorm and get yourself cleaned up.")
        } else {
            potentialDates.stream()
                    .max(Comparator.comparing { c: Character -> c.getAffection(player) })
                    .orElseThrow()
                    .afterParty()
        }
        participants.stream()
                .map(Participant::character)
                .forEach { character: Character ->
                    if (character.getFlag("heelsTraining") >= 50 && !character.hasPure(Trait.proheels)) {
                        if (character.human()) {
                            sb.append("<br/>You've gotten comfortable at fighting in heels.<br/><b>Gained Trait: Heels Pro</b>\n")
                        }
                        character.add(Trait.proheels)
                    }
                    if (character.getFlag("heelsTraining") >= 100 && !character.hasPure(Trait.masterheels)) {
                        if (character.human()) {
                            sb.append("<br/>You've mastered fighting in heels.<br/><b>Gained Trait: Heels Master</b>\n")
                        }
                        character.add(Trait.masterheels)
                    }
                }
        Global.getPlayer()
                .addictions
                .forEach(Consumer { obj: Addiction -> obj.endNight() })
        Global.gui()
                .message(sb.toString())
        afterEnd()
        val post: Postmatch = DefaultPostmatch(participants.stream()
                .map(Participant::character)
                .collect(Collectors.toList()))
        post.run()
    }

    val hour: Int
        get() = rawTime.hour

    fun getTime(): String {
        return String.format("%1d:%01d", rawTime.hour, rawTime.minute)
    }

    val areas: Collection<Area>
        get() = map.values

    fun pause() {
        pause = true
    }

    fun resume() {
        pause = false
        round()
    }

    val combatants: List<Character>
        get() = participants.map(Participant::character)

    @Deprecated("")
    fun findParticipant(c: Character): Participant = participants.single { p: Participant -> p.character == c }

    fun getParticipants(): Set<Participant> {
        return java.util.Set.copyOf(participants)
    }

    companion object {
        private val startTime = LocalTime.of(22, 0, 0)
        @JvmStatic fun newMatch(combatants: Collection<Character?>, condition: BaseModifier): Match {
            val quad = Area("Quad", DescriptionModule.quad(), java.util.Set.of(AreaAttribute.Open))
            val dorm = Area("Dorm", DescriptionModule.dorm())
            val shower = Area("Showers", DescriptionModule.shower())
            val laundry = Area("Laundry Room", DescriptionModule.laundry())
            val engineering = Area("Engineering", DescriptionModule.engineering())
            val lab = Area("Chemistry Lab", DescriptionModule.lab())
            val workshop = Area("Workshop", DescriptionModule.workshop())
            val libarts = Area("Liberal Arts", DescriptionModule.liberalArts())
            val pool = Area("Pool", DescriptionModule.pool())
            val library = Area("Library", DescriptionModule.library())
            val dining = Area("Dining Hall", DescriptionModule.diningHall())
            val kitchen = Area("Kitchen", DescriptionModule.kitchen())
            val storage = Area("Storage Room", DescriptionModule.storage())
            val tunnel = Area("Tunnel", DescriptionModule.tunnel())
            val bridge = Area("Bridge", DescriptionModule.bridge())
            val sau = Area("Student Union", DescriptionModule.studentUnion())
            val courtyard = Area("Courtyard", DescriptionModule.courtyard())
            quad.setMapDrawHint(MapDrawHint(Rectangle(10, 3, 7, 9), "Quad", false))
            dorm.setMapDrawHint(MapDrawHint(Rectangle(14, 12, 3, 5), "Dorm", false))
            shower.setMapDrawHint(MapDrawHint(Rectangle(13, 17, 4, 2), "Showers", false))
            laundry.setMapDrawHint(MapDrawHint(Rectangle(17, 15, 8, 2), "Laundry", false))
            engineering.setMapDrawHint(MapDrawHint(Rectangle(10, 0, 7, 3), "Eng", false))
            lab.setMapDrawHint(MapDrawHint(Rectangle(0, 0, 10, 3), "Lab", false))
            workshop.setMapDrawHint(MapDrawHint(Rectangle(17, 0, 8, 3), "Workshop", false))
            libarts.setMapDrawHint(MapDrawHint(Rectangle(5, 5, 5, 7), "L&A", false))
            pool.setMapDrawHint(MapDrawHint(Rectangle(6, 12, 4, 2), "Pool", false))
            library.setMapDrawHint(MapDrawHint(Rectangle(0, 8, 5, 12), "Library", false))
            dining.setMapDrawHint(MapDrawHint(Rectangle(17, 6, 4, 6), "Dining", false))
            kitchen.setMapDrawHint(MapDrawHint(Rectangle(18, 12, 4, 2), "Kitchen", false))
            storage.setMapDrawHint(MapDrawHint(Rectangle(21, 6, 4, 5), "Storage", false))
            tunnel.setMapDrawHint(MapDrawHint(Rectangle(23, 11, 2, 4), "Tunnel", true))
            bridge.setMapDrawHint(MapDrawHint(Rectangle(0, 3, 2, 5), "Bridge", true))
            sau.setMapDrawHint(MapDrawHint(Rectangle(10, 12, 3, 5), "S.Union", true))
            courtyard.setMapDrawHint(MapDrawHint(Rectangle(6, 14, 3, 6), "Courtyard", true))

            // Right loop
            addDoor(quad, dorm)
            addDoor(dorm, shower)
            addDoor(dorm, laundry)
            addDoor(laundry, tunnel)
            addDoor(tunnel, storage)
            addDoor(storage, dining)
            addDoor(dining, kitchen)
            addDoor(dining, quad)

            // Left loop
            addDoor(quad, sau)
            addDoor(sau, pool)
            addDoor(pool, courtyard)
            addDoor(pool, libarts)
            addDoor(libarts, quad)
            addDoor(libarts, library)
            addDoor(library, bridge)
            addDoor(bridge, lab)
            addDoor(lab, engineering)
            addDoor(engineering, workshop)
            addDoor(engineering, quad)
            workshop.shortcut(pool)
            pool.shortcut(workshop)
            library.shortcut(tunnel)
            tunnel.shortcut(library)
            lab.jump(dining)
            bridge.jump(quad)
            dorm.getPossibleActions().add(Hide())
            dorm.getPossibleActions().add(ResupplyNormal(java.util.Set.of(EscapeRoute(quad,
                    "You hear your opponents searching around the "
                            + "dorm, so once you finish changing, you hop out the window and "
                            + "head to the quad."),
                    EscapeRoute(laundry,
                            "You hear your opponents searching around "
                                    + "the dorm, so once you finish changing, you quietly move "
                                    + "downstairs to the laundry room."))))
            shower.getPossibleActions().add(Bathe.newShower())
            shower.getPossibleActions().add(Hide())
            laundry.getPossibleActions().add(Hide())
            engineering.getPossibleActions().add(Hide())
            lab.getPossibleActions().add(Craft())
            lab.getPossibleActions().add(Hide())
            workshop.getPossibleActions().add(Hide())
            workshop.getPossibleActions().add(Recharge())
            workshop.getPossibleActions().add(Scavenge())
            libarts.getPossibleActions().add(Hide())
            libarts.getPossibleActions().add(Energize())
            pool.getPossibleActions().add(Bathe.newPool())
            pool.getPossibleActions().add(Hide())
            library.getPossibleActions().add(Hide())
            dining.getPossibleActions().add(Hide())
            kitchen.getPossibleActions().add(Craft())
            kitchen.getPossibleActions().add(Hide())
            storage.getPossibleActions().add(Hide())
            storage.getPossibleActions().add(Scavenge())
            sau.getPossibleActions().add(Hide())
            sau.getPossibleActions().add(ResupplyNormal(java.util.Set.of(
                    EscapeRoute(quad,
                            "You don't want to be ambushed leaving the "
                                    + "student union, so once you finish changing, you hop out the "
                                    + "window and head to the quad."),
                    EscapeRoute(pool,
                            "You don't want to be ambushed leaving "
                                    + "the student union, so once you finish changing, you sneak out "
                                    + "the back door and head to the pool."))))
            courtyard.getPossibleActions().add(Hide())
            val cacheLocations = java.util.Set.of(dorm, shower, laundry, engineering, lab, workshop, libarts, pool, library, dining,
                    kitchen, storage, sau, courtyard)
            val map = HashMap(java.util.Map.of("Quad", quad))
            map["Dorm"] = dorm
            map["Shower"] = shower
            map["Laundry"] = laundry
            map["Engineering"] = engineering
            map["Workshop"] = workshop
            map["Lab"] = lab
            map["Liberal Arts"] = libarts
            map["Pool"] = pool
            map["Library"] = library
            map["Dining"] = dining
            map["Kitchen"] = kitchen
            map["Storage"] = storage
            map["Tunnel"] = tunnel
            map["Bridge"] = bridge
            map["Union"] = sau
            map["Courtyard"] = courtyard
            val m = Match(combatants.stream()
                    .map { c: Character? -> Participant(c!!, condition.getActionFilterFor(c)) }
                    .collect(Collectors.toSet()),
                    map,
                    condition)
            m.beforeRoundTriggers.add(Cache.SpawnTrigger(cacheLocations))
            return m
        }

        val SCORING_TEMPLATE = JtwigTemplate.inlineTemplate(
                "{{- self.subject() }} scored {{ score }} point{{- (score != 1) ? 's' : '' }} {{ reason }}.")

        fun scoreString(combatant: Character?, amt: Int, reason: String?): String {
            val model = JtwigModel()
                    .with("self", combatant)
                    .with("score", amt)
                    .with("reason", reason)
            return SCORING_TEMPLATE.render(model)
        }
    }
}