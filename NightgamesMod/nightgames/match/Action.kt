package nightgames.match

import nightgames.areas.Area

abstract class Action protected constructor(name: String) {
    protected val name: String

    init {
        this.name = name
    }

    abstract fun usable(user: Participant?): Boolean
    abstract inner class Instance protected constructor(@JvmField protected val user: Participant, protected val location: Area) {
        abstract fun execute()
        fun getName(): String {
            return name
        }

        protected fun messageOthersInLocation(message: String?) {
            location.occupants.stream()
                    .filter { p: Participant -> p != user }
                    .forEach { p: Participant -> p.character.message(message) }
        }
    }

    class Ready : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return true
        }

        override fun move(p: Participant) {}
        override val isDetectable: Boolean
            get() = true

        override fun eligibleCombatReplacement(encounter: Encounter, p: Participant, other: Participant) = null

        override fun ineligibleCombatReplacement(p: Participant, other: Participant) = null

        override fun spotCheckDifficultyModifier(p: Participant): Int {
            return 0
        }
    }

    abstract class Busy protected constructor(private var roundsToWait: Int) : Participant.State {
        override fun allowsNormalActions(): Boolean {
            return roundsToWait <= 0
        }

        override fun move(p: Participant) {
            if (roundsToWait-- <= 0) {
                moveAfterDelay(p)
            }
        }

        protected abstract fun moveAfterDelay(p: Participant?)
    }

    interface LocationDescription {
        fun describeLocation(): String?
    }

    abstract fun newInstance(user: Participant?, location: Area?): Instance
    override fun toString(): String {
        return name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Action && name == other.name
    }
}