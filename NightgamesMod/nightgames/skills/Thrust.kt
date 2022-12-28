package nightgames.skills

import nightgames.characters.Attribute
import nightgames.characters.Character
import nightgames.characters.Trait
import nightgames.characters.body.AssPart
import nightgames.characters.body.BodyPart
import nightgames.characters.body.CockPart
import nightgames.combat.Combat
import nightgames.combat.Result
import nightgames.global.Global
import nightgames.nskills.tags.SkillTag
import nightgames.skills.damage.Staleness
import nightgames.stance.Stance
import nightgames.status.BodyFetish
import nightgames.status.addiction.AddictionType

open class Thrust @JvmOverloads constructor(name: String?, self: Character?, staleness: Staleness? = Staleness.build().withDecay(.05).withDefault(1.0).withRecovery(.10).withFloor(.5)) : Skill(name, self, 0, staleness) {
    init {
        addTag(SkillTag.fucking)
        addTag(SkillTag.thrusting)
        addTag(SkillTag.pleasureSelf)
    }

    constructor(self: Character?) : this("Thrust", self)

    override fun requirements(c: Combat, user: Character, target: Character): Boolean {
        return !user.has(Trait.temptress) || user[Attribute.Technique] < 11
    }

    protected fun havingSex(c: Combat, target: Character): Boolean {
        return (getSelfOrgan(c, target) != null && getTargetOrgan(c, target) != null && self.canRespond()
                && (c.getStance().havingSexOtherNoStrapped(c, self)
                || c.getStance().partsForStanceOnly(c, self, target).stream().anyMatch { part: BodyPart ->
            part.isType(
                    CockPart.TYPE)
        }))
    }

    override fun usable(c: Combat, target: Character): Boolean {
        return havingSex(c, target) && c.getStance().canthrust(c, self)
    }

    open fun getSelfOrgan(c: Combat, target: Character?): BodyPart? {
        return if (c.getStance().penetratedBy(c, target, self)) {
            self.body.randomInsertable
        } else if (c.getStance().anallyPenetratedBy(c, self, target)) {
            self.body.randomAss
        } else if (c.getStance().vaginallyPenetratedBy(c, self, target)) {
            self.body.randomPussy
        } else {
            null
        }
    }

    open fun getTargetOrgan(c: Combat, target: Character): BodyPart? {
        if (c.getStance().penetratedBy(c, self, target)) {
            return target.body.randomInsertable
        } else if (c.getStance().anallyPenetratedBy(c, target, self)) {
            return target.body.randomAss
        } else if (c.getStance().vaginallyPenetratedBy(c, target, self)) {
            return target.body.randomPussy
        }
        return null
    }

    open fun getDamage(c: Combat, target: Character): IntArray {
        val results = IntArray(2)
        var m = 8 + Global.random(11)
        if (c.getStance().anallyPenetrated(c, target) && self.has(Trait.assmaster)) {
            m *= 1.5.toInt()
        }
        var mt = Math.max(1f, m / 3f)
        if (self.has(Trait.experienced)) {
            mt = Math.max(1f, mt * .66f)
        }
        mt = target.modRecoilPleasure(c, mt)
        if (self.checkAddiction(AddictionType.BREEDER, target)) {
            val bonus: Float = .3f * (self.getAddiction(AddictionType.BREEDER)?.combatSeverity?.ordinal ?: 0)
            mt += mt * bonus
        }
        if (target.checkAddiction(AddictionType.BREEDER, self)) {
            val bonus: Float = .3f * (target.getAddiction(AddictionType.BREEDER)?.combatSeverity?.ordinal ?: 0)
            m += (m * bonus).toInt()
        }
        results[0] = m
        results[1] = mt.toInt()
        return results
    }

    override fun resolve(c: Combat, target: Character): Boolean {
        val selfO = getSelfOrgan(c, target)
        val targetO = getTargetOrgan(c, target)
        if (selfO == null || targetO == null) {
            System.err.println("Something very odd happened during " + javaClass.simpleName + ", stance is " + c.getStance())
            System.err.println(self.save().toString())
            System.err.println(target.save().toString())
            c.write("Something very weird happened, please make a bug report with the logs.")
            return false
        }
        val result: Result
        result = if (c.getStance().penetratedBy(c, self, c.getStance().getPartner(c, self))) {
            Result.reverse
        } else if (c.getStance().en == Stance.anal) {
            Result.anal
        } else {
            Result.normal
        }
        writeOutput(c, result, target)
        val m = getDamage(c, target)
        assert(m.size >= 2)
        if (m[0] != 0) {
            target.body.pleasure(self, selfO, targetO, m[0].toDouble(), c, this)
        }
        if (m[1] != 0) {
            self.body.pleasure(target, targetO, selfO, m[1].toDouble(), c, this)
        }
        if (selfO.isType(AssPart.TYPE) && Global.random(100) < 2 + self[Attribute.Fetish]) {
            target.add(c, BodyFetish(target, self, AssPart.TYPE, .25))
        }
        return true
    }

    override fun getMojoBuilt(c: Combat): Int {
        return 0
    }

    override fun copy(user: Character): Skill {
        return Thrust(user)
    }

    override fun type(c: Combat): Tactics {
        return Tactics.fucking
    }

    override fun deal(c: Combat, damage: Int, modifier: Result, target: Character): String {
        return if (modifier == Result.anal) {
            "You thrust steadily into " + target.getName() + "'s ass, eliciting soft groans of pleasure."
        } else if (modifier == Result.reverse) {
            Global.format("You rock your hips against {other:direct-object}, riding {other:direct-object} smoothly. "
                    + "Despite the slow pace, {other:subject} soon starts gasping and mewing with pleasure.",
                    self, target)
        } else {
            ("You thrust into " + target.getName()
                    + " in a slow, steady rhythm. She lets out soft breathy moans in time with your lovemaking. You can't deny you're feeling "
                    + "it too, but by controlling the pace, you can hopefully last longer than she can.")
        }
    }

    override fun receive(c: Combat, damage: Int, modifier: Result, target: Character): String {
        return if (modifier == Result.anal) {
            var res: String
            res = if (self.has(Trait.strapped)) {
                String.format("%s thrusts her hips, pumping her artificial cock in and out"
                        + " of %s ass and pushing on %s %s.", self.subject(),
                        target.nameOrPossessivePronoun(), target.possessiveAdjective(),
                        if (target.hasBalls()) "prostate" else "innermost parts")
            } else {
                String.format("%s cock slowly pumps the inside of %s rectum.",
                        self.nameOrPossessivePronoun(), target.nameOrPossessivePronoun())
            }
            if (self.has(Trait.assmaster)) {
                res += String.format(" %s penchant for fucking people in the ass makes "
                        + "%s thrusting that much more powerful, and that much more "
                        + "intense for the both of %s.", self.nameOrPossessivePronoun(),
                        self.possessiveAdjective(),
                        c.bothDirectObject(target))
            }
            res
        } else if (modifier == Result.reverse) {
            String.format("%s rocks %s hips against %s, riding %s smoothly and deliberately. "
                    + "Despite the slow pace, the sensation of %s hot %s surrounding "
                    + "%s dick is gradually driving %s to %s limit.", self.subject(),
                    self.possessiveAdjective(), target.nameDirectObject(),
                    target.objectPronoun(), self.nameOrPossessivePronoun(),
                    getSelfOrgan(c, target)!!.describe(self),
                    target.nameOrPossessivePronoun(), target.objectPronoun(),
                    target.possessiveAdjective())
        } else {
            Global.format(
                    "{self:subject} thrusts into {other:name-possessive} {other:body-part:pussy} in a slow steady rhythm, leaving {other:direct-object} gasping.",
                    self, target)
        }
    }

    override fun describe(c: Combat): String {
        return "Slow fuck, minimizes own pleasure"
    }

    override fun getName(c: Combat): String {
        return if (c.getStance().penetratedBy(c, c.getStance().getPartner(c, self), self)) {
            "Thrust"
        } else {
            "Ride"
        }
    }

    override fun getDefaultTarget(c: Combat): Character {
        return c.getStance().getPartner(c, self)
    }

    override fun makesContact(c: Combat): Boolean {
        return true
    }

    override fun getStage(): Stage {
        return Stage.FINISHER
    }
}