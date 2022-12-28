package nightgames.status

import com.google.gson.JsonObject
import nightgames.characters.Attribute
import nightgames.characters.Character
import nightgames.characters.Emotion
import nightgames.characters.body.BodyPart
import nightgames.characters.body.SkinPart
import nightgames.combat.Combat
import nightgames.global.Global
import nightgames.skills.FootWorship
import nightgames.skills.Masturbate
import nightgames.stance.Engulfed
import nightgames.stance.Kneeling
import java.util.*

class Parasited(affected: Character, private val other: Character) : Status("parasited", affected) {
    private var time = 0.0
    private var stage = 0

    init {
        flag(Stsflag.parasited)
        flag(Stsflag.debuff)
        flag(Stsflag.purgable)
    }

    override fun initialMessage(c: Combat?, replacement: Status?): String {
        return Global.format(
                "{other:SUBJECT-ACTION:have|has} planted a part of {other:reflective} in {self:name-possessive} head!\n", affected, other)
    }

    override fun describe(opponent: Character): String {
        return String.format("%s a part of %s inside of %s head.", affected.subjectAction("have", "has"),
                other.nameOrPossessivePronoun(), affected.possessiveAdjective())
    }

    override fun fitnessModifier(): Float {
        return (-40).toFloat()
    }

    override fun mod(a: Attribute): Int {
        return 0
    }

    override fun tick(c: Combat) {
        if (time >= 3) {
            if (stage < 3) {
                stage = 3
                Global.gui().message(other,
                        Global.format("Suddenly, {self:pronoun-action:hear|hears} a disembodied but familiar voice. \"Testing... testing... Good, looks like it worked.\"",  //TODO: Change these? Seems like Mara's. - DSM
                                affected, other))
                Global.gui().message(affected,
                        Global.format("{self:SUBJECT}... {self:action:seem|seems} to be hearing {other:name-possessive} voice inside {self:possessive} head. That's not good.",
                                affected, other))
                Global.gui().message(other,
                        Global.format("{other:NAME} gives {self:name-do} a satisfied smile and {other:possessive} disembodied voice echoes again inside {self:possessive} head, \"{self:NAME}, don't worry... I have connected myself with your brain... We will have so much fun together...\"",
                                affected, other))
            }
            when (Global.random(8)) {
                0 -> {
                    Global.gui().message(other,
                            Global.format("\"...You will cum for me...\"",
                                    affected, other))
                    Global.gui().message(affected,
                            Global.format("With absolutely no warning, {self:subject-action:feel|feels} an incredible orgasm rip through {self:possessive} body.",
                                    affected, other))
                    val part = Global.pickRandom(c.getStance().getPartsFor(c, affected, other)).orElse(affected.body.randomGenital)
                    val otherPart = Global.pickRandom(c.getStance().getPartsFor(c, other, other)).orElse(other.body.getRandom(
                            SkinPart.TYPE))
                    affected.doOrgasm(c, other, part, otherPart)
                }

                1 -> {
                    Global.gui().message(other,
                            Global.format("\"...Give yourself to me...\"",
                                    affected, other))
                    Global.gui().message(affected,
                            Global.format("With no input from {self:possessive} consciousness, {self:name-possessive} body mechanically walks up to {self:name-possessive} body and presses itself into {other:possessive} slime. While immobilized by {self:possessive} inability to send signals through {self:possessive} locomotive nerves, {self:name-possessive} body slowly sinks into {other:name-possessive} crystal blue body.",
                                    affected, other))
                    c.setStance(Engulfed(other, affected))
                    affected.add(c, Frenzied(affected, 2))
                }

                2, 3 -> {
                    Global.gui().message(other,
                            Global.format("\"...You will please me...\"",
                                    affected, other))
                    Global.gui().message(affected,
                            Global.format("{self:SUBJECT-ACTION:feel|feels} an immense need to service {self:NAME}!",
                                    affected, other))
                    c.getRandomWorshipSkill(affected, other) ?: (FootWorship(affected)).resolve(c, other)
                }

                4, 5 -> {
                    if (!c.getStance().dom(affected) && !c.getStance().prone(affected)) {
                        Global.gui().message(other,
                                Global.format("\"...You will kneel for me...\"",
                                        affected, other))
                        c.setStance(Kneeling(other, affected))
                    }
                    Global.gui().message(other,
                            Global.format("\"...You will pleasure yourself...\"",
                                    affected, other))
                    Global.gui().message(affected,
                            Global.format("{self:name-possessive} hands involunarily reach into {self:possessive} crotch and start masturbating!",
                                    affected, other))
                    Masturbate(affected).resolve(c, other)
                }

                6, 7 -> {
                    Global.gui().message(other,
                            Global.format("\"...You will pleasure yourself...\"",
                                    affected, other))
                    Global.gui().message(affected,
                            Global.format("{self:name-possessive} hands involunarily reach into {self:possessive} crotch and start masturbating!",
                                    affected, other))
                    Masturbate(affected).resolve(c, other)
                }

                else -> {
                    Global.gui().message(other,
                            Global.format("\"...You will pleasure yourself...\"",
                                    affected, other))
                    Global.gui().message(affected,
                            Global.format("{self:name-possessive} hands involunarily reach into {self:possessive} crotch and start masturbating!",
                                    affected, other))
                    Masturbate(affected).resolve(c, other)
                }
            }
        } else if (time >= 2) {
            if (stage < 2) {
                stage = 2
                c.write(affected,
                        Global.format("The parasite inside {self:subject} starts moving again. After a long journey, it has somehow reached inside {self:possessive} skull. Even though that part of {self:possessive} body should have no nerves, {self:pronoun-action:swear|swears} {self:pronoun} can feel its cold pseudopods integrating themselves with {self:possessive} brain.",
                                affected, other))
            }
            c.write(affected,
                    Global.format("{self:NAME-POSSESSIVE} thoughts slow down even further. It's becoming difficult to remember why {self:pronoun-action:are|is} even fighting in the first place.",
                            affected, other))
            affected.loseWillpower(c, 2)
        } else if (time >= 1) {
            if (stage < 1) {
                stage = 1
                c.write(affected,
                        Global.format("The slimey parasite inside {self:name-possessive} starts moving again. {self:PRONOUN} can feel it crawling through {self:possessive} head.",
                                affected, other))
            }
            c.write(affected,
                    Global.format("{self:NAME-POSSESSIVE} thoughts slow down. Somehow the parasite is sapping {self:possessive} will to fight.",
                            affected, other))
            affected.loseWillpower(c, 1)
        } else {
            c.write(affected, Global.format("A part of {other:name-possessive} slime is lodged inside {self:name-possessive} head. It doesn't feel too uncomfortable, but {self:pronoun-action:are|is} scared of the implications.",
                    affected, other))
            affected.emote(Emotion.desperate, 5)
            affected.emote(Emotion.nervous, 5)
        }
        time += .2
    }

    override fun damage(c: Combat, x: Int): Int {
        return 0
    }

    override fun pleasure(c: Combat, withPart: BodyPart, targetPart: BodyPart, x: Double): Double {
        return 0.0
    }

    override fun weakened(c: Combat?, x: Int): Int {
        return 0
    }

    override fun tempted(c: Combat, x: Int): Int {
        return 0
    }

    override fun evade(): Int {
        return -5
    }

    override fun escape(): Int {
        return 0
    }

    override fun gainmojo(x: Int): Int {
        return 0
    }

    override fun spendmojo(x: Int): Int {
        return 0
    }

    override fun counter(): Int {
        return 0
    }

    override fun toString(): String {
        return "Parasited"
    }

    override fun value(): Int {
        return 0
    }

    override fun instance(newAffected: Character, newOther: Character?): Status {
        return Parasited(newAffected, newOther!!)
    }

    override fun saveToJson(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("type", javaClass.simpleName)
        return obj
    }

    override fun loadFromJson(obj: JsonObject): Status? {
        return null // Parasited(null, null) // What would this even mean?
    }

    override fun regen(c: Combat?): Int {
        return 0
    }

    companion object {
        var DEBUFFABLE_ATTS = Arrays.asList(
                Attribute.Power,
                Attribute.Seduction,
                Attribute.Cunning
        )
    }
}