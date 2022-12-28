package nightgames.characters

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import nightgames.areas.Area
import nightgames.areas.DescriptionModule.ErrorDescriptionModule
import nightgames.beans.Property
import nightgames.characters.body.*
import nightgames.characters.body.mods.catcher.DemonicMod
import nightgames.characters.body.mods.pitcher.IncubusCockMod
import nightgames.characters.corestats.ArousalStat
import nightgames.characters.corestats.MojoStat
import nightgames.characters.corestats.StaminaStat
import nightgames.characters.corestats.WillpowerStat
import nightgames.combat.Assistant
import nightgames.combat.Combat
import nightgames.combat.Result
import nightgames.daytime.*
import nightgames.global.Configuration
import nightgames.global.Flag
import nightgames.global.Global
import nightgames.global.Scene
import nightgames.grammar.Person
import nightgames.items.Item
import nightgames.items.Loot
import nightgames.items.clothing.Clothing
import nightgames.items.clothing.ClothingSlot
import nightgames.items.clothing.ClothingTrait
import nightgames.items.clothing.Outfit
import nightgames.json.JsonUtils
import nightgames.match.*
import nightgames.match.Status
import nightgames.match.actions.UseBeer
import nightgames.match.actions.UseEnergyDrink
import nightgames.match.actions.UseLubricant
import nightgames.pet.arms.ArmManager
import nightgames.skills.*
import nightgames.skills.damage.DamageType
import nightgames.stance.Stance
import nightgames.status.*
import nightgames.status.addiction.Addiction
import nightgames.status.addiction.AddictionType
import nightgames.status.addiction.Dominance
import nightgames.status.addiction.MindControl
import nightgames.traits.*
import nightgames.trap.Trap
import nightgames.utilities.DebugHelper
import nightgames.utilities.ProseUtils
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max

@Suppress("unused")
abstract class Character(
        /**Returns the name of this character, presumably at the Character level.
         *
         * FIXME: presumably this.name, but it always helps to be explicit. This could be an accessor instead, which would be helpful and more conventional - DSM
         *
         * @return
         * returning the name at this Character level
         */
        var trueName: String,
        level: Int) : Observable(), Cloneable {
    @JvmField
    var initialGender: CharacterSex? = null
    @JvmField var progression: Progression
    fun getProgression() = progression
    @JvmField
    var money: Int
    @JvmField
    var att //Attributes are good opportunity to move to OOP Implementation - They are very similar to meters with base and modified values - DSM
            : MutableMap<Attribute, Int?>
    @JvmField
    var stamina: StaminaStat
    @JvmField
    var arousal: ArousalStat
    @JvmField var mojo: MojoStat
    open fun getMojo() = mojo
    @JvmField
    var willpower: WillpowerStat
    @JvmField
    val type: String = javaClass.simpleName
    open fun getType() = javaClass.simpleName
    @JvmField
    var outfit: Outfit
    @JvmField
    var outfitPlan //List is good but ArrayList is more powerful because it's serializable. - DSM
            : MutableList<Clothing>
    @JvmField
    var location //What does this do? Is it the characters Current Location? This should be stored as a String or implemented as a token on a larger GameMap - DSM
            : Property<Area>
    private var skills //Skills are unlikely objects to mutate tow warrant this - just opinion. - DSM
            : CopyOnWriteArrayList<Skill>
    @JvmField
    var status //List is not Serializable.  Marge into StatusEffect- DSM
            : MutableList<nightgames.status.Status>
    private var statusFlags //Can be merged into a StatusEffect object and made serializable. - DSM
            : MutableSet<Stsflag>
    private var traits //If traits are implemented like all the skills are, then this can just be an ArrayList. - DSM
            : CopyOnWriteArrayList<Trait>
    private var temporaryAddedTraits: MutableMap<Trait, Int>
    private var temporaryRemovedTraits: MutableMap<Trait, Int>
    @JvmField
    var removelist //Rename for clarity? - DSM
            : MutableSet<nightgames.status.Status>
    @JvmField
    var addlist //Rename for clarity?   -DSM
            : MutableSet<nightgames.status.Status>
    private val cooldowns //May not require this if we add new Skills to characters and they may track their own requirements and cooldowns. - DSM
            : MutableMap<String, Int>
    private var inventory: MutableMap<Item?, Int>
    private var flags //Needs to be more strongly leveraged in mechanics.  -DSM
            : MutableMap<String?, Int?>
    @JvmField
    var trophy: Item? = null
    @JvmField
    var attractions: MutableMap<String?, Int?>
    private var affections: MutableMap<String?, Int?>
    @JvmField
    var closet //If clothing can be destroyed, it should stand to reason that characters should purchase replace. Consider reworking - DSM
            : HashSet<Clothing>
    @JvmField
    var body //While current implementation allows for many kinds of parts - it means controlling and finding them gets difficult. - DSM
            : Body
    @JvmField
    var availableAttributePoints = 0
    var orgasmed //Merge into tracker object for combat session. -DSM
            : Boolean
    @JvmField internal var isCustomNPC //This is not necessary. Every character should be based off custom implementation and added as a configuration is chosen. -DSM
            : Boolean
    fun isCustomNPC() = isCustomNPC
    private var pleasured //Merge into tracker object for combat session. - DSM
            : Boolean
    @JvmField
    var orgasms = 0 //Merge into tracker object for combat session. - DSM
    @JvmField
    var cloned //Merge into tracker object for combat session. - DSM
            : Int
    private var levelPlan //This has bloated save files quite a bit, making an XML save file attributeModifier very desireable for editing and reading. - DSM
            : MutableMap<Int?, LevelUpData?>
    private var growth //FIXME: Growth, as well as a host of many variables in many classes, have many public variables. Move to protected or private and implement mutators. The compliler is your friend. - DSM
            : Growth

    // public CombatStats getCombatStats() {  return combatStats;  }  public void setCombatStats(CombatStats combatStats) {  this.combatStats = combatStats; }
    /**Overridden clone() method for Character. Returns a character with values the same as this one.
     *
     * @return
     * Returns a clone of this object.
     *
     * @throws CloneNotSupportedException
     * Is thrown when this object does not support the Cloneable interface.
     */
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Character {
        val c = super.clone() as Character
        c.att = HashMap(att)
        c.stamina = stamina.copy()
        c.cloned = cloned + 1
        c.arousal = arousal.copy()
        c.mojo = mojo.copy()
        c.willpower = willpower.copy()
        c.outfitPlan = java.util.ArrayList(outfitPlan)
        c.outfit = Outfit(outfit)
        c.flags = HashMap(flags)
        c.status = status // Will be deep-copied in finishClone()
        c.traits = CopyOnWriteArrayList(traits)
        c.temporaryAddedTraits = HashMap(temporaryAddedTraits)
        c.temporaryRemovedTraits = HashMap(temporaryRemovedTraits)

        // TODO! We should NEVER modify the growth in a combat sim. If this is not true, this needs to be revisited and deepcloned.
        c.growth = growth.clone() as Growth
        c.removelist = HashSet(removelist)
        c.addlist = HashSet(addlist)
        c.inventory = HashMap(inventory)
        c.attractions = HashMap(attractions)
        c.affections = HashMap(affections)
        c.skills = CopyOnWriteArrayList(getSkills())
        c.body = body.clone()
        c.body.character = c
        c.orgasmed = orgasmed
        c.statusFlags = EnumSet.copyOf(statusFlags)
        c.levelPlan = HashMap()
        for ((key, value) in levelPlan) {
            levelPlan[key] = value!!.clone() as LevelUpData
        }
        return c
    }

    /**This seems to be a helper method used to iterate over the statuses. It's called by the combat log and Class, as well as the informant.
     *
     * @param other
     *
     */
    fun finishClone(other: Character?) {
        val oldstatus: List<nightgames.status.Status?> = status
        status = mutableListOf()
        for (s in oldstatus) {
            status.add(s!!.instance(this, other))
        }
    }

    /**Gets the Resistances list for this character.
     *
     * NOTE: Need more insight into this method.
     *
     * @param c
     * The Combat class.
     * @return
     * Returns a list of resistances.
     */
    fun getResistances(c: Combat?): List<Resistance> {
        val resistances = traits.map { t: Trait? -> Trait.getResistance(t) }.toMutableList()
        if (c != null) {
            val petOrNull = c.assistantsOf(this).firstOrNull { pet -> pet.character.has(Trait.protective) }
            petOrNull?.let { petCharacter: Assistant ->
                resistances.add(Resistance { combat: Combat?, self: Character?, status: nightgames.status.Status ->
                    if (Global.random(100) < 50 && status.flags().contains(Stsflag.debuff) && status.flags().contains(Stsflag.purgable)) {
                        return@Resistance petCharacter.character.nameOrPossessivePronoun() + " Protection"
                    }
                    ""
                })
            }
        }
        return resistances
    }


    /**Nondescriptive getter for some value.
     *
     * FIXME: No, really, what is this and why is it needed? - DSM
     *
     * @param a
     * The Attribute whose value we wish to get.
     *
     * @return
     * Returns a value based on a total complied from a combinations of Traits, ClothingTraits, and Attributes.
     */
    operator fun get(a: Attribute): Int {
        if (a == Attribute.Slime && !has(Trait.slime)) {
            // always return 0 if there's no trait for it.
            return 0
        }
        var total = getPure(a)
        for (s in statuses) {
            total += s.mod(a)
        }
        total += body.mod(a, total)
        when (a) {
            Attribute.Arcane -> {
                if (outfit.has(ClothingTrait.mystic)) {
                    total += 2
                }
                if (has(Trait.kabbalah)) {
                    total += 10
                }
            }

            Attribute.Dark -> {
                if (outfit.has(ClothingTrait.broody)) {
                    total += 2
                }
                if (has(Trait.fallenAngel)) {
                    total += 10
                }
            }

            Attribute.Ki -> {
                if (outfit.has(ClothingTrait.martial)) {
                    total += 2
                }
                if (has(Trait.valkyrie)) {
                    total += 5
                }
            }

            Attribute.Fetish -> if (outfit.has(ClothingTrait.kinky)) {
                total += 2
            }

            Attribute.Cunning -> if (has(Trait.FeralAgility) && `is`(Stsflag.feral)) {
                // extra 5 strength at 10, extra 17 at 60.
                total += Math.pow(progression.level.toDouble(), .7).toInt()
            }

            Attribute.Power -> {
                if (has(Trait.testosterone) && hasDick()) {
                    total += Math.min(20, 10 + progression.level / 4)
                }
                if (has(Trait.FeralStrength) && `is`(Stsflag.feral)) {
                    // extra 5 strength at 10, extra 17 at 60.
                    total += Math.pow(progression.level.toDouble(), .7).toInt()
                }
                if (has(Trait.valkyrie)) {
                    total += 10
                }
            }

            Attribute.Science -> if (has(ClothingTrait.geeky)) {
                total += 2
            }

            Attribute.Hypnosis -> if (has(Trait.Illusionist)) {
                total += getPure(Attribute.Arcane) / 2
            }

            Attribute.Speed -> {
                if (has(ClothingTrait.bulky)) {
                    total -= 1
                }
                if (has(ClothingTrait.shoes)) {
                    total += 1
                }
                if (has(ClothingTrait.heels) && !has(Trait.proheels)) {
                    total -= 2
                }
                if (has(ClothingTrait.highheels) && !has(Trait.proheels)) {
                    total -= 1
                }
                if (has(ClothingTrait.higherheels) && !has(Trait.proheels)) {
                    total -= 1
                }
            }

            Attribute.Seduction -> if (has(Trait.repressed)) {
                total /= 2
            }

            else -> {}
        }
        return Math.max(0, total)
    }

    /**Determines if the Outfit has a given ClothingTrait attribute in the parameter.
     *
     * FIXME: This should be renamed and merged/refactored with a clothingset. This level of access may not be necessary. - DSM
     *
     * @param attribute
     * The ClothingTrait Attribute to be searched for.
     *
     * @return
     * Returns true if the outfit has the given attribute.
     */
    fun has(attribute: ClothingTrait?): Boolean {
        return outfit.has(attribute)
    }

    /**Returns the unmodified value of a given attribute.
     *
     * FIXME: This could be an accessor of an unmodified Attribute value, instead. - DSM
     *
     * @param a
     * The attribute to have its pure value calculated.
     * @return total
     *
     */
    fun getPure(a: Attribute?): Int {
        var total = 0
        if (att.containsKey(a) && a != Attribute.Willpower) {
            total = att[a]!!
        }
        return total
    }

    /**Checks the attribute against the difficulty class. Returns true if it passes.
     *
     * NOTE: This class seems to be more like a debugging class. It should be moved. -DSM
     * FIXME: This should not be in character - as it's very useful in many other places. - DSM
     *
     * @param a
     * The attribute to roll a check against
     *
     * @param dc
     * The Difficulty Class to roll the dice against.
     *
     * @return
     * Returns true if the roll beats the DC.
     */
    fun check(a: Attribute, dc: Int): Boolean {
        val rand = Global.random(20)
        if (rand == 0) {
            // critical hit
            return true
        }
        return if (rand == 19) {
            // critical miss
            false
        } else get(a) != 0 && get(a) + rand >= dc
    }

    /**Simple method for gaining the amount of exp given in i and updates the character accordingly.
     * Accounts for traits like fastlearner and Leveldrainer.
     *
     * @param i
     * The value of experience to increment by.
     */
    fun gainXP(i: Int) {
        var i = i
        assert(i >= 0)
        var rate = 1.0
        if (has(Trait.fastLearner)) {
            rate += .2
        }
        rate *= Global.xpRate
        i = Math.round(i * rate).toInt()
        progression.gainXP(i)
    }

    fun rankup() {
        progression.rank = progression.rank + 1
    }

    abstract fun ding(c: Combat?)

    /**Modifies a given base damage value by a given parameters.
     *
     * @param type
     * The damage type - used to obtain further information on defenses of both user and target.
     *
     * @param other
     * The target of the damage. Their defenses influence the multiplier.
     *
     * @param baseDamage
     * The base damage to be modified.
     *
     * @return
     * Returns a minium value of a double calculated from a moderation between the maximum and minimum damage.
     *
     */
    fun modifyDamage(type: DamageType, other: Character, baseDamage: Double): Double {
        // so for each damage type, one level from the attacker should result in about 3% increased damage, while a point in defense should reduce damage by around 1.5% per level.
        // this differential should be max capped to (2 * (100 + attacker's level * 1.5))%
        // this differential should be min capped to (.5 * (100 + attacker's level * 1.5))%
        val maxDamage = baseDamage * 2 * (1 + .015 * progression.level)
        val minDamage = baseDamage * .5 * (1 + .015 * progression.level)
        val multiplier = 1 + .03 * getOffensivePower(type) - .015 * other.getDefensivePower(type)
        val damage = baseDamage * multiplier
        return Math.min(Math.max(minDamage, damage), maxDamage)
    }

    /**Gets a defensive power value of this character bby a given DamageType. Each damage type in the game has a formula based on a value gotten from a character's Attribute.
     *
     * @param type
     * The Damage type to check.
     * @return
     * Returns a different value based upon the damage type.
     */
    private fun getDefensivePower(type: DamageType): Double {
        return when (type) {
            DamageType.arcane -> (get(Attribute.Arcane) + get(Attribute.Dark) / 2 + get(Attribute.Divinity) / 2 + get(Attribute.Ki) / 2).toDouble()
            DamageType.biological -> (get(Attribute.Animism) / 2 + get(Attribute.Bio) / 2 + get(Attribute.Medicine) / 2 + get(Attribute.Science) / 2 + get(Attribute.Cunning) / 2 + get(Attribute.Seduction) / 2).toDouble()
            DamageType.pleasure -> get(Attribute.Seduction).toDouble()
            DamageType.temptation -> (get(Attribute.Seduction) * 2 + get(Attribute.Submissive) * 2 + get(Attribute.Cunning)) / 2.0
            DamageType.technique -> get(Attribute.Cunning).toDouble()
            DamageType.physical -> (get(Attribute.Power) * 2 + get(Attribute.Cunning)) / 2.0
            DamageType.gadgets -> get(Attribute.Cunning).toDouble()
            DamageType.drain -> (get(Attribute.Dark) * 2 + get(Attribute.Arcane)) / 2.0
            DamageType.stance -> (get(Attribute.Cunning) * 2 + get(Attribute.Power)) / 2.0
            DamageType.weaken -> (get(Attribute.Dark) * 2 + get(Attribute.Divinity)) / 2.0
            DamageType.willpower -> (get(Attribute.Dark) + get(Attribute.Fetish) + get(Attribute.Divinity) * 2 + progression.level) / 2.0
        }
    }

    /**Gets an offensive power value of this character bby a given DamageType. Each damage type in the game has a formula based on a value gotten from a character's Attribute.
     *
     * @param type
     * The Damage type to check.
     * @return
     * Returns a different value based upon the damage type.
     */
    private fun getOffensivePower(type: DamageType): Double {
        return when (type) {
            DamageType.biological -> ((get(Attribute.Animism) + get(Attribute.Bio) + get(Attribute.Medicine) + get(Attribute.Science)) / 2).toDouble()
            DamageType.gadgets -> {
                var power = (get(Attribute.Science) * 2 + get(Attribute.Cunning)) / 3.0
                if (has(Trait.toymaster)) {
                    power += 20.0
                }
                power
            }

            DamageType.pleasure -> get(Attribute.Seduction).toDouble()
            DamageType.arcane -> get(Attribute.Arcane).toDouble()
            DamageType.temptation -> (get(Attribute.Seduction) * 2 + get(Attribute.Cunning)) / 3.0
            DamageType.technique -> get(Attribute.Cunning).toDouble()
            DamageType.physical -> (get(Attribute.Power) * 2 + get(Attribute.Cunning) + get(Attribute.Ki) * 2) / 3.0
            DamageType.drain -> (get(Attribute.Dark) * 2 + get(Attribute.Arcane)) / if (has(Trait.gluttony)) 1.5 else 2.0
            DamageType.stance -> (get(Attribute.Cunning) * 2 + get(Attribute.Power)) / 3.0
            DamageType.weaken -> (get(Attribute.Dark) * 2 + get(Attribute.Divinity) + get(Attribute.Ki)) / 3.0
            DamageType.willpower -> (get(Attribute.Dark) + get(Attribute.Fetish) + get(Attribute.Divinity) * 2 + progression.level) / 3.0
        }
    }
    /**Recursive? half-method for dealing with pain. Processes pain considering several traits, attributes and positions.
     * TODO: Someone explain this implementation.
     *
     * @param c
     * The combat to make use of this method.
     *
     * @param other
     * The opponent.
     *
     * @param i
     * The value of pain.
     *
     * @param primary
     *
     * @param physical
     * Indicates if hte pain is physical.
     *
     */
    @JvmOverloads
    fun pain(c: Combat?, other: Character?, i: Int, primary: Boolean = true, physical: Boolean = true) {
        var pain = i
        var bonus = 0
        if (`is`(Stsflag.rewired) && physical) {
            val message = String.format("%s pleasured for <font color='rgb(255,50,200)'>%d<font color='white'>\n",
                    Global.capitalizeFirstLetter(subjectWas()), pain)
            c?.writeSystemMessage(message, true)
            arouse(pain, c)
            return
        }
        if (has(Trait.slime)) {
            bonus += Slime.painModifier(pain)
            c?.write(this, Slime.textOnPain(grammar))
        }
        if (c != null) {
            if (has(Trait.cute) && other != null && other !== this && primary && physical) {
                bonus += Cute.painModifier(this, pain)
                c.write(this, Cute.textOnPain(grammar, other.grammar))
            }
            if (other != null && other !== this && other.has(Trait.dirtyfighter)
                    && (c.getStance().prone(other) || c.getStance().sub(other))
                    && physical) {
                bonus += DirtyFighter.painModifier()
                c.write(this, DirtyFighter.textOnPain(grammar, other.grammar))
            }
            if (has(Trait.sacrosanct) && physical && primary) {
                c.write(this, Global.format(
                        "{other:SUBJECT-ACTION:well|wells} up with guilt at hurting such a holy being. {self:PRONOUN-ACTION:become|becomes} temporarily untouchable in {other:possessive} eyes.",
                        this, other))
                add(c, Alluring(this, 1))
            }
            for (s in statuses) {
                bonus += s.damage(c, pain)
            }
        }
        pain += bonus
        pain = Math.max(1, pain)
        emote(Emotion.angry, pain / 3)

        // threshold at which pain calms you down
        val painAllowance = Math.max(10, stamina.max() / 6)
        var arousalLoss = pain - painAllowance
        if (other != null && other.has(Trait.wrassler)) {
            arousalLoss = Wrassler.inflictedPainArousalLossModifier(pain, painAllowance)
        }
        if (arousalLoss > 0 && !`is`(Stsflag.masochism)) {
            calm(c, arousalLoss)
        }
        // if the pain exceeds the threshold and you aren't a masochist
        // calm down by the overflow
        c?.writeSystemMessage(String.format("%s hurt for <font color='rgb(250,10,10)'>%d<font color='white'>",
                subjectWas(), pain), true)
        if (other != null && other.has(Trait.sadist) && !`is`(Stsflag.masochism)) {
            c!!.write("<br/>" + Global.capitalizeFirstLetter(String.format("%s blows hits all the right spots and %s to some masochistic tendencies.",
                    other.nameOrPossessivePronoun(), subjectAction("awaken"))))
            add(c, Masochistic(this))
        }
        // if you are a masochist, arouse by pain up to the threshold.
        if (`is`(Stsflag.masochism) && physical) {
            arouse(Math.max(i, painAllowance), c)
        }
        if (other != null && other.has(Trait.disablingblows) && Global.random(5) == 0) {
            val mag = Global.random(3) + 1
            c!!.write(other, Global.format("Something about the way {other:subject-action:hit|hits}"
                    + " {self:name-do} seems to strip away {self:possessive} strength.", this, other))
            add(c, Abuff(this, Attribute.Power, -mag, 10))
        }
        stamina.exhaust(pain)
    }

    /**Drains this character's stamina by value i.
     *
     * @param c
     * The combat that requires this method.
     *
     * @param drainer
     * the character that is performing the drain on this character.
     *
     * @param i
     * The base value to drain this character's stamina.
     */
    fun drain(c: Combat?, drainer: Character, i: Int) {
        var drained = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.drained(c, drained)
        }
        drained += bonus
        if (drained >= stamina.get()) {
            drained = stamina.get()
        }
        if (drained > 0) {
            c?.writeSystemMessage(String.format("%s drained of <font color='rgb(200,200,200)'>%d<font color='white'> stamina by %s",
                    subjectWas(), drained, drainer.subject()), true)
            stamina.exhaust(drained)
            drainer.stamina.recover(drained)
        }
    }

    /**Weaken's this character's Stamina by value i.
     *
     * @param c
     * The combat requiring this method.
     *
     * @param i
     * The base value, which is modified by bonuses.
     *
     */
    fun weaken(c: Combat?, i: Int) {
        var weak = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.weakened(c, i)
        }
        weak += bonus
        weak = Math.max(1, weak)
        if (weak >= stamina.get()) {
            weak = stamina.get()
        }
        if (weak > 0) {
            c?.writeSystemMessage(String.format("%s weakened by <font color='rgb(200,200,200)'>%d<font color='white'>",
                    subjectWas(), weak), true)
            stamina.exhaust(weak)
        }
    }

    @JvmOverloads
    fun heal(c: Combat?, i: Int, reason: String? = "") {
        var i = i
        i = Math.max(1, i)
        c?.writeSystemMessage(String.format("%s healed for <font color='rgb(100,240,30)'>%d<font color='white'>%s",
                subjectWas(), i, reason), true)
        stamina.recover(i)
    }

    fun subject(): String {
        return grammar.subject().defaultNoun()
    }

    fun pleasure(i: Int, c: Combat, source: Character?): Int {
        return resolvePleasure(i, c, source, Body.nonePart, Body.nonePart)
    }

    fun resolvePleasure(i: Int, c: Combat, source: Character?, selfPart: BodyPart?, opponentPart: BodyPart?): Int {
        var pleasure = i
        emote(Emotion.horny, i / 4 + 1)
        if (pleasure < 1) {
            pleasure = 1
        }
        pleasured = true
        // pleasure = 0;
        arousal.pleasure(pleasure)
        if (checkOrgasm()) {
            doOrgasm(c, source, selfPart, opponentPart)
        }
        return pleasure
    }

    fun temptNoSkillNoTempter(c: Combat, i: Int) {
        temptNoSkillNoSource(c, null, i)
    }

    fun temptNoSkillNoSource(c: Combat, tempter: Character?, i: Int) {
        tempt(c, tempter, null, i, null)
    }

    fun temptNoSource(c: Combat, tempter: Character?, i: Int, skill: Skill?) {
        tempt(c, tempter, null, i, skill)
    }

    fun temptNoSkill(c: Combat, tempter: Character?, with: BodyPart?, i: Int) {
        tempt(c, tempter, with, i, null)
    }

    fun temptWithSkill(c: Combat, tempter: Character?, with: BodyPart?, i: Int, skill: Skill?) {
        tempt(c, tempter, with, i, skill)
    }

    /**Tempts this character with a bodypart, accounting for various skills, the opponent, traits and statuses.
     *
     * FIXME: This is entirely too long, and would be a good opportunity for cleaning up. Several processes are at work, here, and Objectifying Skills would contribute to cleaning this up. - DSM
     *
     * @param c
     * The combar requiring this method.
     *
     * @param tempter
     * The character tempting this character.
     *
     * @param with
     * The bodypart they are tempting this character with.
     *
     * @param i
     * The base tempt value?
     *
     * @param skill
     * A Skill. May be null.
     *
     */
    fun tempt(c: Combat, tempter: Character?, with: BodyPart?, i: Int, skill: Skill?) {
        var extraMsg = ""
        var baseModifier = 1.0
        if (has(Trait.oblivious)) {
            extraMsg += " (Oblivious)"
            baseModifier *= .1
        }
        if (has(Trait.Unsatisfied) && (arousal.percent() >= 50 || willpower.percent() < 25)) {
            extraMsg += " (Unsatisfied)"
            baseModifier *= if (c != null && c.getOpponentCharacter(this).human()) {
                .2
            } else {
                .66
            }
        }
        var bonus = 0
        for (s in statuses) {
            bonus += s.tempted(c, i)
        }
        if (has(Trait.desensitized2)) {
            bonus -= i / 2
        }
        var bonusString = ""
        if (bonus > 0) {
            bonusString = String.format(" + <font color='rgb(240,60,220)'>%d<font color='white'>", bonus)
        } else if (bonus < 0) {
            bonusString = String.format(" - <font color='rgb(120,180,200)'>%d<font color='white'>", Math.abs(bonus))
        }
        if (tempter != null) {
            val dmg: Int
            val message: String
            var temptMultiplier = baseModifier
            var stalenessModifier = 1.0
            var stalenessString = ""
            if (skill != null) {
                stalenessModifier = c!!.getCombatantData(skill.self)!!.getMoveModifier(skill)
                if (abs(stalenessModifier - 1.0) >= .1) {
                    stalenessString = String.format(", staleness: %.1f", stalenessModifier)
                }
            }
            if (with != null) {
                // triple multiplier for the body part
                temptMultiplier *= tempter.body.getCharismaBonus(c, this) + with.getHotness(tempter, this) * 2
                dmg = Math.max(0, Math.round((i + bonus) * temptMultiplier * stalenessModifier)).toInt()
                message = if (Global.checkFlag(Flag.basicSystemMessages)) {
                    String.format("""
    %s tempted by %s %s for <font color='rgb(240,100,100, arg1)'>%d<font color='white'>
    
    """.trimIndent(),
                            Global.capitalizeFirstLetter(tempter.subject()),
                            tempter.nameOrPossessivePronoun(), with.describe(tempter), dmg)
                } else {
                    String.format("""
    %s tempted by %s %s for <font color='rgb(240,100,100)'>%d<font color='white'> (base:%d%s, charisma:%.1f%s)%s
    
    """.trimIndent(),
                            Global.capitalizeFirstLetter(subjectWas()), tempter.nameOrPossessivePronoun(),
                            with.describe(tempter), dmg, i, bonusString, temptMultiplier, stalenessString, extraMsg)
                }
            } else {
                temptMultiplier *= tempter.body.getCharismaBonus(c, this)
                if (c != null && tempter.has(Trait.obsequiousAppeal)
                        && c.getStance().sub(tempter)) {
                    temptMultiplier *= 2.0
                }
                dmg = Math.max(Math.round((i + bonus) * temptMultiplier * stalenessModifier).toInt(), 0)
                message = if (Global.checkFlag(Flag.basicSystemMessages)) {
                    String.format("""
    %s tempted %s for <font color='rgb(240,100,100, arg1)'>%d<font color='white'>
    
    """.trimIndent(),
                            Global.capitalizeFirstLetter(tempter.subject()),
                            if (tempter === this) reflexivePronoun() else nameDirectObject(), dmg)
                } else {
                    String.format("""
    %s tempted %s for <font color='rgb(240,100,100)'>%d<font color='white'> (base:%d%s, charisma:%.1f%s)%s
    
    """.trimIndent(),
                            Global.capitalizeFirstLetter(tempter.subject()),
                            if (tempter === this) reflexivePronoun() else nameDirectObject(),
                            dmg, i, bonusString, temptMultiplier, stalenessString, extraMsg)
                }
            }
            c?.writeSystemMessage(message, Global.checkFlag(Flag.basicSystemMessages))
            tempt(dmg)
            if (tempter.has(Trait.mandateOfHeaven)) {
                val arousalPercent = (dmg / arousal.max() * 100).toDouble()
                val data = c!!.getCombatantData(this)
                data!!.setDoubleFlag(Combat.TEMPT_WORSHIP_BONUS, data.getDoubleFlag(Combat.TEMPT_WORSHIP_BONUS) + arousalPercent)
                val newWorshipBonus = data.getDoubleFlag(Combat.TEMPT_WORSHIP_BONUS)
                if (newWorshipBonus < 10) {
                    // nothing yet?
                } else if (newWorshipBonus < 25) {
                    c.write(tempter, Global.format("There's a nagging urge for {self:name-do} to throw {self:reflective} at {other:name-possessive} feet and beg for release.", this, tempter))
                } else if (newWorshipBonus < 50) {
                    c.write(tempter, Global.format("{self:SUBJECT-ACTION:feel|feels} an urge to throw {self:reflective} at {other:name-possessive} feet and beg for release.", this, tempter))
                } else {
                    c.write(tempter, Global.format("{self:SUBJECT-ACTION:are|is} feeling an irresistable urge to throw {self:reflective} at {other:name-possessive} feet and beg for release.", this, tempter))
                }
            }
        } else {
            val damage = Math.max(0, Math.round((i + bonus) * baseModifier).toInt())
            c?.writeSystemMessage(String.format("%s tempted for <font color='rgb(240,100,100)'>%d<font color='white'>%s\n",
                    subjectWas(), damage, extraMsg), false)
            tempt(damage)
        }
    }

    /**Half-recursive method for arousing this character. Performs the heavy lifting of arounse (int, combat)
     * @param i
     * The base value of arousal.
     * @param c
     * The combat required for this function.
     * @param source
     * The source of the arousal damage.
     */
    @JvmOverloads
    fun arouse(i: Int, c: Combat?, source: String? = "") {
        var i = i
        var extraMsg = ""
        if (has(Trait.Unsatisfied) && (arousal.percent() >= 50 || willpower.percent() < 25)) {
            extraMsg += " (Unsatisfied)"
            // make it much less effective vs NPCs because they're bad at exploiting the weakness
            i = if (c != null && c.getOpponentCharacter(this).human()) {
                Math.max(1, i / 5)
            } else {
                Math.max(1, i * 2 / 3)
            }
        }
        val message = String.format("%s aroused for <font color='rgb(240,100,100)'>%d<font color='white'> %s%s\n",
                Global.capitalizeFirstLetter(subjectWas()), i, source, extraMsg)
        c?.writeSystemMessage(message, true)
        tempt(i)
    }

    open fun subjectAction(verb: String?, pluralverb: String): String {
        return subject() + " " + pluralverb
    }

    fun subjectAction(verb: String?): String {
        return subjectAction(verb, ProseUtils.getThirdPersonFromFirstPerson(verb))
    }

    protected open fun subjectWas(): String? {
        return subject() + " was"
    }

    /**Tempts this character. Simple method valled by many other classes to do a simple amount of arousal to this character.
     *
     * @param i
     * The base value.
     */
    fun tempt(i: Int) {
        val bonus = 0
        emote(Emotion.horny, i / 4)
        arousal.pleasure(i)
    }

    /**Calms this character.
     * @param c
     * The combat that this method requires.
     * @param i
     * The base base value.
     *
     */
    fun calm(c: Combat?, i: Int) {
        if (i > 0) {
            if (c != null) {
                val message = String.format("%s calmed down by <font color='rgb(80,145,200)'>%d<font color='white'>\n",
                        Global.capitalizeFirstLetter(subjectAction("have", "has")), i)
                c.writeSystemMessage(message, true)
            }
            arousal.calm(i)
        }
    }

    /**Builds this character's mojo based upon percentage and source.
     *
     * @param percent
     * The base percentage of mojo to gain.
     *
     * @param source
     * The source of Mojo gain.
     *
     */
    @JvmOverloads
    fun buildMojo(c: Combat?, percent: Int, source: String? = "") {
        if (Dominance.mojoIsBlocked(this, c)) {
            c!!.write(c.getOpponentCharacter(this), String.format("Enraptured by %s display of dominance, %s no mojo.",
                    c.getOpponentCharacter(this).nameOrPossessivePronoun(), subjectAction("build")))
            return
        }
        var x = percent * Math.min(mojo.max(), 200) / 100
        var bonus = 0
        for (s in statuses) {
            bonus += s.gainmojo(x)
        }
        x += bonus
        if (x > 0) {
            mojo.build(x)
            c?.writeSystemMessage(Global.capitalizeFirstLetter(String.format("%s <font color='rgb(100,200,255)'>%d<font color='white'> mojo%s.",
                    subjectAction("built", "built"), x, source)), true)
        } else if (x < 0) {
            loseMojo(c, x)
        }
    }

    @JvmOverloads
    fun spendMojo(c: Combat?, i: Int, source: String? = "") {
        var cost = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.spendmojo(i)
        }
        cost += bonus
        mojo.deplete(cost)
        if (c != null && i != 0) {
            c.writeSystemMessage(Global.capitalizeFirstLetter(String.format("%s <font color='rgb(150,150,250)'>%d<font color='white'> mojo%s.",
                    subjectAction("spent", "spent"), cost, source)), true)
        }
    }

    @JvmOverloads
    fun loseMojo(c: Combat?, i: Int, source: String? = ""): Int {
        mojo.deplete(i)
        c?.writeSystemMessage(Global.capitalizeFirstLetter(String.format("%s <font color='rgb(150,150,250)'>%d<font color='white'> mojo%s.",
                subjectAction("lost", "lost"), i, source)), true)
        return i
    }

    fun location(): Area? {
        return location.get()
    }

    fun init(): Int {
        return att[Attribute.Speed]!! + Global.random(10)
    }

    fun reallyNude(): Boolean {
        return topless() && pantsless()
    }

    fun torsoNude(): Boolean {
        return topless() && pantsless()
    }

    fun mostlyNude(): Boolean {
        return breastsAvailable() && crotchAvailable()
    }

    fun breastsAvailable(): Boolean {
        return outfit.slotOpen(ClothingSlot.top)
    }

    fun crotchAvailable(): Boolean {
        return outfit.slotOpen(ClothingSlot.bottom)
    }

    fun dress(c: Combat) {
        outfit.dress(c.getCombatantData(this)!!.clothespile)
    }

    fun change() {
        outfit.undress()
        outfit.dress(outfitPlan)
        if (Global.getMatch() != null) {
            Global.getMatch().condition.handleOutfit(this)
        }
    }

    fun getName(): String {
        val disguised = getStatus(Stsflag.disguised) as Disguised?
        return if (disguised != null) {
            disguised.target.trueName
        } else trueName
    }

    fun completelyNudify(c: Combat?) {
        val articles = outfit.undress()
        if (c != null) {
            articles.forEach { article: Clothing? -> c.getCombatantData(this)!!.addToClothesPile(this, article) }
        }
    }

    /* undress without any modifiers */
    fun undress(c: Combat) {
        if (!breastsAvailable() || !crotchAvailable()) {
            // first time only strips down to what blocks fucking
            outfit.strip().forEach { article: Clothing? -> c.getCombatantData(this)!!.addToClothesPile(this, article) }
        } else {
            // second time strips down everything
            outfit.undress().forEach { article: Clothing? -> c.getCombatantData(this)!!.addToClothesPile(this, article) }
        }
    }

    /* undress non indestructibles */
    fun nudify(): Boolean {
        if (!breastsAvailable() || !crotchAvailable()) {
            // first time only strips down to what blocks fucking
            outfit.forcedstrip()
        } else {
            // second time strips down everything
            outfit.undressOnly { c: Clothing -> !c.`is`(ClothingTrait.indestructible) }
        }
        return mostlyNude()
    }

    fun strip(article: Clothing?, c: Combat): Clothing? {
        if (article == null) {
            return null
        }
        val res = outfit.unequip(article)
        c.getCombatantData(this)!!.addToClothesPile(this, res)
        return res
    }

    fun strip(slot: ClothingSlot?, c: Combat): Clothing? {
        return strip(outfit.getTopOfSlot(slot), c)
    }

    fun gainTrophy(c: Combat, target: Character) {
        val underwear = target.outfitPlan.firstOrNull { article: Clothing -> article.slots.contains(ClothingSlot.bottom) && article.layer == 0 }
        if (underwear == null || c.getCombatantData(target)!!.clothespile.contains(underwear)) {
            this.gain(target.trophy)
        }
    }

    fun shredRandom(): Clothing? {
        val slot = outfit.randomShreddableSlot
        return slot?.let { shred(it) }
    }

    fun topless(): Boolean {
        return outfit.slotEmpty(ClothingSlot.top)
    }

    fun pantsless(): Boolean {
        return outfit.slotEmpty(ClothingSlot.bottom)
    }

    @JvmOverloads
    fun stripRandom(c: Combat, force: Boolean = false): Clothing? {
        return strip(if (force) outfit.randomEquippedSlot else outfit.randomNakedSlot, c)
    }

    val randomStrippable: Clothing?
        get() {
            val slot = outfit.randomEquippedSlot
            return if (slot == null) null else outfit.getTopOfSlot(slot)
        }

    fun shred(slot: ClothingSlot): Clothing? {
        val article = outfit.getTopOfSlot(slot)
        return if (article == null || article.`is`(ClothingTrait.indestructible)) {
            System.err.println("Tried to shred clothing that doesn't exist at slot " + slot.name + " at clone "
                    + cloned)
            System.err.println(outfit.toString())
            Thread.dumpStack()
            null
        } else {
            // don't add it to the pile
            outfit.unequip(article)
        }
    }

    private fun countdown(counters: MutableMap<Trait, Int>) {
        val it = counters.entries.iterator()
        while (it.hasNext()) {
            val ent = it.next()
            val remaining = ent.value - 1
            if (remaining > 0) {
                ent.setValue(remaining)
            } else {
                it.remove()
            }
        }
    }

    fun tick(c: Combat?) {
        body.tick(c)
        countdown(temporaryAddedTraits)
        countdown(temporaryRemovedTraits)
    }

    fun getTraits(): Collection<Trait> {
        val allTraits: MutableCollection<Trait> = HashSet()
        allTraits.addAll(traits)
        allTraits.addAll(temporaryAddedTraits.keys)
        allTraits.removeAll(temporaryRemovedTraits.keys)
        return allTraits
    }

    fun clearTraits() {
        val traitsToRemove: List<Trait?> = java.util.ArrayList(traits)
        traitsToRemove.forEach { t: Trait? -> removeTraitDontSaveData(t) }
    }

    val traitsPure: Collection<Trait?>
        get() = Collections.unmodifiableCollection(traits)

    fun addTemporaryTrait(t: Trait?, duration: Int): Boolean {
        if (t == null) return false
        if (!getTraits().contains(t)) {
            temporaryAddedTraits[t] = duration
            return true
        } else if (temporaryAddedTraits.containsKey(t)) {
            temporaryAddedTraits[t] = max(duration, temporaryAddedTraits[t]!!)
            return true
        }
        return false
    }

    fun removeTemporarilyAddedTrait(t: Trait?): Boolean {
        if (temporaryAddedTraits.containsKey(t)) {
            temporaryAddedTraits.remove(t)
            return true
        }
        return false
    }

    fun removeTemporaryTrait(t: Trait, duration: Int): Boolean {
        if (temporaryRemovedTraits.containsKey(t)) {
            temporaryRemovedTraits[t] = Math.max(duration, temporaryRemovedTraits[t]!!)
            return true
        } else if (traits.contains(t)) {
            temporaryRemovedTraits[t] = duration
            return true
        }
        return false
    }

    fun getLevelUpFor(level: Int): LevelUpData? {
        levelPlan.putIfAbsent(level, LevelUpData())
        return levelPlan[level]
    }

    @JvmOverloads
    fun modAttributeDontSaveData(a: Attribute, i: Int, silent: Boolean = false) {
        if (human() && i != 0 && !silent && cloned == 0) {
            Global.writeIfCombat(Global.gui().combat, this,
                    "You have " + (if (i > 0) "gained" else "lost") + " " + Math.abs(i) + " " + a!!.name)
            Global.updateIfCombat(Global.gui().combat)
        }
        if (a == Attribute.Willpower) {
            willpower.gain((i * 2).toFloat())
        } else {
            att[a] = att.getOrDefault(a, 0)!! + i
        }
    }

    @JvmOverloads
    fun mod(a: Attribute, i: Int, silent: Boolean = false) {
        modAttributeDontSaveData(a, i, silent)
        getLevelUpFor(progression.level)!!.modAttribute(a, i)
    }

    fun addTraitDontSaveData(t: Trait?): Boolean {
        if (t == null) {
            System.err.println("Tried to add an null trait!")
            DebugHelper.printStackFrame(5, 1)
            return false
        }
        if (traits.addIfAbsent(t)) {
            if (t == Trait.mojoMaster) {
                mojo.gain(20f)
            }
            return true
        }
        return false
    }

    open fun add(t: Trait?): Boolean {
        if (addTraitDontSaveData(t)) {
            getLevelUpFor(progression.level)!!.addTrait(t)
            return true
        }
        return false
    }

    fun removeTraitDontSaveData(t: Trait?): Boolean {
        if (traits.remove(t)) {
            if (t == Trait.mojoMaster) {
                mojo.gain(-20f)
            }
            return true
        }
        return false
    }

    fun remove(t: Trait?): Boolean {
        if (removeTraitDontSaveData(t)) {
            getLevelUpFor(progression.level)!!.removeTrait(t)
            return true
        }
        return false
    }

    fun hasPure(t: Trait?): Boolean {
        return getTraits().contains(t)
    }

    fun has(t: Trait?): Boolean {
        if (t == null) {
            throw RuntimeException()
        }
        var hasTrait = false
        if (t.parent != null) {
            hasTrait = getTraits().contains(t.parent)
        }
        if (outfit.has(t)) {
            return true
        }
        hasTrait = hasTrait || hasPure(t)
        return hasTrait
    }

    fun hasDick(): Boolean {
        return body.randomCock != null
    }

    fun hasBalls(): Boolean {
        return body.randomBalls != null
    }

    fun hasPussy(): Boolean {
        return body.randomPussy != null
    }

    fun hasBreasts(): Boolean {
        return body.randomBreasts != null
    }

    fun countFeats(): Int {
        var count = 0
        for (t in traits) {
            if (t!!.isFeat) {
                count++
            }
        }
        return count
    }

    fun regen(c: Combat?) {
        regen(c, true)
    }

    fun regen(c: Combat? = null, combat: Boolean = false) {
        addictions.forEach { obj: Addiction? -> obj!!.refreshWithdrawal() }
        var regen = 1
        // TODO can't find the concurrent modification error, just use a copy
        // for now I guess...
        for (s in HashSet(statuses)) {
            regen += s!!.regen(c)
        }
        if (has(Trait.BoundlessEnergy)) {
            regen += 1
        }
        if (regen > 0) {
            heal(c, regen)
        } else {
            weaken(c, -regen)
        }
        if (combat) {
            if (has(Trait.exhibitionist) && mostlyNude()) {
                buildMojo(c, 5)
            }
            if (outfit.has(ClothingTrait.stylish)) {
                buildMojo(c, 1)
            }
            if (has(Trait.SexualGroove)) {
                buildMojo(c, 3)
            }
            if (outfit.has(ClothingTrait.lame)) {
                buildMojo(c, -1)
            }
        }
    }

    fun preturnUpkeep() {
        orgasmed = false
    }

    fun addNonCombat(status: Status) {
        add(null, status.inflictedStatus)
    }

    fun has(status: nightgames.status.Status): Boolean {
        return this.status.any { s: nightgames.status.Status? ->
            s!!.flags().containsAll(status.flags()) && status.flags().containsAll(status.flags()) &&
                    s.javaClass == status.javaClass && s.variant == status.variant
        }
    }

    open fun add(c: Combat?, status: nightgames.status.Status) {
        var cynical = false
        var message: String? = ""
        var done = false
        var effectiveStatus: nightgames.status.Status? = status
        for (s in statuses) {
            if (s.flags().contains(Stsflag.cynical)) {
                cynical = true
            }
        }
        if (cynical && status.mindgames()) {
            message = subjectAction("resist", "resists") + " " + status.name + " (Cynical)."
            done = true
        } else {
            for (r in getResistances(c)) {
                var resistReason = ""
                resistReason = r.resisted(c, this, status)
                if (resistReason.isNotEmpty()) {
                    message = subjectAction("resist", "resists") + " " + status.name + " (" + resistReason + ")."
                    done = true
                    break
                }
            }
        }
        if (!done) {
            var unique = true
            for (s in this.status) {
                if (s.javaClass == status.javaClass && s.variant == status.variant) {
                    s.replace(status)
                    message = s.initialMessage(c, status)
                    done = true
                    effectiveStatus = s
                    break
                }
                if (s.overrides(status)) {
                    unique = false
                }
            }
            if (!done && unique) {
                this.status.add(status)
                message = status.initialMessage(c, null)
                done = true
            }
        }
        if (done) {
            if (message != null) {
                message = Global.capitalizeFirstLetter(message)
                if (c != null) {
                    if (!c.getOpponentCharacter(this).human() || !c.getOpponentCharacter(this).`is`(Stsflag.blinded)) {
                        c.write(this, "<b>$message</b>")
                    }
                    effectiveStatus!!.onApply(c, c.getOpponentCharacter(this))
                } else if (human() || location() != null && location()!!.humanPresent()) {
                    Global.gui().message("<b>$message</b>")
                    effectiveStatus!!.onApply(null, null)
                }
            }
        }
    }

    private fun getPheromonesChance(c: Combat): Double {
        val baseChance = .1 + exposure / 3 + (arousal.overflow + arousal.get()) / arousal.max().toFloat()
        var mod = c.getStance().pheromoneMod(this)
        if (has(Trait.FastDiffusion)) {
            mod = Math.max(2.0, mod)
        }
        return Math.min(1.0, baseChance * mod)
    }

    fun rollPheromones(c: Combat): Boolean {
        val chance = getPheromonesChance(c)
        val roll = Global.randomdouble()
        // System.out.println("Pheromones: rolled " + Global.formatDecimal(roll)
        // + " vs " + chance + ".");
        return roll < chance
    }

    val pheromonePower: Int
        get() =//return (int) (2 + Math.sqrt(get(Attribute.Animism) + get(Attribute.Bio)) / 2);
            5

    fun dropStatus(c: Combat?, opponent: Character?) {
        val removedStatuses = status.filter { s: nightgames.status.Status? -> !s!!.meetsRequirements(c, this, opponent) }.toMutableSet()
        removedStatuses.addAll(removelist)
        removedStatuses.forEach { s: nightgames.status.Status? -> s!!.onRemove(c, opponent) }
        status.removeAll(removedStatuses)
        for (s in addlist) {
            add(c, s)
        }
        removelist.clear()
        addlist.clear()
    }

    fun removeStatusNoSideEffects() {
        status.removeAll(removelist)
        removelist.clear()
    }

    fun `is`(sts: Stsflag): Boolean {
        if (statusFlags.contains(sts)) return true
        for (s in statuses) {
            if (s.flags().contains(sts)) {
                return true
            }
        }
        return false
    }

    fun `is`(sts: Stsflag?, variant: String): Boolean {
        for (s in statuses) {
            if (s.flags().contains(sts) && s.variant == variant) {
                return true
            }
        }
        return false
    }

    fun stunned(): Boolean {
        for (s in statuses) {
            if (s.flags().contains(Stsflag.stunned) || s.flags().contains(Stsflag.falling)) {
                return true
            }
        }
        return false
    }

    fun distracted(): Boolean {
        for (s in statuses) {
            if (s.flags().contains(Stsflag.distracted) || s.flags().contains(Stsflag.trance)) {
                return true
            }
        }
        return false
    }

    fun hasStatus(flag: Stsflag?): Boolean {
        for (s in statuses) {
            if (s.flags().contains(flag)) {
                return true
            }
        }
        return false
    }

    fun removeStatus(status: nightgames.status.Status) {
        removelist.add(status)
    }

    fun removeStatus(flag: Stsflag?) {
        for (s in statuses) {
            if (s.flags().contains(flag)) {
                removelist.add(s)
            }
        }
    }

    fun bound(): Boolean {
        return `is`(Stsflag.bound)
    }

    fun free() {
        for (s in statuses) {
            if (s.flags().contains(Stsflag.bound)) {
                removelist.add(s)
            }
        }
    }

    fun struggle() {
        for (s in statuses) {
            s.struggle(this)
        }
    }

    /**Returns the chance of escape for this character based upon a total of bonuses from traits and stances. */
    fun getEscape(c: Combat?, from: Character?): Int {
        var total = 0
        for (s in statuses) {
            total += s.escape()
        }
        if (has(Trait.freeSpirit)) {
            total += 5
        }
        if (has(Trait.Slippery)) {
            total += 10
        }
        if (from != null) {
            if (from.has(Trait.Clingy)) {
                total -= 5
            }
            if (from.has(Trait.FeralStrength) && from.`is`(Stsflag.feral)) {
                total -= 5
            }
        }
        if (c != null && checkAddiction(AddictionType.DOMINANCE, c.getOpponentCharacter(this))) {
            total -= getAddiction(AddictionType.DOMINANCE)!!.combatSeverity.ordinal * 8
        }
        if (has(Trait.FeralStrength) && `is`(Stsflag.feral)) {
            total += 5
        }
        if (c != null) {
            val stanceMod = c.getStance().getEscapeMod(c, this)
            if (stanceMod < 0) {
                total += if (bound()) {
                    stanceMod / 2
                } else {
                    stanceMod
                }
            }
        }
        return total
    }

    fun canMasturbate(): Boolean {
        return !(stunned() || bound() || `is`(Stsflag.distracted) || `is`(Stsflag.enthralled))
    }

    fun canAct(): Boolean {
        return !(stunned() || distracted() || bound() || `is`(Stsflag.enthralled))
    }

    fun canRespond(): Boolean {
        return !(stunned() || distracted() || `is`(Stsflag.enthralled))
    }

    abstract fun describe(per: Int, observer: Character): String?
    abstract fun resist3p(c: Combat?, target: Character?, assist: Character?): Boolean
    open fun displayStateMessage(knownTrap: Trap.Instance?) {}

    /**abstract method for determining if this character is human - meaning the player.
     * TODO: Reccomend renaming to isHuman(), to make more meaningful name and easier to find. */
    abstract fun human(): Boolean
    abstract fun bbLiner(c: Combat?, target: Character?): String?
    abstract fun nakedLiner(c: Combat?, target: Character?): String?
    abstract fun stunLiner(c: Combat?, target: Character?): String?
    abstract fun taunt(c: Combat?, target: Character?): String?

    /**Determines if this character is controlled by a human.
     *
     * NOTE: Since there are currently no mechanisms available to control another character, a simple boolean for isPLayer might suffice.
     *
     * @param c
     * The combat required for this method.
     */
    fun humanControlled(c: Combat?): Boolean {
        return human()
    }

    /**Saves this character using a JsonObject.
     *
     * This currently creates a large amount of sprawl in the save file, but moving towards object-oriented packages of members or XML may help. - DSM
     *
     */
    fun save(): JsonObject {
        val saveObj = JsonObject()
        saveObj.addProperty("name", trueName)
        saveObj.addProperty("type", type)
        saveObj.add(JSON_PROGRESSION, progression.save())
        saveObj.addProperty("money", money)
        run {
            val jsCoreStats = JsonObject()
            jsCoreStats.add(jsStamina, stamina.save())
            jsCoreStats.add(jsArousal, arousal.save())
            jsCoreStats.add(jsMojo, mojo.save())
            jsCoreStats.add(jsWillpower, willpower.save())
            saveObj.add(Companion.jsCoreStats, jsCoreStats)
        }
        saveObj.add("affections", JsonUtils.JsonFromMap(affections))
        saveObj.add("attractions", JsonUtils.JsonFromMap(attractions))
        saveObj.add("attributes", JsonUtils.JsonFromMap(att))
        saveObj.add("outfit", JsonUtils.jsonFromCollection(outfitPlan))
        saveObj.add("closet", JsonUtils.jsonFromCollection(closet))
        saveObj.add("traits", JsonUtils.jsonFromCollection(traits)) //FIXME: May be contributing to levelup showing duplicate Trait entries making levelling and deleveling problematic. - DSM
        saveObj.add("body", body.save())
        saveObj.add("inventory", JsonUtils.JsonFromMap(inventory))
        saveObj.addProperty("human", human())
        saveObj.add("flags", JsonUtils.JsonFromMap(flags))
        saveObj.add("levelUps", JsonUtils.JsonFromMap(levelPlan)) //FIXME: May be contributing to levelup showing duplicate Trait entries making levelling and deleveling problematic. - DSM
        saveObj.add("growth", JsonUtils.gson!!.toJsonTree(growth))
        // TODO eventually this should load any status, for now just load addictions
        val statusJson = JsonArray()
        addictions.map { addiction -> addiction.saveToJson() }.forEach { addictionJson -> statusJson.add(addictionJson) }
        saveObj.add("status", statusJson)
        saveInternal(saveObj)
        return saveObj
    }

    protected fun saveInternal(obj: JsonObject?) {}

    /**Loads this character from a Json Object that was output to file.
     *
     */
    fun load(`object`: JsonObject) {
        trueName = `object`["name"].asString
        progression = Progression(`object`[JSON_PROGRESSION].asJsonObject)
        if (`object`.has("growth")) {
            growth = JsonUtils.gson!!.fromJson(`object`["growth"], Growth::class.java)
            growth.removeNullTraits()
        }
        money = `object`["money"].asInt
        run {
            val jsCoreStats = `object`.getAsJsonObject(jsCoreStats)
            stamina = StaminaStat(jsCoreStats[jsStamina].asJsonObject)
            arousal = ArousalStat(jsCoreStats[jsArousal].asJsonObject)
            mojo = MojoStat(jsCoreStats[jsMojo].asJsonObject)
            willpower = WillpowerStat(jsCoreStats[jsWillpower].asJsonObject)
        }
        affections = JsonUtils.fromJson(`object`.getAsJsonObject("affections"))
        attractions = JsonUtils.fromJson(`object`.getAsJsonObject("attractions"))
        run {
            outfitPlan.clear()
            JsonUtils.getOptionalArray(`object`, "outfit").ifPresent { array: JsonArray -> this.addClothes(array) }
        }
        run {
            closet = JsonUtils.fromJson<HashSet<Clothing>>(`object`.getAsJsonArray("closet"))
        }
        run {
            traits = CopyOnWriteArrayList(
                    JsonUtils.fromJson<List<Trait>>(`object`.getAsJsonArray("traits"))
                            .filterNotNull())
            if (this.type == "Airi") traits.remove(Trait.slime)
        }
        body = Body.load(`object`.getAsJsonObject("body"), this)
        att = JsonUtils.fromJson(`object`.getAsJsonObject("attributes"))
        inventory = JsonUtils.fromJson(`object`.getAsJsonObject("inventory"))
        flags.clear()
        JsonUtils.getOptionalObject(`object`, "flags")
                .ifPresent { obj: JsonObject -> flags.putAll(JsonUtils.fromJson(obj)) }
        levelPlan = if (`object`.has("levelUps")) {
            JsonUtils.fromJson(`object`.getAsJsonObject("levelUps"))
        } else {
            HashMap()
        }
        status = mutableListOf()
        for (element in `object`.getAsJsonArray("status") ?: JsonArray()) {
            try {
                val addiction = Addiction.load(this, element.asJsonObject)
                if (addiction != null) {
                    status.add(addiction)
                }
            } catch (e: Exception) {
                System.err.println("Failed to load status:")
                System.err.println(JsonUtils.gson!!.toJson(element))
                e.printStackTrace()
            }
        }
        change()
        Global.gainSkills(this)
        Global.learnSkills(this)
    }

    private fun addClothes(array: JsonArray) {
        JsonUtils.stringsFromJson(array)?.mapNotNull { key: String? -> Clothing.getByID(key) }?.let { outfitPlan.addAll(it) }
    }

    abstract fun afterParty()
    fun checkOrgasm(): Boolean {
        return arousal.isAtUnfavorableExtreme && !`is`(Stsflag.orgasmseal) && pleasured
    }

    /**Makes the character orgasm. Currently accounts for various traits involved with orgasms.
     *
     * @param c
     * The combat that this method requires.
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param opponentPart
     * The part in the opponent that is making this character orgasm.
     *
     */
    fun doOrgasm(c: Combat, opponent: Character?, selfPart: BodyPart?, opponentPart: BodyPart?) {
        var total = 1
        if (this !== opponent && opponent != null) {
            if (opponent.has(Trait.carnalvirtuoso)) {
                total++
            }
            if (opponent.has(Trait.intensesuction)
                    && (outfit.has(ClothingTrait.harpoonDildo) || outfit.has(ClothingTrait.harpoonOnahole)) && Global.random(3) == 0) {
                total++
            }
        }
        for (i in 1..total) {
            resolveOrgasm(c, opponent, selfPart, opponentPart, i, total)
        }
    }

    /**Resolves the orgasm. Accounts for various traits and outputs dynamic text to the GUI.
     *
     * @param c
     * The combat that this method requires.
     *
     * @param opponent
     * The opponent that is making this orgasm happen.
     *
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     *
     * @param opponentPart
     * The part in the opponent that is making this character orgasm.
     *
     * @param times
     * The number of times this person is orgasming.
     *
     * @param totalTimes
     * The total amount of times that the character has orgasmed.
     *
     */
    protected open fun resolveOrgasm(c: Combat, opponent: Character?, selfPart: BodyPart?, opponentPart: BodyPart?, times: Int, totalTimes: Int) {
        if (has(Trait.HiveMind)) {
            if (HiveMind.resolveOrgasm(c, this, opponent)) {
                return
            }
        }
        val orgasmLiner = "<b>" + orgasmLiner(c,
                opponent ?: c.getOpponentCharacter(this)) + "</b>"
        val opponentOrgasmLiner = if (opponent == null || opponent === this || opponent.isPet()) "" else "<b>" + opponent.makeOrgasmLiner(c, this) + "</b>"
        orgasmed = true
        if (times == 1) {
            c.write(this, "<br/>")
        }
        if (opponent === this) {
            resolvePreOrgasmForSolo(c, opponent, selfPart, times)
        } else {
            resolvePreOrgasmForOpponent(c, opponent, selfPart, opponentPart, times, totalTimes)
        }
        val overflow = arousal.overflow
        c.write(this, String.format("<font color='rgb(255,50,200)'>%s<font color='white'> arousal overflow",
                overflow))
        if (this !== opponent) {
            resolvePostOrgasmForOpponent(c, opponent, selfPart, opponentPart)
        }
        orgasm()
        if (`is`(Stsflag.feral)) {
            arousal.pleasure(arousal.max() / 2)
        }
        val extra = 25.0f * overflow / arousal.max()
        val willloss = orgasmWillpowerLoss
        loseWillpower(c, willloss, Math.round(extra), true, "")
        if (has(Trait.sexualDynamo)) {
            c.write(this, Global.format("{self:NAME-POSSESSIVE} climax makes "
                    + "{self:direct-object} positively gleam with erotic splendor; "
                    + "{self:possessive} every move seems more seductive than ever.",
                    this, opponent))
            add(c, Abuff(this, Attribute.Seduction, 5, 10))
        }
        if (has(Trait.lastStand) && opponent != null && opponent != this) {
            val tighten = OrgasmicTighten(this)
            val thrust = OrgasmicThrust(this)
            if (tighten.usable(c, opponent)) {
                tighten.resolve(c, opponent)
            }
            if (thrust.usable(c, opponent)) {
                thrust.resolve(c, opponent)
            }
        }
        if (this !== opponent && times == totalTimes && canRespond()) {          //FIXME: Explicitly Parentesize for clear order of operations. - DSM
            c.write(this, orgasmLiner)
            c.write(opponent, opponentOrgasmLiner)
        }
        if (has(Trait.nymphomania) && Global.random(100) < Math.sqrt((get(Attribute.Nymphomania) + get(Attribute.Animism)).toDouble()) * 10
                && !willpower.isAtUnfavorableExtreme && times == totalTimes) {
            if (human()) {
                c.write("Cumming actually made you feel kind of refreshed, albeit with a "
                        + "burning desire for more.")
            } else {
                c.write(Global.format(
                        "After {self:subject} comes down from {self:possessive} orgasmic high, "
                                + "{self:pronoun} doesn't look satisfied at all. There's a mad glint in "
                                + "{self:possessive} eye that seems to be endlessly asking for more.",
                        this, opponent))
            }
            restoreWillpower(c,
                    5 + Math.min((get(Attribute.Animism) + get(Attribute.Nymphomania)) / 5, 15))
        }
        if (times == totalTimes) {
            val purgedStatuses = statuses.filter { status ->
                (status.mindgames() && status.flags().contains(Stsflag.purgable)
                        || status.flags().contains(Stsflag.orgasmPurged))
                    }
            if (purgedStatuses.isNotEmpty()) {
                if (human()) {
                    c.write(this, "<b>Your mind clears up after your release.</b>")
                } else {
                    c.write(this, "<b>You see the light of reason return to "
                            + nameDirectObject() + " after "
                            + possessiveAdjective() + " release.</b>")
                }
                purgedStatuses.forEach { status -> this.removeStatus(status) }
            }
        }
        if (checkAddiction(AddictionType.CORRUPTION, opponent) && selfPart != null && opponentPart != null) {
            if (c.getStance().havingSex(c, this) && c.getCombatantData(this)!!.getIntegerFlag("ChoseToFuck") == 1) {
                c.write(this,
                        Global.format("{self:NAME-POSSESSIVE} willing sacrifice to "
                                + "{other:name-do} greatly reinforces the corruption inside "
                                + "of {self:direct-object}.", this, opponent))
                addict(c, AddictionType.CORRUPTION, opponent, Addiction.HIGH_INCREASE)
            }
            if (opponent!!.has(Trait.TotalSubjugation)
                    && c.getStance().en == Stance.succubusembrace) {
                c.write(this,
                        Global.format("The succubus takes advantage of {self:name-possessive} "
                                + "moment of vulnerability and overwhelms {self:posssessive} mind with "
                                + "{other:possessive} soul-corroding lips.", this, opponent))
                addict(c, AddictionType.CORRUPTION, opponent, Addiction.HIGH_INCREASE)
            }
        }
        if (checkAddiction(AddictionType.ZEAL, opponent) && selfPart != null && opponentPart != null && c.getStance().penetratedBy(c, opponent, this)
                && selfPart.isType(CockPart.TYPE)) {
            c.write(this,
                    Global.format("Experiencing so much pleasure inside of {other:name-do} "
                            + "reinforces {self:name-possessive} faith in the lovely goddess.",
                            this, opponent))
            addict(c, AddictionType.ZEAL, opponent, Addiction.MED_INCREASE)
        }
        if (checkAddiction(AddictionType.ZEAL, opponent) && selfPart != null && opponentPart != null && c.getStance().penetratedBy(c, this, opponent)
                && opponentPart.isType(CockPart.TYPE)
                && (selfPart.isType(PussyPart.TYPE) || selfPart.isType(AssPart.TYPE))) {
            c.write(this,
                    Global.format("Experiencing so much pleasure from {other:name-possessive} "
                            + "cock inside {self:direct-object} reinforces {self:name-possessive} faith.",
                            this, opponent))
            addict(c, AddictionType.ZEAL, opponent, Addiction.MED_INCREASE)
        }
        if (checkAddiction(AddictionType.BREEDER, opponent)) {
            // Clear combat addiction
            unaddictCombat(AddictionType.BREEDER, opponent, 1f, c)
        }
        if (checkAddiction(AddictionType.DOMINANCE, opponent) && c.getStance().dom(opponent)) {
            c.write(this, "Getting dominated by " + opponent!!.nameDirectObject()
                    + " seems to excite " + nameDirectObject() + " even more.")
            addict(c, AddictionType.DOMINANCE, opponent, Addiction.LOW_INCREASE)
        }
        orgasms += 1
    }

    /**Helper method for resolveOrgasm(). Writes dynamic text to the GUI based on bodypart.
     *
     * @param c
     * The combat that this method requires.
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param times
     * The number of times this person is orgasming.
     *
     * .
     */
    private fun resolvePreOrgasmForSolo(c: Combat, opponent: Character, selfPart: BodyPart?, times: Int) {
        if (selfPart != null && selfPart.isType(CockPart.TYPE)) {
            if (times == 1) {
                c.write(this, Global.format("<b>{self:NAME-POSSESSIVE} back arches as thick ropes of jizz fire from "
                        + "{self:possessive} dick and land on {self:reflective}.</b>",
                        this, opponent))
            } else {
                c.write(this, Global.format(
                        "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                                + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                                + "spent.</b>",
                        this, opponent))
            }
        } else {
            if (times == 1) {
                c.write(this, Global.format("<b>{self:SUBJECT-ACTION:shudder|shudders} as {self:pronoun} "
                        + "{self:action:bring|brings} {self:reflective} to a toe-curling climax.</b>",
                        this, opponent))
            } else {
                c.write(this, Global.format(
                        "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                                + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                                + "spent.</b>",
                        this, opponent))
            }
        }
    }

    /**Helper method for resolving the opponent's orgasm. Helps write text to the GUI.
     *
     * @param c
     * The combat that this method requires.
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param opponentPart
     * The opopnent's part that is orgasming.
     */
    private fun resolvePreOrgasmForOpponent(c: Combat, opponent: Character?, selfPart: BodyPart?, opponentPart: BodyPart?,
                                            times: Int, total: Int) {
        if (c.getStance().inserted(this) && !has(Trait.strapped)) {
            val partner = c.getStance().getPenetratedCharacter(c, this)
            val holePart = Global.pickRandom(c.getStance().getPartsFor(c, partner, this)).orElse(null)
            if (times == 1) {
                var hole = "pulsing hole"
                if (holePart != null && holePart.isType(BreastsPart.TYPE)) {
                    hole = "cleavage"
                } else if (holePart != null && holePart.isType(MouthPart.TYPE)) {
                    hole = "hungry mouth"
                }
                c.write(this, Global.format(
                        "<b>{self:SUBJECT-ACTION:tense|tenses} up as {self:possessive} hips "
                                + "wildly buck against {other:name-do}. In no time, {self:possessive} hot "
                                + "seed spills into {other:possessive} %s.</b>",
                        this, partner, hole))
            } else {
                c.write(this, Global.format(
                        "<b>{other:NAME-POSSESSIVE} devilish orfice does not let up, and "
                                + "{other:possessive} intense actions somehow force {self:name-do} to "
                                + "cum again instantly.</b>",
                        this, partner))
            }
            val opponentHolePart = Global.pickRandom(c.getStance().getPartsFor(c, opponent, this))
            opponentHolePart.ifPresent { bodyPart: BodyPart? -> partner.body.receiveCum(c, this, bodyPart) }
        } else if (selfPart != null && selfPart.isType(CockPart.TYPE) && opponentPart != null && !opponentPart.isType("none")) {
            if (times == 1) {
                c.write(this, Global.format(
                        "<b>{self:NAME-POSSESSIVE} back arches as thick ropes of jizz fire "
                                + "from {self:possessive} dick and land on {other:name-possessive} "
                                + opponentPart.describe(opponent) + ".</b>",
                        this, opponent))
            } else {
                c.write(this, Global.format(
                        "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                                + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                                + "spent.</b>",
                        this, opponent))
            }
            opponent!!.body.receiveCum(c, this, opponentPart)
        } else {
            if (times == 1) {
                c.write(this, Global.format(
                        "<b>{self:SUBJECT-ACTION:shudder|shudders} as "
                                + "{other:subject-action:bring|brings} {self:direct-object} "
                                + "to a toe-curling climax.</b>",
                        this, opponent))
            } else {
                c.write(this, Global.format(
                        "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                                + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                                + "spent.</b>",
                        this, opponent))
            }
        }
        if (opponent!!.has(Trait.mindcontroller) && cloned == 0) {
            val res = MindControl.Result(this, opponent, c.getStance())
            var message = res.description
            if (res.hasSucceeded()) {
                if (opponent.has(Trait.EyeOpener) && outfit.has(ClothingTrait.harpoonDildo)) {
                    message += ("Below, the vibrations of the dildo reach a powerful crescendo,"
                            + " and your eyes open wide in shock, a perfect target for "
                            + " what's coming next.")
                    addict(c, AddictionType.MIND_CONTROL, opponent, Addiction.LOW_INCREASE)
                } else if (opponent.has(Trait.EyeOpener) && outfit.has(ClothingTrait.harpoonOnahole)) {
                    message += ("The warm sheath around your dick suddenly tightens, pulling incredibly"
                            + ", almost painfully tight around the shaft. At the same time, it starts"
                            + " vibrating powerfully. The combined assault causes your eyes to open"
                            + " wide and defenseless.")
                    addict(c, AddictionType.MIND_CONTROL, opponent, Addiction.LOW_INCREASE)
                }
                message += ("While your senses are overwhelmed by your violent orgasm, the deep pools of Mara's eyes"
                        + " swirl and dance. You helplessly stare at the intricate movements and feel a strong"
                        + " pressure on your mind as you do. When your orgasm dies down, so do the dancing patterns."
                        + " With a satisfied smirk, Mara tells you to lift an arm. Before you have even processed"
                        + " her words, you discover that your right arm is sticking straight up into the air. This"
                        + " is probably really bad.")
                addict(c, AddictionType.MIND_CONTROL, opponent, Addiction.MED_INCREASE)
            }
            c.write(this, message)
        }
    }

    open fun getRandomLineFor(lineType: String?, c: Combat?, target: Character?): String? {
        return ""
    }

    /**Helper method for resolving what happens after orgasm for the opponent. Helps write dynamic text to the GUI.
     * @param c
     * The combat that this method requires.
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param opponentPart
     * The opopnent's part that is orgasming.
     *
     */
    private fun resolvePostOrgasmForOpponent(c: Combat, opponent: Character?, selfPart: BodyPart?, opponentPart: BodyPart?) {
        if (selfPart != null && opponentPart != null) {
            selfPart.onOrgasmWith(c, this, opponent, opponentPart, true)
            opponentPart.onOrgasmWith(c, opponent, this, selfPart, false)
        }
        body.onOrgasm(c, this, opponent)
        if (opponent!!.has(Trait.erophage)) {
            c.write(Global.capitalizeFirstLetter("<b>" +
                    opponent.subjectAction("flush", "flushes")
                    + " as the feedback from " + nameOrPossessivePronoun() + " orgasm feeds "
                    + opponent.possessiveAdjective() + " divine power.</b>"))
            opponent.add(c, Alluring(opponent, 5))
            opponent.buildMojo(c, 100)
            if (c.getStance().inserted(this) && opponent.has(Trait.divinity)) {
                opponent.add(c, DivineCharge(opponent, 1.0))
            }
        }
        if (opponent.has(Trait.sexualmomentum)) {
            c.write(Global.capitalizeFirstLetter("<b>"
                    + opponent.subjectAction("are more composed", "seems more composed")
                    + " as " + nameOrPossessivePronoun() + " forced orgasm goes straight to "
                    + opponent.possessiveAdjective() + " ego.</b>"))
            opponent.restoreWillpower(c, 10 + Global.random(10))
        }
    }

    fun loseWillpower(c: Combat?, i: Int, primary: Boolean) {
        loseWillpower(c, i, 0, primary, "")
    }

    /**Processes willpower loss for this character.
     *
     * @param c
     * The combat required for this method.
     * @param i
     * The base value of willpower loss.
     * @param extra
     *
     * @param primary
     * indicates if this is primary.
     * @param source
     * The source of the willpower loss.
     *
     */
    @JvmOverloads
    fun loseWillpower(c: Combat?, i: Int, extra: Int = 0, primary: Boolean = false, source: String? = "") {
        var amt = i + extra
        var reduced = ""
        if (has(Trait.strongwilled) && primary) {
            amt = amt * 2 / 3 + 1
            reduced += " (Strong-willed)"
        }
        if (`is`(Stsflag.feral) && primary) {
            amt = amt / 2
            reduced += " (Feral)"
        }
        val old = willpower.get()
        willpower.exhaust(amt)
        if (c != null) {
            c.writeSystemMessage(String.format(
                    "%s lost <font color='rgb(220,130,40)'>%s<font color='white'> willpower$reduced%s.",
                    Global.capitalizeFirstLetter(subject()), if (extra == 0) Integer.toString(amt) else "$i+$extra ($amt)",
                    source), true)
        } else if (human()) {
            Global.gui().systemMessage(String.format("%s lost <font color='rgb(220,130,40)'>%d<font color='white'> willpower" + reduced
                    + "%s.", subject(), amt, source))
        }
    }

    fun restoreWillpower(c: Combat, i: Int) {
        willpower.recover(i)
        c.writeSystemMessage(String.format("%s regained <font color='rgb(181,230,30)'>%d<font color='white'> willpower.", subject(), i), true)
    }

    /**Helper method that Handles the inserted?
     *
     * @param c
     * The Combat that this method requires.
     *
     */
    private fun handleInserted(c: Combat) {
        val partners = c.getStance().getAllPartners(c, this)
        partners.forEach { opponent: Character ->
            val selfOrganIt: Iterator<BodyPart>
            val otherOrganIt: Iterator<BodyPart>
            selfOrganIt = c.getStance().getPartsFor(c, this, opponent).iterator()
            otherOrganIt = c.getStance().getPartsFor(c, opponent, this).iterator()
            if (selfOrganIt.hasNext() && otherOrganIt.hasNext()) {
                val selfOrgan = selfOrganIt.next()
                val otherOrgan = otherOrganIt.next()
                if (has(Trait.energydrain) && selfOrgan != null && otherOrgan != null && selfOrgan.isErogenous && otherOrgan.isErogenous) {
                    c.write(this, Global.format(
                            "{self:NAME-POSSESSIVE} body glows purple as {other:subject-action:feel|feels} {other:possessive} very spirit drained into {self:possessive} "
                                    + selfOrgan.describe(this) + " through your connection.",
                            this, opponent))
                    val m = Global.random(5) + 5
                    opponent.drain(c, this, modifyDamage(DamageType.drain, opponent, m.toDouble()).toInt())
                }
                body.tickHolding(c, opponent, selfOrgan, otherOrgan)
            }
        }
    }

    open fun endOfCombatRound(c: Combat, opponent: Character) {
        dropStatus(c, opponent)
        tick(c)
        status.forEach { s: nightgames.status.Status? -> s!!.tick(c) }
        val removed: MutableList<String> = java.util.ArrayList()
        for (s in cooldowns.keys) {
            if (cooldowns[s]!! <= 1) {
                removed.add(s)
            } else {
                cooldowns[s] = cooldowns[s]!! - 1
            }
        }
        for (s in removed) {
            cooldowns.remove(s)
        }
        handleInserted(c)
        if (outfit.has(ClothingTrait.tentacleSuit)) {
            c.write(this, Global.format("The tentacle suit squirms against {self:name-possessive} body.", this,
                    opponent))
            if (hasBreasts()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.randomBreasts)
            }
            TentaclePart.pleasureWithTentacles(c, this, 5, body.skin)
        }
        if (outfit.has(ClothingTrait.tentacleUnderwear)) {
            var undieName = "underwear"
            if (hasPussy()) {
                undieName = "panties"
            }
            c.write(this, Global.format("The tentacle $undieName squirms against {self:name-possessive} crotch.",
                    this, opponent))
            if (hasDick()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.randomCock)
                body.pleasure(null, null, body.randomCock, 5.0, c)
            }
            if (hasBalls()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.randomBalls)
            }
            if (hasPussy()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.randomPussy)
            }
            TentaclePart.pleasureWithTentacles(c, this, 5, body.randomAss)
        }
        if (outfit.has(ClothingTrait.harpoonDildo)) {
            if (!hasPussy()) {
                c.write(Global.format("Since {self:name-possessive} pussy is now gone, the dildo that was stuck inside of it falls"
                        + " to the ground. {other:SUBJECT-ACTION:reel|reels} it back into its slot on"
                        + " {other:possessive} arm device.", this, opponent))
            } else {
                var damage = 5
                if (opponent.has(Trait.pussyhandler)) {
                    damage += 2
                }
                if (opponent.has(Trait.yank)) {
                    damage += 3
                }
                if (opponent.has(Trait.conducivetoy)) {
                    damage += 3
                }
                if (opponent.has(Trait.intensesuction)) {
                    damage += 3
                }
                c.write(Global.format("{other:NAME-POSSESSIVE} harpoon dildo is still stuck in {self:name-possessive}"
                        + " {self:body-part:pussy}, vibrating against {self:possessive} walls.", this, opponent))
                body.pleasure(opponent, ToysPart.dildo, body.randomPussy, damage.toDouble(), c)
            }
        }
        if (outfit.has(ClothingTrait.harpoonOnahole)) {
            if (!hasDick()) {
                c.write(Global.format("Since {self:name-possessive} dick is now gone, the onahole that was stuck onto it falls"
                        + " to the ground. {other:SUBJECT-ACTION:reel|reels} it back into its slot on"
                        + " {other:possessive} arm device.", this, opponent))
            } else {
                var damage = 5
                if (opponent.has(Trait.dickhandler)) {
                    damage += 2
                }
                if (opponent.has(Trait.yank)) {
                    damage += 3
                }
                if (opponent.has(Trait.conducivetoy)) {
                    damage += 3
                }
                if (opponent.has(Trait.intensesuction)) {
                    damage += 3
                }
                c.write(Global.format("{other:NAME-POSSESSIVE} harpoon onahole is still stuck on {self:name-possessive}"
                        + " {self:body-part:cock}, vibrating against {self:possessive} shaft.", this, opponent))
                body.pleasure(opponent, ToysPart.onahole, body.randomCock, damage.toDouble(), c)
            }
        }
        if (getPure(Attribute.Animism) >= 4 && arousal.percent() >= 50 && !`is`(Stsflag.feral)) {
            add(c, Feral(this))
        }
        if (opponent.has(Trait.temptingass) && !`is`(Stsflag.frenzied)) {
            var chance = 20
            chance += Math.max(0, Math.min(15, opponent[Attribute.Seduction] - get(Attribute.Seduction)))
            if (`is`(Stsflag.feral)) chance += 10
            if (`is`(Stsflag.charmed) || opponent.`is`(Stsflag.alluring)) chance += 5
            if (has(Trait.assmaster) || has(Trait.analFanatic)) chance += 5
            val fetish = body.getFetish(AssPart.TYPE)
            if (fetish.isPresent && opponent.has(Trait.bewitchingbottom)) {
                chance += (20 * fetish.get().magnitude).toInt()
            }
            if (chance >= Global.random(100)) {
                val fuck = AssFuck(this)
                if (fuck.requirements(c, opponent) && fuck.usable(c, opponent)) {
                    c.write(opponent,
                            Global.format("<b>The look of {other:name-possessive} ass,"
                                    + " so easily within {self:possessive} reach, causes"
                                    + " {self:subject} to involuntarily switch to autopilot."
                                    + " {self:SUBJECT} simply {self:action:NEED|NEEDS} that ass.</b>",
                                    this, opponent))
                    add(c, Frenzied(this, 1))
                }
            }
        }
        pleasured = false
        val opponentAssistants = java.util.ArrayList(c.assistantsOf(opponent))
        Collections.shuffle(opponentAssistants)
        val randomOpponentPetOrNull = opponentAssistants.firstOrNull()
        if (!isPet() && randomOpponentPetOrNull != null) {
            val pet = randomOpponentPetOrNull.character
            val weakenBetter = (modifyDamage(DamageType.physical, pet, 100.0) / pet.stamina.remaining()
                    > 100 / pet.stamina.remaining())
            if (canAct() && c.getStance().mobile(this) && pet.roll(this, c, 20)) {
                c.write(this, Global.format("<b>{self:SUBJECT-ACTION:turn} {self:possessive} attention"
                        + " on {other:name-do}</b>", this, pet))
                if (weakenBetter) {
                    c.write(Global.format("{self:SUBJECT-ACTION:focus|focuses} {self:possessive} attentions on {other:name-do}, "
                            + "thoroughly exhausting {other:direct-object} in a game of cat and mouse.<br/>", this, pet))
                    pet.weaken(c, modifyDamage(DamageType.physical, pet, Global.random(10, 20).toDouble()).toInt())
                } else {
                    c.write(Global.format("{self:SUBJECT-ACTION:focus|focuses} {self:possessive} attentions on {other:name-do}, "
                            + "harassing and toying with {other:possessive} body as much as {self:pronoun} can.<br/>", this, pet))
                    pet.body.pleasure(this, body.randomHands, pet.body.randomGenital, Global.random(10, 20).toDouble(), c)
                }
            }
        }
        if (has(Trait.apostles)) {
            Apostles.eachCombatRound(c, this, opponent)
        }
        if (has(Trait.Rut) && Global.random(100) < (arousal.percent() - 25) / 2 && !`is`(Stsflag.frenzied)) {
            c.write(this, Global.format("<b>{self:NAME-POSSESSIVE} eyes dilate and {self:possessive} body flushes as {self:pronoun-action:descend|descends} into a mating frenzy!</b>", this, opponent))
            add(c, Frenzied(this, 3, true))
        }
    }

    open fun orgasmLiner(c: Combat?, target: Character?): String {         //FIXME: This could be an astract method. Eclipse just doesn't like you changing them by adding args after you first sign them.- DSM
        return ""
    }

    open fun makeOrgasmLiner(c: Combat?, target: Character?): String {    //FIXME: This could be an astract method. Eclipse just doesn't like you changing them by adding args after you first sign them.- DSM
        return ""
    }

    private val orgasmWillpowerLoss: Int
        private get() = 25

    abstract fun emote(emo: Emotion?, amt: Int)
    fun learn(copy: Skill) {
        skills.addIfAbsent(copy.copy(this))
    }

    open fun notifyTravel(dest: Area?, message: String?) {}
    open fun endOfMatchRound() {
        regen()
        tick(null)
        status.forEach { obj: nightgames.status.Status? -> obj!!.afterMatchRound() }
        if (has(Trait.Confident)) {
            willpower.recover(10)
            mojo.deplete(5)
        } else {
            willpower.recover(5)
            mojo.deplete(10)
        }
        if (has(Trait.exhibitionist) && mostlyNude()) {
            mojo.build(2)
        }
        dropStatus(null, null)
        if (has(Trait.QuickRecovery)) {
            heal(null, Global.random(4, 7), " (Quick Recovery)")
        }
        update()
    }

    open fun gain(item: Item?) {
        gain(item, 1)
    }

    fun remove(item: Item?) {
        gain(item, -1)
    }

    fun gain(item: Clothing) {
        closet.add(item)
        update()
    }

    fun gain(item: Item?, q: Int) {
        var amt = 0
        if (inventory.containsKey(item)) {
            amt = count(item)
        }
        inventory[item] = Math.max(0, amt + q)
        update()
    }

    @JvmOverloads
    fun has(item: Item?, quantity: Int = 1): Boolean {
        return inventory.containsKey(item) && inventory[item]!! >= quantity
    }

    fun unequipAllClothing() {
        closet.addAll(outfitPlan)
        outfitPlan.clear()
        change()
    }

    fun has(item: Clothing?): Boolean {
        return closet.contains(item) || outfit.equipped.contains(item)
    }

    @JvmOverloads
    fun consume(item: Item?, quantity: Int, canBeResourceful: Boolean = true) {
        var quantity = quantity
        if (canBeResourceful && has(Trait.resourceful) && Global.random(5) == 0) {
            quantity--
        }
        if (inventory.containsKey(item)) {
            gain(item, -quantity)
        }
    }

    fun count(item: Item?): Int {
        return if (inventory.containsKey(item)) {
            inventory[item]!!
        } else 0
    }

    fun chargeBattery() {
        val power = count(Item.Battery)
        if (power < 20) {
            gain(Item.Battery, 20 - power)
        }
    }

    /**Performs the tasks associated with finishing a match. temporary traits are removed while meters are reset.
     *
     */
    fun finishMatch() {
        Global.gui().clearImage()
        change()
        clearStatus()
        temporaryAddedTraits.clear()
        temporaryRemovedTraits.clear()
        body.purge(null)
        stamina.renew()
        arousal.renew()
        mojo.renew()
    }

    abstract fun challenge(other: Character): String?
    fun lvlBonus(opponent: Character): Int {
        return if (opponent.progression.level > progression.level) {
            12 * (opponent.progression.level - progression.level)
        } else {
            0
        }
    }

    fun getVictoryXP(opponent: Character): Int {
        return 25 + lvlBonus(opponent)
    }

    fun getAssistXP(opponent: Character): Int {
        return 18 + lvlBonus(opponent)
    }

    fun getDefeatXP(opponent: Character): Int {
        return 18 + lvlBonus(opponent)
    }

    /**Gets the attraction of this character to another. */
    fun getAttraction(other: Character?): Int {
        if (other == null) {
            System.err.println("Other is null")
            Thread.dumpStack()
            return 0
        }
        return if (attractions.containsKey(other.type)) {
            attractions[other.type]!!
        } else {
            0
        }
    }

    /**Gains attraction value x to a given other character.
     * @param other
     * The character to gain attraction with.
     * @param x
     * the amount of attraction to gain.
     */
    fun gainAttraction(other: Character?, x: Int) {
        if (other == null) {
            System.err.println("Other is null")
            Thread.dumpStack()
            return
        }
        if (attractions.containsKey(other.type)) {
            attractions[other.type] = attractions[other.type]!! + x
        } else {
            attractions[other.type] = x
        }
    }

    fun getAffections(): Map<String?, Int?> {
        return Collections.unmodifiableMap(affections)
    }

    fun getAffection(other: Character?): Int {
        if (other == null) {
            System.err.println("Other is null")
            Thread.dumpStack()
            return 0
        }
        return if (affections.containsKey(other.type)) {
            affections[other.type]!!
        } else {
            0
        }
    }

    fun gainAffection(other: Character?, x: Int) {
        var x = x
        if (other == null) {
            System.err.println("Other is null")
            Thread.dumpStack()
            return
        }
        if (other === this) {
            //skip narcissism.
            return
        }
        if (other.has(Trait.affectionate) && Global.random(2) == 0) {
            x += 1
        }
        if (affections.containsKey(other.type)) {
            affections[other.type] = affections[other.type]!! + x
        } else {
            affections[other.type] = x
        }
    }

    /**outputs the evasion bonus as a result of traits and status effects that affect it. */
    fun evasionBonus(): Int {
        var ac = 0
        for (s in statuses) {
            ac += s.evade()
        }
        if (has(Trait.clairvoyance)) {
            ac += 5
        }
        if (has(Trait.FeralAgility) && `is`(Stsflag.feral)) {
            ac += 5
        }
        return ac
    }

    private val statuses: Collection<nightgames.status.Status>
        private get() = status

    /**outputs the counter chance as a result of traits and status effects that affect it. */
    fun counterChance(c: Combat?, opponent: Character, skill: Skill?): Int {
        var counter = 3
        // subtract some counter chance if the opponent is more cunning than you.
        // 1% decreased counter chance per 5 points of cunning over you.
        counter += Math.min(0, get(Attribute.Cunning) - opponent[Attribute.Cunning]) / 5
        // increase counter chance by perception difference
        counter += get(Attribute.Perception) - opponent[Attribute.Perception]
        // 1% increased counter chance per 2 speed over your opponent.
        counter += getSpeedDifference(opponent) / 2
        for (s in statuses) {
            counter += s.counter()
        }
        if (has(Trait.clairvoyance)) {
            counter += 3
        }
        if (has(Trait.aikidoNovice)) {
            counter += 3
        }
        if (has(Trait.fakeout)) {
            counter += 3
        }
        if (opponent.`is`(Stsflag.countered)) {
            counter -= 10
        }
        if (has(Trait.FeralAgility) && `is`(Stsflag.feral)) {
            counter += 5
        }
        // Maximum counter chance is 3 + 5 + 2 + 3 + 3 + 3 + 5 = 24, which is super hard to achieve.
        // I guess you also get some more counter with certain statuses effects like water form.
        // Counters should be pretty rare.
        return Math.max(0, counter)
    }

    private fun getSpeedDifference(opponent: Character): Int {
        return Math.min(Math.max(get(Attribute.Speed) - opponent[Attribute.Speed], -5), 5)
    }

    /**Determines and returns the chace to hit, depending on the given accuracy and differences in levels, as well as traits. */
    fun getChanceToHit(attacker: Character, c: Combat?, accuracy: Int): Int {
        val hitDiff = attacker.getSpeedDifference(this) + (attacker[Attribute.Perception] - get(
                Attribute.Perception))
        var levelDiff = Math.min(attacker.progression.level - progression.level, 5)
        levelDiff = Math.max(levelDiff, -5)

        // with no level or hit differences and an default accuracy of 80, 80%
        // hit rate
        // each level the attacker is below the target will reduce this by 2%,
        // to a maximum of 10%
        // each point in accuracy of skill affects changes the hit chance by 1%
        // each point in speed and perception will increase hit by 5%
        var chanceToHit = 2 * levelDiff + accuracy + 5 * (hitDiff - evasionBonus())
        if (has(Trait.hawkeye)) {
            chanceToHit += 5
        }
        return chanceToHit
    }

    /**Used by many resolve functions, this method returns true if the attacker hits with a given accuracy. */
    fun roll(attacker: Character, c: Combat?, accuracy: Int): Boolean {
        val attackroll = Global.random(100)
        val chanceToHit = getChanceToHit(attacker, c, accuracy)
        return attackroll < chanceToHit
    }

    /**Determines the Difficulty Class for knocking someone down. */
    fun knockdownDC(): Int {
        var dc = 10 + stamina.get() / 10 + stamina.percent() / 5
        if (`is`(Stsflag.braced)) {
            dc += getStatus(Stsflag.braced)!!.value()
        }
        if (has(Trait.stabilized)) {
            dc += (12 + 3 * Math.sqrt(get(Attribute.Science).toDouble())).toInt()
        }
        if (has(ClothingTrait.heels) && !has(Trait.proheels)) {
            dc -= 7
        }
        if (has(ClothingTrait.highheels) && !has(Trait.proheels)) {
            dc -= 8
        }
        if (has(ClothingTrait.higherheels) && !has(Trait.proheels)) {
            dc -= 10
        }
        if (bound()) {
            dc /= 2
        }
        if (stunned()) {
            dc /= 4
        }
        return dc
    }

    abstract fun counterattack(target: Character?, type: Tactics?, c: Combat?)
    fun clearStatus() {
        status.removeIf { status: nightgames.status.Status? -> !status!!.flags().contains(Stsflag.permanent) }
    }

    fun getStatus(flag: Stsflag): nightgames.status.Status? {
        return getStatusesWithFlag(flag).firstOrNull()
    }

    // terrible code? who me? nahhhhh.
    fun <T> getStatusOfClass(clazz: Class<T>) = status.filterIsInstance(clazz)

    val insertedStatus: Collection<InsertedStatus>
        get() = getStatusOfClass(InsertedStatus::class.java)

    /**Returns an Integer representing the value of prize for --defeating?-- this character. The prize depends on the rank of the characcter. */
    fun prize(): Int {
        return if (progression.rank >= 2) {
            500
        } else if (progression.rank == 1) {
            200
        } else {
            50
        }
    }

    /**Processes teh end of the battle for this character.  */
    fun endofbattle(c: Combat?) {
        for (s in status) {
            if (!s.lingering() && !s.flags().contains(Stsflag.permanent)) {
                removelist.add(s)
            }
        }
        cooldowns.clear()
        dropStatus(null, null)
        orgasms = 0
        update()
        if (has(ClothingTrait.heels)) {
            setFlag("heelsTraining", getFlag("heelsTraining") + 1)
        }
        if (has(ClothingTrait.highheels)) {
            setFlag("heelsTraining", getFlag("heelsTraining") + 1)
        }
        if (has(ClothingTrait.higherheels)) {
            setFlag("heelsTraining", getFlag("heelsTraining") + 1)
        }
        if (`is`(Stsflag.disguised) || has(Trait.slime)) {
            purge(c)
        }
        if (has(ClothingTrait.harpoonDildo)) {
            outfit.unequip(Clothing.getByID("harpoondildo"))
        }
        if (has(ClothingTrait.harpoonOnahole)) {
            outfit.unequip(Clothing.getByID("harpoononahole"))
        }
    }

    fun setFlag(string: String?, i: Int) {
        flags[string] = i
    }

    fun getFlag(string: String?): Int {
        return if (flags.containsKey(string)) {
            flags[string]!!
        } else 0
    }

    fun canSpend(mojo: Int): Boolean {
        var cost = mojo
        for (s in statuses) {
            cost += s.spendmojo(mojo)
        }
        return this.mojo.get() >= cost
    }

    fun getInventory(): Map<Item?, Int> {
        return inventory
    }

    fun listStatus(): java.util.ArrayList<String> {
        val result = java.util.ArrayList<String>()
        for (s in statuses) {
            result.add(s.toString())
        }
        return result
    }

    /**Dumps stats to the GUi.
     */
    fun dumpstats(notableOnly: Boolean): String {
        val b = StringBuilder()
        b.append("<b>")
        b.append(trueName + ": Level " + progression.level + "; ")
        for (a in att.keys) {
            b.append(a!!.name + " " + att[a] + ", ")
        }
        b.append("</b>")
        b.append("<br/>Max Stamina " + stamina.max() + ", Max Arousal " + arousal.max() + ", Max Mojo " + mojo.max()
                + ", Max Willpower " + willpower.max() + ".")
        b.append("<br/>")
        if (human()) {
            // ALWAYS GET JUDGED BY ANGEL. lol.
            body.describeBodyText(b, Global.getCharacterByType("Angel"), notableOnly)
        } else {
            body.describeBodyText(b, Global.getPlayer(), notableOnly)
        }
        if (getTraits().size > 0) {
            b.append("<br/>Traits:<br/>")
            val traits: MutableList<Trait> = getTraits().toMutableList()
            traits.sortWith(Comparator { first: Trait, second: Trait -> first.toString().compareTo(second.toString()) })
            for (t in traits) {
                b.append(t.toString() + ": " + t.desc)
                b.append("<br/>")
            }
        }
        b.append("</p>")
        return b.toString()
    }

    override fun toString(): String {
        return type
    }

    /** */
    fun getOtherFitness(c: Combat, other: Character): Float {
        var fit = 0f
        // Urgency marks
        var arousalMod = 1.0f
        val staminaMod = 1.0f
        val mojoMod = 1.0f
        val usum = arousalMod + staminaMod + mojoMod
        val escape = other.getEscape(c, this)
        if (escape > 1) {
            fit += (8 * Math.log(escape.toDouble())).toFloat()
        } else if (escape < -1) {
            fit += (-8 * Math.log(-escape.toDouble())).toFloat()
        }
        var totalAtts = 0
        for (attribute in att.keys) {
            totalAtts += att[attribute]!!
        }
        fit += (Math.sqrt(totalAtts.toDouble()) * 5).toFloat()

        // what an average piece of clothing should be worth in fitness
        var topFitness = 8.0
        var bottomFitness = 6.0
        // If I'm horny, I want the other guy's clothing off, so I put more
        // fitness in them
        if (mood == Emotion.horny) {
            topFitness += 6.0
            bottomFitness += 8.0
            // If I'm horny, I want to make the opponent cum asap, put more
            // emphasis on arousal
            arousalMod = 2.0f
        }

        // check body parts based on my preferences
        if (other.hasDick()) {
            fit -= ((dickPreference() - 3) * 4).toFloat()
        }
        if (other.hasPussy()) {
            fit -= ((pussyPreference() - 3) * 4).toFloat()
        }
        fit += c.assistantsOf(other).sumOf { assistant -> assistant.fitness }.toFloat()
        fit += other.outfit.getFitness(c, bottomFitness, topFitness).toFloat()
        fit += other.body.getCharismaBonus(c, this).toFloat()
        // Extreme situations
        if (other.arousal.isAtUnfavorableExtreme) {
            fit -= 50f
        }
        // will power empty is a loss waiting to happen
        if (other.willpower.isAtUnfavorableExtreme) {
            fit -= 100f
        }
        if (other.stamina.isAtUnfavorableExtreme) {
            fit -= staminaMod * 3
        }
        fit += other.willpower.real * 5.33f
        // Short-term: Arousal
        fit += arousalMod / usum * 100.0f * (other.arousal.max() - other.arousal.get()) / Math
                .min(100, other.arousal.max())
        // Mid-term: Stamina
        fit += (staminaMod / usum * 50.0f * (1 - Math
                .exp((-other.stamina.get().toFloat() / Math.min(other.stamina.max().toFloat(), 100.0f)).toDouble()))).toFloat()
        // Long term: Mojo
        fit += (mojoMod / usum * 50.0f * (1 - Math
                .exp((-other.mojo.get().toFloat() / Math.min(other.mojo.max().toFloat(), 40.0f)).toDouble()))).toFloat()
        for (status in other.statuses) {
            fit += status.fitnessModifier()
        }
        // hack to make the AI favor making the opponent cum
        fit -= (100 * other.orgasms).toFloat()
        // special case where if you lost, you are super super unfit.
        if (other.orgasmed && other.willpower.isAtUnfavorableExtreme) {
            fit -= 1000f
        }
        return fit
    }

    fun getFitness(c: Combat): Float {
        var fit = 0f
        // Urgency marks
        var arousalMod = 1.0f
        val staminaMod = 2.0f
        val mojoMod = 1.0f
        val usum = arousalMod + staminaMod + mojoMod
        val other = c.getOpponentCharacter(this)
        var totalAtts = 0
        for (attribute in att.keys) {
            totalAtts += att[attribute]!!
        }
        fit += (Math.sqrt(totalAtts.toDouble()) * 5).toFloat()
        // Always important: Position
        fit += c.assistantsOf(this).sumOf { assistant -> assistant.fitness }.toFloat()
        val escape = getEscape(c, other)
        if (escape > 1) {
            fit += (8 * Math.log(escape.toDouble())).toFloat()
        } else if (escape < -1) {
            fit += (-8 * Math.log(-escape.toDouble())).toFloat()
        }
        // what an average piece of clothing should be worth in fitness
        var topFitness = 4.0
        var bottomFitness = 4.0
        // If I'm horny, I don't care about my clothing, so I put more less
        // fitness in them
        if (mood == Emotion.horny || `is`(Stsflag.feral)) {
            topFitness = .5
            bottomFitness = .5
            // If I'm horny, I put less importance on my own arousal
            arousalMod = .7f
        }
        fit += outfit.getFitness(c, bottomFitness, topFitness).toFloat()
        fit += body.getCharismaBonus(c, other).toFloat()
        if (c.getStance().inserted()) { // If we are fucking...
            // ...we need to see if that's beneficial to us.
            fit += body.penetrationFitnessModifier(this, other, c.getStance().inserted(this),
                    c.getStance().anallyPenetrated(c))
        }
        if (hasDick()) {
            fit += ((dickPreference() - 3) * 4).toFloat()
        }
        if (hasPussy()) {
            fit += ((pussyPreference() - 3) * 4).toFloat()
        }
        if (has(Trait.pheromones)) {
            fit += (5 * pheromonePower).toFloat()
            fit += (15 * getPheromonesChance(c) * (2 + pheromonePower)).toFloat()
        }

        // Also somewhat of a factor: Inventory (so we don't
        // just use it without thinking)
        for (item in inventory.keys) {
            fit += item!!.price.toFloat() / 10
        }
        // Extreme situations
        if (arousal.isAtUnfavorableExtreme) {
            fit -= 100f
        }
        if (stamina.isAtUnfavorableExtreme) {
            fit -= staminaMod * 3
        }
        fit += willpower.real * 5.3f
        // Short-term: Arousal
        fit += (arousalMod / usum * 100.0f * (arousal.max() - arousal.get())
                / Math.min(100, arousal.max()))
        // Mid-term: Stamina
        fit += (staminaMod / usum * 50.0f
                * (1 - Math.exp((-stamina.get().toFloat() / Math.min(stamina.max().toFloat(), 100.0f)).toDouble()))).toFloat()
        // Long term: Mojo
        fit += (mojoMod / usum * 50.0f * (1 - Math.exp((-mojo.get().toFloat() / Math.min(mojo.max().toFloat(), 40.0f)).toDouble()))).toFloat()
        for (status in statuses) {
            fit += status.fitnessModifier()
        }
        if (this is NPC) {
            val mods = this.ai.aiModifiers!!
            fit += (mods.modPosition(c.getStance().enumerate()) * 6).toFloat()
            fit += status.flatMap { s -> s.flags() }.sumOf { f -> mods.modSelfStatus(f) }.toFloat()
            fit += c.getOpponentCharacter(this)
                    .status.flatMap { s -> s.flags() }.sumOf { f -> mods.modOpponentStatus(f) }.toFloat()
        }
        // hack to make the AI favor making the opponent cum
        fit -= (100 * orgasms).toFloat()
        // special case where if you lost, you are super super unfit.
        if (orgasmed && willpower.isAtUnfavorableExtreme) {
            fit -= 1000f
        }
        return fit
    }

    open fun nameOrPossessivePronoun(): String {
        return getName() + "'s"
    }

    fun getExposure(slot: ClothingSlot?): Double {
        return outfit.getExposure(slot)
    }

    val exposure: Double
        get() = outfit.exposure
    abstract val portrait: String?
    fun modMoney(i: Int) {
        setMoney((money + Math.round(i * Global.moneyRate)).toInt())
    }

    fun setMoney(i: Int) {
        money = i
        update()
    }

    fun pronoun(): String {
        return grammar.subject().pronoun()
    }

    open val mood: Emotion
        get() = Emotion.confident

    fun possessiveAdjective(): String {
        return grammar.possessiveAdjective()
    }

    fun possessivePronoun(): String {
        return grammar.possessivePronoun()
    }

    fun objectPronoun(): String {
        return grammar.`object`().pronoun()
    }

    fun reflexivePronoun(): String {
        return grammar.reflexivePronoun()
    }

    fun useFemalePronouns(): Boolean {
        return (hasPussy()
                || !hasDick() || body.randomBreasts.size.compareTo(BreastsPart.Size.min()) > 0 && body.face.getFemininity(this) > 0) || body.face.getFemininity(this) >= 1.5 || human() && Global.checkFlag(Flag.PCFemalePronounsOnly) || !human() && Global.checkFlag(Flag.NPCFemalePronounsOnly)
    }

    open fun nameDirectObject(): String {
        return grammar.`object`().defaultNoun()
    }

    fun clothingFuckable(part: BodyPart): Boolean {
        if (part.isType(StraponPart.TYPE)) {
            return true
        }
        return if (part.isType(CockPart.TYPE)) {
            outfit.slotEmptyOrMeetsCondition(ClothingSlot.bottom
            ) { article: Clothing ->
                (!article.`is`(ClothingTrait.armored) && !article.`is`(ClothingTrait.bulky)
                        && !article.`is`(ClothingTrait.persistent))
            }
        } else if (part.isType(PussyPart.TYPE) || part.isType(AssPart.TYPE)) {
            outfit.slotEmptyOrMeetsCondition(ClothingSlot.bottom) { article: Clothing ->
                (article.`is`(ClothingTrait.skimpy) || article.`is`(ClothingTrait.open)
                        || article.`is`(ClothingTrait.flexible))
            }
        } else {
            false
        }
    }

    fun pussyPreference(): Double {
        return (11 - Global.getValue(Flag.malePref)).toDouble()
    }

    open fun dickPreference(): Double {
        return Global.getValue(Flag.malePref).toDouble()
    }

    fun wary(): Boolean {
        return hasStatus(Stsflag.wary)
    }

    fun gain(c: Combat?, item: Item) {
        c?.write(Global.format("<b>{self:subject-action:have|has} gained " + item.pre() + item.getName() + "</b>",
                this, this))
        gain(item, 1)
    }

    open fun temptLiner(c: Combat, target: Character?): String? {
        return if (c.getStance().sub(this)) {
            Global.format("{self:SUBJECT-ACTION:try} to entice {other:name-do} by wiggling suggestively in {other:possessive} grip.", this, target)
        } else Global.format("{self:SUBJECT-ACTION:pat} {self:possessive} groin and {self:action:promise} {self:pronoun-action:will} show {other:direct-object} a REAL good time.", this, target)
    }

    open fun action(firstPerson: String?, thirdPerson: String?): String? {
        return thirdPerson
    }

    fun action(verb: String?): String? {
        return action(verb, ProseUtils.getThirdPersonFromFirstPerson(verb))
    }

    fun addCooldown(skill: Skill) {
        if (skill.cooldown <= 0) {
            return
        }
        if (cooldowns.containsKey(skill.toString())) {
            cooldowns[skill.toString()] = cooldowns[skill.toString()]!! + skill.cooldown
        } else {
            cooldowns[skill.toString()] = skill.cooldown
        }
    }

    fun cooldownAvailable(s: Skill): Boolean {
        var cooledDown = true
        if (cooldowns.containsKey(s.toString()) && cooldowns[s.toString()]!! > 0) {
            cooledDown = false
        }
        return cooledDown
    }

    fun getCooldown(s: Skill): Int? {
        return if (cooldowns.containsKey(s.toString()) && cooldowns[s.toString()]!! > 0) {
            cooldowns[s.toString()]
        } else {
            0
        }
    }

    fun checkLoss(c: Combat): Boolean {
        return (orgasmed || c.timer > 150) && willpower.isAtUnfavorableExtreme
    }

    fun recruitLiner(): String {
        return ""
    }

    fun stripDifficulty(other: Character): Int {
        if (outfit.has(ClothingTrait.tentacleSuit) || outfit.has(ClothingTrait.tentacleUnderwear)) {
            return other[Attribute.Science] + 20
        }
        if (outfit.has(ClothingTrait.harpoonDildo) || outfit.has(ClothingTrait.harpoonOnahole)) {
            var diff = 20
            if (other.has(Trait.yank)) {
                diff += 5
            }
            if (other.has(Trait.conducivetoy)) {
                diff += 5
            }
            if (other.has(Trait.intensesuction)) {
                diff += 5
            }
            return diff
        }
        return 0
    }

    fun drainWillpower(c: Combat?, drainer: Character, i: Int) {
        var drained = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.drained(c, drained)
        }
        drained += bonus
        if (drained >= willpower.get()) {
            drained = willpower.get()
        }
        drained = Math.max(1, drained)
        val restored = drained
        c?.writeSystemMessage(String.format("%s drained of <font color='rgb(220,130,40)'>%d<font color='white'> willpower<font color='white'> by %s",
                subjectWas(), drained, drainer.subject()), true)
        willpower.exhaust(drained)
        drainer.willpower.recover(restored)
    }

    fun drainWillpowerAsMojo(c: Combat?, drainer: Character, i: Int, efficiency: Float) {
        var drained = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.drained(c, drained)
        }
        drained += bonus
        if (drained >= willpower.get()) {
            drained = willpower.get()
        }
        drained = Math.max(1, drained)
        val restored = Math.round(drained * efficiency)
        c?.writeSystemMessage(String.format("%s drained of <font color='rgb(220,130,40)'>%d<font color='white'> willpower as <font color='rgb(100,162,240)'>%d<font color='white'> mojo by %s",
                subjectWas(), drained, restored, drainer.subject()), true)
        willpower.exhaust(drained)
        drainer.mojo.build(restored)
    }

    fun drainStaminaAsMojo(c: Combat?, drainer: Character, i: Int, efficiency: Float) {
        var drained = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.drained(c, drained)
        }
        drained += bonus
        if (drained >= stamina.get()) {
            drained = stamina.get()
        }
        drained = Math.max(1, drained)
        val restored = Math.round(drained * efficiency)
        c?.writeSystemMessage(String.format("%s drained of <font color='rgb(240,162,100)'>%d<font color='white'> stamina as <font color='rgb(100,162,240)'>%d<font color='white'> mojo by %s",
                subjectWas(), drained, restored, drainer.subject()), true)
        stamina.exhaust(drained)
        drainer.mojo.build(restored)
    }

    fun drainMojo(c: Combat?, drainer: Character, i: Int) {
        var drained = i
        var bonus = 0
        for (s in statuses) {
            bonus += s.drained(c, drained)
        }
        drained += bonus
        if (drained >= mojo.get()) {
            drained = mojo.get()
        }
        drained = Math.max(1, drained)
        c?.writeSystemMessage(String.format("%s drained of <font color='rgb(0,162,240)'>%d<font color='white'> mojo by %s",
                subjectWas(), drained, drainer.subject()), true)
        mojo.deplete(drained)
        drainer.mojo.build(drained)
    }

    // TODO: Rename this method; it has the same name as Observer's update(), which is a little
    // confusing given that this is an Observable.
    fun update() {
        setChanged()
        notifyObservers()
    }

    fun footAvailable(): Boolean {
        val article = outfit.getTopOfSlot(ClothingSlot.feet)
        return article == null || article.layer < 2
    }

    fun hasInsertable(): Boolean {
        return hasDick() || has(Trait.strapped)
    }

    val isDemonic: Boolean
        /**Checks if this character has any mods that would consider them demonic.
         *
         * FIXME: Shouldn't the Incubus Trait also exist and be added to this?
         *
         * @return
         * Returns true if They have a demonic attributeModifier on their pussy or cock, or has the succubus trait.
         */
        get() = (has(Trait.succubus) || body.randomPussy.moddedPartCountsAs(DemonicMod.TYPE)
                || body.randomCock.moddedPartCountsAs(IncubusCockMod.TYPE))

    fun baseDisarm(): Int {
        var disarm = 0
        if (has(Trait.cautious)) {
            disarm += 5
        }
        return disarm
    }

    /**Helper method for getDamage() - modifies recoil pleasure damage.
     *
     * @return
     * Returns a floating decimal modifier.
     */
    fun modRecoilPleasure(c: Combat, mt: Float): Float {
        var total = mt
        if (c.getStance().sub(this)) {
            total += (get(Attribute.Submissive) / 2).toFloat()
        }
        if (has(Trait.responsive)) {
            total += total / 2
        }
        return total
    }

    fun isPartProtected(target: BodyPart): Boolean {
        return target.isType(HandsPart.TYPE) && has(ClothingTrait.nursegloves)
    }

    /**Removes temporary traits from this character.
     */
    fun purge(c: Combat?) {
        temporaryAddedTraits.clear()
        temporaryRemovedTraits.clear()
        status = status.filter { Stsflag.purgable !in it.flags() }.toMutableList()
        body.purge(c)
    }

    /**
     * applies bonuses and penalties for using an attribute.
     */
    fun usedAttribute(att: Attribute, c: Combat?, baseChance: Double) {
        // divine recoil applies at 20% per magnitude
        if (att == Attribute.Divinity && Global.randomdouble() < baseChance) {
            add(c, DivineRecoil(this, 1.0))
        }
    }

    /**
     * Attempts to knock down this character
     */
    fun knockdown(c: Combat?, other: Character, attributes: Set<Attribute>, strength: Int, roll: Int) {
        if (canKnockDown(c, other, attributes, strength, roll.toDouble())) {
            add(c, Falling(this))
        }
    }

    fun knockdownBonus(): Int {
        return 0
    }

    fun canKnockDown(c: Combat?, other: Character, attributes: Set<Attribute>, strength: Int, roll: Double): Boolean {
        return knockdownDC() < strength + roll * 100 + attributes.sumOf { attribute -> other[attribute] } +
                other.knockdownBonus()
    }

    fun checkResists(type: ResistType?, other: Character?, value: Double, roll: Double): Boolean {
        return when (type) {
            ResistType.mental -> value < roll * 100
            else -> false
        }
    }

    /**
     * If true, count insertions by this character as voluntary
     */
    fun canMakeOwnDecision(): Boolean {
        return !`is`(Stsflag.charmed) && !`is`(Stsflag.lovestruck) && !`is`(Stsflag.frenzied)
    }

    fun printStats(): String {
        return ("Character{" + "name='" + trueName + '\'' + ", type=" + type + ", level=" + progression.level +
                ", xp=" + progression.xp + ", rank=" + progression.rank + ", money=" + money +
                ", att=" + att + ", stamina=" + stamina.max()
                + ", arousal=" + arousal.max() + ", mojo=" + mojo.max() + ", willpower=" + willpower.max()
                + ", outfit=" + outfit + ", traits=" + traits + ", inventory=" + inventory + ", flags=" + flags
                + ", trophy=" + trophy + ", closet=" + closet + ", body=" + body + ", availableAttributePoints="
                + availableAttributePoints + '}')
    }

    open fun getMaxWillpowerPossible(): Int {
        return Int.MAX_VALUE
    }

    fun levelUpIfPossible(c: Combat?): Boolean {
        var req: Int
        var dinged = false
        while (progression.canLevelUp()) {
            progression.levelUp()
            ding(c)
            dinged = true
        }
        return dinged
    }

    /**This character Makes preparations before a match starts. Called only by Match.Start()
     *
     */
    open fun matchPrep(m: Match?) {
        if (has(Trait.RemoteControl)) {
            val currentCount = inventory.getOrDefault(Item.RemoteControl, 0)
            gain(Item.RemoteControl, 2 - currentCount + get(Attribute.Science) / 10)
        }
    }

    /**Compares many variables between this character and the given character. Returns true only if they are all the same.
     *
     * @return
     *
     * Returns true only if all values are the same.
     */
    fun hasSameStats(character: Character): Boolean {
        if (trueName != character.trueName) {
            return false
        }
        if (type != character.type) {
            return false
        }
        if (!progression.hasSameStats(character.progression)) {
            return false
        }
        if (money != character.money) {
            return false
        }
        if (att != character.att) {
            return false
        }
        if (stamina.max() != character.stamina.max()) {
            return false
        }
        if (arousal.max() != character.arousal.max()) {
            return false
        }
        if (mojo.max() != character.mojo.max()) {
            return false
        }
        if (willpower.max() != character.willpower.max()) {
            return false
        }
        if (outfit != character.outfit) {
            return false
        }
        if (HashSet(traits) != HashSet(character.traits)) {
            return false
        }
        if (inventory != character.inventory) {
            return false
        }
        if (flags != character.flags) {
            return false
        }
        if (trophy != character.trophy) {
            return false
        }
        if (closet != character.closet) {
            return false
        }
        return if (body != character.body) {
            false
        } else availableAttributePoints == character.availableAttributePoints
    }

    fun flagStatus(flag: Stsflag) {
        statusFlags.add(flag)
    }

    fun unflagStatus(flag: Stsflag) {
        statusFlags.remove(flag)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) //If it has the same memory address - DSM
            return true
        if (o == null || javaClass != o.javaClass) //Becuse this is overridden at the character level this must be if it's a character... - DSM
            return false
        if (o === Global.noneCharacter() || this === Global.noneCharacter()) return false
        val character = o as Character
        return type == character.type && trueName == character.trueName
    }

    override fun hashCode(): Int {
        val result = type.hashCode()
        return result * 31 + trueName.hashCode()
    }

    fun getGrowth(): Growth {
        return growth
    }

    fun setGrowth(growth: Growth) {
        this.growth = growth
    }

    fun getSkills(): Collection<Skill> {
        return skills
    }

    /**Distributes points during levelup. Called by several classes.
     *
     * @param preferredAttributes
     * A list of preferred attributes.
     *
     *
     */
    fun distributePoints(preferredAttributes: List<PreferredAttribute>) {
        if (availableAttributePoints <= 0) {
            return
        }
        val avail = java.util.ArrayList<Attribute?>()
        var preferred: MutableList<PreferredAttribute> = preferredAttributes.toMutableList()
        for (a in att.keys) {
            if (Attribute.isTrainable(this, a) && (getPure(a) > 0 || Attribute.isBasic(this, a))) {
                avail.add(a)
            }
        }
        if (avail.size == 0) {
            avail.add(Attribute.Cunning)
            avail.add(Attribute.Power)
            avail.add(Attribute.Seduction)
        }
        var noPrefAdded = 2
        while (availableAttributePoints > 0) {
            var selected: Attribute? = null
            // remove all the attributes that isn't in avail
            preferred.removeAll { p: PreferredAttribute ->
                        val att = p.getPreferred(this)
                        att.isPresent && avail.contains(att.get())
                    }
            if (preferred.size > 0) {
                if (noPrefAdded > 1) {
                    noPrefAdded = 0
                    val pref = preferred.removeFirst()
                            .getPreferred(this)
                    if (pref.isPresent) {
                        selected = pref.get()
                    }
                } else {
                    noPrefAdded += 1
                }
            }
            if (selected == null) {
                selected = avail[Global.random(avail.size)]
            }
            mod(selected!!, 1)
            selected = null
            availableAttributePoints--
        }
    }

    open fun isPetOf(other: Character?): Boolean {
        return false
    }

    open fun isPet(): Boolean {
        return false
    }

    open fun getPetLimit(): Int {
        return if (has(Trait.congregation)) 2 else 1
    }

    fun setName(name: String) {
        trueName = name
    }

    fun hasStatusVariant(variant: String): Boolean {
        return status.any { s: nightgames.status.Status? -> s!!.variant == variant }
    }

    fun getPermanentStatuses(): List<nightgames.status.Status?> {
        return getStatusesWithFlag(Stsflag.permanent)
    }

    fun getStatusesWithFlag(flag: Stsflag): List<nightgames.status.Status> {
        return status.filter { status -> status.flags().contains(flag) }
    }

    val addictions: List<Addiction>
        get() = status.filterIsInstance<Addiction>()

    fun hasAddiction(type: AddictionType): Boolean {
        return addictions.any { addiction -> addiction.type == type }
    }

    fun getAddiction(type: AddictionType) = addictions.firstOrNull { addiction -> addiction.type == type }

    fun getStrongestAddiction() = addictions.maxByOrNull { it.severity }

    /**Constructor for a character - creates a character off of a name and level. Base Attributes start at 5 and other stats are derived from that.
     * @param name
     * The name of the character.
     * @param level
     * The level that the character starts at.
     */
    init {
        progression = Progression(level)
        growth = Growth()
        cloned = 0
        isCustomNPC = false
        body = Body(this)
        att = HashMap()
        cooldowns = HashMap()
        flags = HashMap()
        levelPlan = HashMap()
        att[Attribute.Power] = 5
        att[Attribute.Cunning] = 5
        att[Attribute.Seduction] = 5
        att[Attribute.Perception] = 5
        att[Attribute.Speed] = 5
        money = 0
        stamina = StaminaStat(22 + 3 * level)
        arousal = ArousalStat(90 + 10 * level)
        mojo = MojoStat(100)
        willpower = WillpowerStat(40)
        orgasmed = false
        pleasured = false
        outfit = Outfit()
        outfitPlan = java.util.ArrayList()
        closet = HashSet()
        skills = CopyOnWriteArrayList()
        status = java.util.ArrayList()
        statusFlags = EnumSet.noneOf(Stsflag::class.java)
        traits = CopyOnWriteArrayList()
        temporaryAddedTraits = HashMap()
        temporaryRemovedTraits = HashMap()
        removelist = HashSet()
        addlist = HashSet()
        //Can be changed into a flag that is stored in flags. -DSM
        inventory = HashMap()
        attractions = HashMap(2)
        affections = HashMap(2)
        location = Property(Area("", ErrorDescriptionModule()))
        // this.combatStats = new CombatStats();       //TODO: Reading, writing, cloning?
        progression.rank = 0
        Global.learnSkills(this)
    }

    fun addict(c: Combat?, type: AddictionType, cause: Character?, mag: Float) {
        val dbg = false
        if (!human() && !NPC_ADDICTABLES.contains(type)) {
            if (dbg) {
                System.out.printf("Skipping %s addiction on %s because it's not supported for NPCs", type.name, this.type)
            }
        }
        val addiction = getAddiction(type)
        if (addiction != null && addiction.cause == cause) {
            if (dbg) System.out.printf("Aggravating %s on player by %.3f\n", type.name, mag)
            addiction.aggravate(c, mag)
            if (dbg) System.out.printf("%s magnitude is now %.3f\n", addiction.type.name, addiction.magnitude)
        } else {
            if (dbg) System.out.printf("Creating initial %s on player with %.3f\n", type.name, mag)
            val addict = type.build(this, cause, mag)
            add(c, addict)
            addict.describeInitial()
        }
    }

    /**The reverse of Character.addict(). this alleviates the addiction by type.
     *
     * @param c
     * The combat required for this method.
     *
     * @param type
     * The type of addiction being processed.
     *
     * @param mag
     * The magnitude to decrease the addiction.
     *
     */
    fun unaddict(c: Combat?, type: AddictionType, mag: Float) {
        val dbg = false
        if (dbg) {
            System.out.printf("Alleviating %s on player by %.3f\n", type.name, mag)
        }
        val addiction = getAddiction(type) ?: return
        addiction.alleviate(c, mag)
        if (addiction.shouldRemove()) {
            if (dbg) System.out.printf("Removing %s from player", type.name)
            removeStatusImmediately(addiction)
        }
    }

    /**Removes the given status from this character. Used by Addiction removal. */
    fun removeStatusImmediately(status: nightgames.status.Status?) {
        this.status.remove(status)
    }

    /**Processes addiction
     *
     * FIXME: This method currently has no hits in the cal lhierarchy - is this method unused or deprecated? - DSM
     *
     * * @param c
     * The combat required for this method.
     *
     * @param type
     * The type of addiction being processed.
     *
     * @param cause
     * The cuase of this addiction.
     *
     * @param mag
     * The magnitude to increase the addiction.
     */
    fun addictCombat(type: AddictionType, cause: Character, mag: Float, c: Combat) {
        val dbg = false
        val addiction = getAddiction(type)
        if (addiction != null) {
            if (dbg) System.out.printf("Aggravating ${type.name} on player by $mag (Combat vs $trueName)\n")
            addiction.aggravateCombat(c, mag)
            if (dbg) System.out.printf("%s magnitude is now %.3f\n", addiction.type.name, addiction.magnitude)
        } else {
            if (dbg) System.out.printf("Creating initial ${type.name} on player with $mag (Combat vs $trueName)\n")
            val addict = type.build(this, cause, Addiction.LOW_THRESHOLD)
            addict.aggravateCombat(c, mag)
            add(c, addict)
        }
    }

    /**The reverse of Character.addict(). this alleviates the addiction by type. Called by many resolve() methods of skills.
     *
     * @param c
     * The combat required for this method.
     *
     * @param type
     * The type of addiction being processed.
     *
     * @param mag
     * The magnitude to decrease the addiction.
     *
     */
    fun unaddictCombat(type: AddictionType, cause: Character?, mag: Float, c: Combat?) {
        val dbg = false
        val addiction = getAddiction(type) ?: return
        if (dbg) {
            System.out.printf("Alleviating %s on player by %.3f (Combat vs %s)\n", type.name, mag, cause!!.trueName)
        }
        addiction.alleviateCombat(c, mag)
    }

    fun getAddictionSeverity(type: AddictionType): Addiction.Severity {
        return getAddiction(type)?.severity ?: Addiction.Severity.NONE
    }

    fun checkAddiction(): Boolean {
        return addictions.any { addiction -> addiction.atLeast(Addiction.Severity.LOW) }
    }

    fun checkAddiction(type: AddictionType) = getAddiction(type)?.isActive ?: false

    fun checkAddiction(type: AddictionType, cause: Character?): Boolean {
        return getAddiction(type)?.let { it.isActive && it.wasCausedBy(cause) } ?: false
    }

    open fun loserLiner(c: Combat?, target: Character?): String? {
        return Global.format("{self:SUBJECT-ACTION:try} seems dissatisfied with losing so badly.", this, target)
    }

    open fun victoryLiner(c: Combat?, target: Character?): String? {
        return Global.format("{self:SUBJECT-ACTION:try} smiles in satisfaction with their victory.", this, target)
    }

    open fun exercise(source: Exercise?): Int {
        val maximumStaminaForLevel = Configuration.getMaximumStaminaPossible(this)
        var gain = 1 + Global.random(2)
        if (has(Trait.fitnessNut)) {
            gain = gain + Global.random(2)
        }
        gain = Math.max(0,
                Math.min(maximumStaminaForLevel, stamina.max() + gain) - stamina.max())
        stamina.gain(gain.toFloat())
        return gain
    }

    open fun porn(source: Porn?): Int {
        val maximumArousalForLevel = Configuration.getMaximumArousalPossible(this)
        var gain = 1 + Global.random(2)
        if (has(Trait.expertGoogler)) {
            gain = gain + Global.random(2)
        }
        gain = Math.max(0, Math.min(maximumArousalForLevel, arousal.max() + gain) - arousal.max())
        arousal.gain(gain.toFloat())
        return gain
    }

    open fun chooseLocateTarget(potentialTargets: Map<Character?, Runnable?>?, noneOption: Runnable?, msg: String?) {
        throw UnsupportedOperationException("attempted to choose locate target")
    }

    open fun leaveAction(callback: Runnable?) {
        throw UnsupportedOperationException(String.format("attempted to leave locate action"))
    }

    open fun chooseShopOption(shop: Store, items: Collection<Loot?>?,
                              additionalChoices: List<String?>?) {
        throw UnsupportedOperationException(String.format("attempted to choose options in shop %s", shop.toString()))
    }

    // displayTexts and prices are expected to be 1:1
    open fun chooseBodyShopOption(shop: BodyShop, displayTexts: List<String?>?,
                                  prices: List<Int?>?, additionalChoices: List<String?>?) {
        throw UnsupportedOperationException(String.format("attempted to access options from %s", shop.toString()))
    }

    open fun nextCombat(c: Combat?) {
        // Can't be sure this isn't used at the moment
    }

    open fun sceneNext(source: Scene?) {
        // Can't be sure this isn't used at the moment
    }

    open fun chooseActivitySubchoices(activity: Activity?, choices: List<String?>?) {
        // Can't be sure this isn't used at the moment
    }

    fun getItemActions(): Set<Action> {
        val res = HashSet<Action>()
        val inv = getInventory()
        for (i in inv.keys) {
            if (inv[i]!! > 0) {
                when (i) {
                    Item.Beer -> {
                        res.add(UseBeer())
                        res.add(UseLubricant())
                        res.add(UseEnergyDrink())
                    }
                    Item.Lubricant -> {
                        res.add(UseLubricant())
                        res.add(UseEnergyDrink())
                    }
                    Item.EnergyDrink -> res.add(UseEnergyDrink())
                    else -> {}
                }
            }
        }
        return res
    }

    fun masterOrMistress(): String {
        return if (useFemalePronouns()) "mistress" else "master"
    }

    open fun getArmManager(): ArmManager? = null

    fun orgasm() {
        if (has(Trait.insatiable)) {
            Insatiable.renewArousal(arousal)
        } else {
            arousal.renew()
        }
    }

    fun getAttribute(a: Attribute?) = att[a]

    abstract val grammar: Person
    open fun notifyCombatStart(c: Combat?, opponent: Character?) {}
    open fun message(message: String?) {}
    open fun sendVictoryMessage(c: Combat?, flag: Result?) {}
    open fun sendDefeatMessage(c: Combat?, flag: Result?) {}
    open fun sendDrawMessage(c: Combat?, flag: Result?) {}
    abstract fun makeIntelligence(): Intelligence
    abstract fun makeDialog(): Dialog
    open fun notifyStanceImage(path: String?) {}

    companion object {
        private const val JSON_PROGRESSION = "progression"
        private const val jsCoreStats = "coreStats"
        private const val jsStamina = "stamina"
        private const val jsArousal = "arousal"
        private const val jsMojo = "mojo"
        private const val jsWillpower = "willpower"

        /**Processes addiction gain for this character.
         * @param c
         * The combat required for this method.
         *
         * @param type
         * The type of addiction being processed.
         *
         * @param cause
         * The cuase of this addiction.
         *
         * @param mag
         * The magnitude to increase the addiction.
         *
         */
        private val NPC_ADDICTABLES: Set<AddictionType> = EnumSet.of(AddictionType.CORRUPTION)
    }
}