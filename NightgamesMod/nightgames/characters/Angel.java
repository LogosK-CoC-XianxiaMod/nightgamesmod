package nightgames.characters;

import nightgames.characters.body.BreastsPart;
import nightgames.characters.body.BreastsPart.Size;
import nightgames.characters.body.FacePart;
import nightgames.characters.body.WingsPart;
import nightgames.characters.body.mods.AngelicWingsMod;
import nightgames.characters.body.mods.catcher.DivineMod;
import nightgames.characters.body.mods.pitcher.BlessedCockMod;
import nightgames.characters.corestats.ArousalStat;
import nightgames.characters.corestats.StaminaStat;
import nightgames.characters.corestats.WillpowerStat;
import nightgames.characters.custom.CharacterLine;
import nightgames.combat.Combat;
import nightgames.combat.CombatScene;
import nightgames.combat.CombatSceneChoice;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.grammar.Shorthand;
import nightgames.items.Item;
import nightgames.items.clothing.Clothing;
import nightgames.start.NpcConfiguration;
import org.jtwig.JtwigTemplate;

import java.util.Arrays;
import java.util.Optional;

public class Angel extends BasePersonality {
    private static final long serialVersionUID = -8169646189131720872L;
    private static final String ANGEL_SEX_FOCUS = "AngelSexFocus";
    private static final String ANGEL_NYMPHOMANIA_FOCUS = "AngelNymphomaniaFocus";
    private static final String ANGEL_WORSHIP_FOCUS = "AngelWorshipFocus";
    private static final String ANGEL_FOLLOWERS_FOCUS = "AngelFollowersFocus";

    private int dominance=0;

    public Angel() {
        this(null, null);
    }

    public Angel(NpcConfiguration charConfig, NpcConfiguration commonConfig) {
        super("Angel", true);
        character.plan = Plan.hunting;
        character.mood = Emotion.confident;
        setupCharacter(this,charConfig, commonConfig);
        constructLines();
    }

    public void applyBasicStats(Character self) {
        preferredCockMod = Optional.of(new BlessedCockMod());
        self.outfitPlan.add(Clothing.getByID("Tshirt"));
        self.outfitPlan.add(Clothing.getByID("bra"));
        self.outfitPlan.add(Clothing.getByID("thong"));
        self.outfitPlan.add(Clothing.getByID("miniskirt"));
        self.outfitPlan.add(Clothing.getByID("sandals"));
        self.change();
        self.modAttributeDontSaveData(Attribute.Seduction, 2);
        self.modAttributeDontSaveData(Attribute.Perception, 1);
        Global.gainSkills(self);

        self.stamina.setMax(60);
        self.arousal.setMax(110);

        self.trophy = Item.AngelTrophy;
        self.body.add(new BreastsPart(Size.DDCup));
        // very feminine face
        self.body.add(new FacePart(0.3, 4.2));
        self.initialGender = CharacterSex.female;
    }

    private static Growth newGrowth() {
        var stamina = new CoreStatGrowth<StaminaStat>(2, 1);
        var arousal = new CoreStatGrowth<ArousalStat>(9, 4);
        var willpower = new CoreStatGrowth<WillpowerStat>(1.0f, .25f);
        return new Growth(new CoreStatsGrowth(stamina, arousal, willpower));
    }

    @Override
    public void setGrowth() {
        character.setGrowth(newGrowth());
        
        this.addFirstFocusScene(); 
               
        this.addSecondFocusScene();  
        
       
        character.getGrowth().addTrait(0, Trait.undisciplined);
        character.getGrowth().addTrait(0, Trait.lickable);
        character.getGrowth().addTrait(3, Trait.responsive);
        character.getGrowth().addTrait(9, Trait.sexTraining1);
        // 12 - first choice 1
        character.getGrowth().addTrait(15, Trait.expertGoogler);
        character.getGrowth().addTrait(18, Trait.experienced);
        character.getGrowth().addTrait(20, Trait.skeptical);
        // 21 - second choice 1
        character.getGrowth().addTrait(24, Trait.tongueTraining1);
        // 27 - first choice 2
        // 30 - second choice 2
        character.getGrowth().addTrait(33, Trait.sexTraining2);
        character.getGrowth().addTrait(36, Trait.tongueTraining2);
        // 39 - first choice 3
        // 42 - second choice 3
        // 45 - second choice 4
        // 48 - second choice 5
        character.getGrowth().addTrait(51, Trait.desensitized);
        // 54 - first choice 4
        character.getGrowth().addTrait(57, Trait.desensitized2);
        // 60 - second choice 6
        preferredAttributes.add(
                        c -> c.get(Attribute.Divinity) < 50 ? Optional.of(Attribute.Divinity) : Optional.empty());
        preferredAttributes.add(c -> Optional.of(Attribute.Seduction));
        preferredAttributes.add(
                c -> (c.has(Trait.nymphomania)
                    && c.get(Attribute.Nymphomania) < (c.getProgression().getLevel() - 10) / 2)
                    ? Optional.of(Attribute.Nymphomania) : Optional.empty());
    }

    private void useSex() {
        Global.flag(ANGEL_SEX_FOCUS);
        character.getGrowth()
                 .addTrait(12, Trait.holecontrol);
        character.getGrowth()
                 .addTrait(20, Trait.zealinspiring);
        character.getGrowth()
                 .addTrait(25, Trait.powerfulhips);
        character.getGrowth()
                 .addTrait(39, Trait.insertion);
        character.getGrowth()
                 .addTrait(54, Trait.autonomousPussy);
    }

    private void useNymphomania() {
        Global.flag(ANGEL_NYMPHOMANIA_FOCUS);
        character.modAttributeDontSaveData(Attribute.Nymphomania, 1);
        character.getGrowth()
                 .addTrait(12, Trait.lastStand);
        character.getGrowth()
                 .addTrait(20, Trait.nymphomania);
        character.getGrowth()
                 .addTrait(25, Trait.RawSexuality);
        character.getGrowth()
                 .addTrait(39, Trait.erophage);
        character.getGrowth()
                 .addTrait(54, Trait.sexualDynamo);
    }

    private void useFollowers() {
        Global.flag(ANGEL_FOLLOWERS_FOCUS);
        character.getGrowth()
                 .addTrait(21, Trait.apostles);
        character.getGrowth()
                 .addTrait(30, Trait.leadership);
        if (Global.checkFlag(ANGEL_SEX_FOCUS)) {
            character.getGrowth()
                     .addTrait(42, Trait.inspirational);
        }
        if (Global.checkFlag(ANGEL_NYMPHOMANIA_FOCUS)) {
            character.getGrowth()
                     .addTrait(42, Trait.showmanship);
        }
        character.getGrowth()
                 .addTrait(45, Trait.tactician);
        character.getGrowth()
                 .addTrait(48, Trait.devoteeFervor);
        character.getGrowth()
                 .addTrait(60, Trait.congregation);
    }

    private void useWorship() {
        Global.flag(ANGEL_WORSHIP_FOCUS);
        character.getGrowth()
                 .addTrait(21, Trait.objectOfWorship);
        character.getGrowth()
                 .addTrait(30, Trait.magicEyeArousal);
        character.getGrowth()
                 .addTrait(42, Trait.sacrosanct);
        character.getGrowth()
                 .addTrait(45, Trait.genuflection);
        if (Global.checkFlag(ANGEL_SEX_FOCUS)) {
            character.getGrowth()
                     .addTrait(48, Trait.piety);
        }
        if (Global.checkFlag(ANGEL_NYMPHOMANIA_FOCUS)) {
            character.getGrowth()
                     .addTrait(48, Trait.mandateOfHeaven);
        }
        character.getGrowth()
                 .addTrait(60, Trait.revered);
        
        character.getGrowth().addTrait(80, Trait.divineAssumption);
    }

    @Override
    public void rest(int time) {
        if (character.getProgression().getRank() >= 1) {
            if (!character.has(Trait.demigoddess)
                && (Global.checkFlag(ANGEL_SEX_FOCUS) || Global.checkFlag(ANGEL_NYMPHOMANIA_FOCUS))) {
                advance();
            }
        }
        super.rest(time);
        if (!(character.has(Item.Dildo) || character.has(Item.Dildo2)) && character.money >= 250) {
            character.gain(Item.Dildo);
            character.money -= 250;
        }
        if (!(character.has(Item.Onahole) || character.has(Item.Onahole2)) && character.money >= 300) {
            character.gain(Item.Onahole);
            character.money -= 300;
        }
        if (!character.has(Item.Onahole2) && character.has(Item.Onahole) && character.money >= 300) {
            character.remove(Item.Onahole);
            character.gain(Item.Onahole2);
            character.money -= 300;
        }
        if (!(character.has(Item.Strapon) || character.has(Item.Strapon2)) && character.money >= 500) {
            character.gain(Item.Strapon);
            character.money -= 500;
        }
        if (!character.has(Item.Strapon2) && character.has(Item.Strapon) && character.money >= 500) {
            character.remove(Item.Strapon);
            character.gain(Item.Strapon2);
            character.money -= 500;
        }
        buyUpTo(Item.PriapusDraft, 3);
        if (character.getProgression().getRank() >= 1) {
            if (!character.has(Trait.lacedjuices) && character.money >= 1000) {
                character.money -= 1000;
                character.getGrowth()
                         .addTrait(Math.min(20, character.getProgression().getLevel()), Trait.lacedjuices);
            }
            if (character.money > 0) {
                Global.getDay()
                      .visit("Body Shop", character, Global.random(character.money));
            }
            if (character.money > 0) {
                Global.getDay()
                      .visit("Black Market", character, Global.random(character.money));
            }
            if (character.money > 0) {
                Global.getDay()
                      .visit("Workshop", character, Global.random(character.money));
            }
        }
        if (character.money > 0) {
            Global.getDay()
                  .visit("XXX Store", character, Global.random(character.money));
        }
        if (character.money > 0) {
            Global.getDay()
                  .visit("Black Market", character, Global.random(character.money));
        }
        if (character.money > 0) {
            Global.getDay()
                  .visit("Bookstore", character, Global.random(character.money));
        }
        if (character.money > 0) {
            Global.getDay()
                  .visit("Hardware Store", character, Global.random(character.money));
        }
        Decider.visit(character);
        int r;
        for (int i = 0; i < time; i++) {
            r = Global.random(8);
            if (r == 1) {
                Global.getDay()
                      .visit("Exercise", this.character, 0);
            } else if (r == 0) {
                Global.getDay()
                      .visit("Browse Porn Sites", this.character, 0);
            }
        }
    }

    private void constructLines() {
        addLine(CharacterLine.BB_LINER, (c, self,
                        other) -> "Angel seems to enjoy your anguish in a way that makes you more than a little nervous. <i>\"That's a great look for you, I'd like to see it more often.\"</i>");
        addLine(CharacterLine.NAKED_LINER, (c, self,
                        other) -> "Angel gives you a haughty look, practically showing off her body. <i>\"I can't blame you for wanting to see me naked, everyone does.\"</i>");
        addLine(CharacterLine.STUNNED_LINER, (c, self,
                        other) -> "Angel groans on the floor. <i>\"You really are a beast. It takes a gentle touch to please a lady.\"</i>");
        addLine(CharacterLine.TAUNT_LINER, (c, self, other) -> {
            if (other.hasDick()) {
                return "Angel pushes the head of your dick with her finger and watches it spring back into place. <i>\"You obviously can't help yourself. If only you were a little bigger, we could have a lot of fun.\"</i>";
            } else {
                return "Angel spreads your cleft with her fingers, <i>\"So wet. Does my little slut want to come? Can't help yourself?\"</i>";
            }
        });
        addLine(CharacterLine.CHALLENGE, (c, self,
                        other) -> "{self:SUBJECT} licks {self:possessive} lips and stalks {other:name-do} like a predator.");
        addLine(CharacterLine.TEMPT_LINER, (c, self,
                        other) -> "Angel looks at you with a grin, <i>\"You're almost drooling. Is staring at my body that much fun? If you want me that much, why don't you just sit there and let me make you feel good.\"</i>");
        addLine(CharacterLine.ORGASM_LINER, (c, self, other) -> {
            final String finalLines[] = {"<i>\"Fill me! Fill me with everything you have!\"</i>",
                            "Angel pants with a flushed face and lidded eyes, <i>\"I'll pay you back... I wont let you escape!\"</i>",
                            "<i>\"Fuck me! fuck me! fuck me! fuck!\"</i>",};
            switch (self.orgasms) {
                case 0:
                    return "<i>\"Mmm, maybe you do have promise. Care to try that again?\"</i>";
                case 1:
                    return "<i>\"Oh fuck, I didn't think you had it in you. More. Now.\"</i>";
                case 2:
                    return "Angel massages her own cunt as she cums, <i>\"Not enough... Not nearly enough!\"<i>";
                default:
                    return Global.pickRandom(Arrays.asList(finalLines))
                                 .get();
            }
        });
        addLine(CharacterLine.MAKE_ORGASM_LINER, (c, self, other) -> {
            final String finalLines[] = {"<i>\"You're making this too easy. Put your back into it!\"</i>",
                            "<i>\"I'm not satisfied. No, not at all. So keep going.\"</i>",
                            "<i>\"No resting. I won't allow it.\"</i>",};
            switch (other.orgasms) {
                case 0:
                    return "Angel stares you in the eye as your consciousness returns from the precipice <i>\"Once isn't enough. I need more. You can do that for me, right?\"</i>";
                case 1:
                    return "<i>\"Surely you won't let it end with just two times right?\"</i>";
                case 2:
                    return "<i>\"Enough? This is no where near enough. Stop panting and keep going.\"</i>";
                default:
                    return Global.pickRandom(Arrays.asList(finalLines))
                                 .get();
            }
        });
        
        /*  //If Angel is a Corrupted demigoddess  
             if (character.has(Trait.demigoddess) && character.isDemonic()) {
          
             //if Angel is a demigoddess
            } else if (other.getLevel() >= self.getLevel()) {
            
            //When she's not any of those
             } else {
             
             }
         */
        
        addLine(CharacterLine.NIGHT_LINER,
                        (c, self, other) -> "As you start to head back after the match, Angel grabs your hand and drags you in the other direction. <i>\"You're officially kidnapped, because I haven't had "
                                        + "enough sex yet tonight.\"</i> That makes sense... kinda? You did just finish three hours of intense sex-fighting. If she wants too much more than that, you're "
                                        + "both going to end up pretty sleep deprived. Angel looks like she's struggling to put her thoughts into words. <i>\"I had enough sex in general, but I want some "
                                        + "more time having you all to myself.\"</i> That's quite flattering coming from her, but why you specifically? Angel is openly bisexual, she could just as easily "
                                        + "take one of the other girls back with her. She looks back at you and blushes noticeably. <i>\"It's better with you, and not just because you have a cock. It is "
                                        + "a pretty good fit though. I don't know. It doesn't matter. I'm kidnapping you, so we're going to go back to my room, have sex, and you're going to stay the night "
                                        + "in case I want more sex in the morning.\"</i> You follow without protest. <br/>You lose a lot of sleep, but you don't regret it.");

        description = JtwigTemplate.classpathTemplate("angel/describe.twig");
            
        //This is an example of a new kind of Line - a Line to be said if this character wins the entire night. 
        addLine(CharacterLine.VICTORY_LINER, (c, self, other) -> {
            if (character.has(Trait.demigoddess) && character.isDemonic()) {
                return "[PLACEHOLDER] Angel (Corrupted Goddess) says, \"</i>That's right! Everyone on their knees!\"</i>";
            } else if (character.has(Trait.demigoddess)) {
                return "[PLACEHOLDER] Angel (Goddess) says, \"</i>How's that for some good religion? I could go for some more.\"</i>";
            } else {
                return "[PLACEHOLDER] Angel says, \"</i>MMm! That was nice. I could go for some more.\"</i>";
            }
        });
        
          //This is an example of a new kind of Line - a Line to be said if this character gets 0 victories. 
        addLine(CharacterLine.LOSER_LINER, (c, self, other) -> {
            if (character.has(Trait.demigoddess) && character.isDemonic()) {
                return "[PLACEHOLDER] Angel (Corrupted Goddess) says, \"</i>Wow...I didn't win ANYTHING?\"</i>";
            } else if (character.has(Trait.demigoddess)) {
                return "[PLACEHOLDER] Angel (Goddess) says, \"</i>Wow...I didn't win ANYTHING?\"</i>";
            } else {
                return "[PLACEHOLDER] Angel says, \"</i>Wow...I didn't win ANYTHING?\"</i>";
            }
        });
    }

    @Override
    public String victory(Combat c, Result flag) {
        character.arousal.renew();
        Character opponent = c.getOpponentCharacter(character);

        String message = "";

        if (c.getStance().anallyPenetrated(c, opponent)
            && !opponent.has(Trait.buttslut)) {
            dominance += 1;

            message = "Angel leans over you as she grinds her hips against yours. <i>\"You're going to come for me, aren't you?\"</i> she purrs into your ear. You shake your head; "
                            + "no way could you live it down if you came while you had something in your ass. Angel frowns and gives your ass a firm slap. <i>\"No reach around for you "
                            + "then,\"</i> she snaps. <i>\"We'll just do this the old fashioned way.\"</i> She renews her assault on your poor ass and you feel your will slipping. Another solid slap "
                            + "to your ass sends you into a shuddering orgasm. Angel's triumphant laughter rings in your head as the shame makes you flush bright red.<br/><br/>Pulling her "
                            + (character.hasDick() ? character.body.getRandomCock()
                                                                   .describe(character)
                                            : "strapon")
                            + " from your ass with a wet slurp Angel flips you over"
                            + (!character.hasDick() ? " and removes the strapon." : ". ")
                            + "She then squats down and lines your cock up with her now soaked pussy, <i>\"Do "
                            + "a good enough good job and I might not tell my friends how you came like a whore while I fucked your ass.\"</i> She gloats with a smug grin on her face. "
                            + "Appalled at the idea that she might share that information with anyone, you strengthen your resolve to fuck the woman above you.<br/><br/>Several minutes later, "
                            + "you are breathing hard. Angel sits not far from you, face flush with pleasure. You smile internally as you sit, trying to catch your breath. No way "
                            + "she could have been disappointed with that performance.  You can only gape as you look up to see Angel is gone along with your clothes. You sigh as you "
                            + "stand and ready yourself to move on. You wouldn't put past Angel to tell her girlfriends regardless of how well you performed, you just hope that's as "
                            + "far as that information goes.";
        } else if (c.getStance().inserted(character)
            && character.willpower.percent() < 0.5) {
            dominance += 1;

            message = "Angel stares you in the eye, while expertly thrusting in and out of your slobbering pussy. Your needy cunt quivers as she leans close and gives you a long steamy kiss, "
                            + "tongue and all. You try to get away from her, but she holds you down and merciless pounds away at your overused pussy. You can tell she is turned on as well, but "
                            + "it'll do you no good, as you're already feeling yourself slip over the edge. "
                            + "<br/><br/>Finally it becomes too much, and you cum hard. You wrap your arms and legs unconsciously cling to Angel's body and you seek out "
                            + "a kiss from her. Angel takes note of your convulsing body, and smirks, <i>\"I think you need some more training. I could make "
                            + "anyone cum instantly while they're in me.\"</i> After a small pause, Angel grins devilishly and resumes pumping in and out of your pussy "
                            + "in long leisurely strokes. <i>\"Hmm in fact, why don't I train you a bit? You know what they say, practice makes perfect!\" "
                            + "You groan in frustration as your oversensitive cunt receives her cock again. "
                            + "<br/><br/>This could be a long night.";
        } else if (c.getStance().inserted(character) && character.willpower.percent()>=0.5 && dominance>=10) {
            dominance+=1;

            minDominance=10;

            message = "Angel holds you in place with her hands on your breasts, rubbing the sides and pinching and flicking your nipples.  She's no longer holding you down and "
                            + "pounding your pussy mercilessly- she stopped doing that when you started moaning and grinding your hips back against her. Now she's lazily rocking "
                            + "back and forth, watching your response as she thrusts from different angles and with different speeds. She's clearly no longer rushing to make you "
                            + "cum before she does- at this point it's obvious that that's guaranteed to happen.<p>It seems that Angel's decided she's found the best way to fuck "
                            + "your pussy- As she calmly pounds you along this new angle, you cum on the second stroke- and she just keeps thrusting. By the time she finally "
                            + "brings herself to her first orgasm, you've cum three more times and your cunt is so sensitive that you're convinced you can feel every vein on her "
                            + "cock. Angel reaches up and grabs your flushed, panting head and turns it so you're looking right at her grinning face.<p>Angel gives an obviously "
                            + "fake sigh, and shakes her head in mock dissapointment. \"Honestly, "+opponent.getName()+", after all those times I helped you train, giving you "
                            + "practice pleasing my cock with your pussy, you don't manage to make me cum until you've cum so many times I've lost count?\"<br>Shaking her head "
                            + "again, Angel pulls out of you, eliciting a fresh groan from your lips. She riffles through her things, pulling out two dildos and several lengths "
                            + "of some kind of fabric. \"It seems that you're too much of a slut for my nice, easy training to work properly, so we're going to have to do some "
                            + "more involved work to turn you into a proper cock-milker.\" Normally when you're beaten, you go along with whatever the victor decides to do with "
                            + "you out of a combination of arousal, a sense of fair play, and exhaustion, but this seems like a bit more than you're confortable with. You open "
                            + "your mouth to protest, but Angel takes it as an invitation to shove one of the dildos she's holding in.<p> The dildo has a musky taste that at this "
                            + "point you're familiar wih as belonging to Angel's pussy juices. The faux-cock is not overly thick and fits easily into your mouth- it's around the "
                            + "girth of Angel's futanari dick- but it's alarmingly long, with the part outside of your lips being nearly fifteen inches long. Thankfully, Angel "
                            + "only shoved perhaps four inches of it into your mouth, but you start feeling a lot less thankful as you notice that one of the bands Angel was holding "
                            + "was actually attached to this dildo, and Angel wraps it around your head, fastening it somehow in back. The band is quite tight, though not to the "
                            + "point of being painful, and is very inelastic. When you inhale, you hear a faint whistling noice, and notice that you can breathe through the "
                            + "dildo- it must have holes running through it to make it easier to deepthroat. That would be somewhat reassuring if it didn't imply that Angel "
                            + "intends to have you deepthroat it.<p>While you're preoccupied with this oral invader, Angel quickly wraps bands around your elbows and knees, forcing "
                            + "you into a helpless, impossible-to-escape-from position. She finishes up by clipping a rod between the wraps on your knees, forcing you to keep your "
                            + "legs spread in a way that opens your pussy up to anyone who might wish to use it to get off in. She rolls you over until you're resting on your knees "
                            + "and elbows, ass in the air, horribly exposed.<p>Angel pats you on the ass and says \"Alright, this is how your training in pleasuring my cock is "
                            + "going to go.\" She drapes herself over your back, her breasts over your shoulders and her cock rubbing across the dripping entrance to your cunt. "
                            + "\"When I rub anything against the opening to your cunt like I'm doing now, you're going to push back into it.\"<br>You don't move- you have no "
                            + "intention of acting like the slut she seems to want to make you. \"Oh, I forgot to mention something,\" Angel says, and you can almost hear the "
                            + "devilish smile in her voice. Angel reaches up to where the dildo invading your mouth meets the band that holds it in place, and flips some switch "
                            + "you can't see. Immediately, the dildo in your mouth begins slowly pumping in and out of the orfice, moving perhaps two inches between the ends of "
                            + "its motion. At its deepest point, it's not pushing uncomfortably against the back of your throat, and if it were pushing any further you'd have to "
                            + "start deepthroating it. You quickly realize that the motor and the band are too strong for you to make much progress trying to resist the facefuck, "
                            + "and resign yourself to just letting it happen. Angel indulgently pats your cheek and continues, \"Every time you don't do something you're supposed "
                            + "to, you get some spankings, and I press this button over here,\" Angel caresses something near the switch that started the device pumping into your "
                            + "mouth, \"which makes it move a quarter inch deeper.\" That doesn't sound like much, but your eyes widen as you realize that the massive length of "
                            + "the dildo means that it could probably hit your vocal cords, and the nearly fifty deepenings that would be required to get it all the way in says "
                            + "alarming things about how long she intends to keep you here.<p>\"We'll keep doing this until you manage to make me cum without making a mistake, so "
                            + "we could be here quite a while if you don't put everything you have into making sure you do all the slutty cock-pleasing things you're supposed to.\""
                            + "<br>Angel rubs her cock's shaft over your entrance again. \"Now, what were you supposed to do, cock slut?\"<p>Angel seems to have done a lot to "
                            + "prepare for this, and you suspect she intends to do exactly as she says. There's no way you're getting out of this without her letting you out, so "
                            + "you decide to go along with her demands as best you can.<br>You push back against her dick, trying to rub your pussy against it. Angel pats you on "
                            + "the ass again and says\"Good, just like that cock slut. Now, let me tell you what a good cock slut is supposed to do. Make sure you listen closely, "
                            + "I'm not going to repeat this.\"<br>\"First, a clock slut grinds back against anything touching its pussy. Second, a cock slut deepthroats and licks "
                            + "anything put in its mouth, and always sucks out and swallows any liquids it can get. The cock slut should make sure to lick the cock in its mouth right now, it's a special dildo and "
                            + "I'll know whether you do.\"<p>You decide discretion is the better part of valour- or in this case, submission is the better part of getting out of "
                            + "bondage- and immediately start licking the dildo fucking your face.<p>Angel pats you on the ass again- either able to tell you started licking, or "
                            + "predicting it- and continues \"Good cock slut. Now third, whenever a cock slut has a cock in its cock hole, it clenches to try to keep the cock "
                            + "inside on every thrust, and unclenches whenever anything tries to get inside.\"<p>INCOMPLETE";

        } else if (c.getStance().inserted(opponent)) {

            message = "Angel rides your cock passionately, pushing you inevitably closer to ejaculation. Her hot pussy is wrapped around your shaft like... well, exactly "
                            + "what it is. More importantly, she's a master with her hip movements and you've held out against her as long as you can. You can only hope her own orgasm is equally "
                            + "imminent. <i>\"Not even close,\"</i> She practically growls. <i>\"Don't give up now.\"</i> That's an impossible command. How can she expect you not to cum when "
                            + "her slick love canal is milking your dick so expertly? As the last of your restraint crumbles, you let out a groan and shoot a thick load of semen "
                            + "into her depths. <br/><br/>You lie on the floor panting as Angel looks down at you, somehow annoyed despite her victory. <i>\"Is that the best you can do? "
                            + "You know it's rude to finish before your lover.\"</i> She starts to lick and suck on her finger, sensually. <i>\"Don't think you can get off on your own and the "
                            + "sex is done just like that. I never let a man go until I'm satisfied.\"</i> You're quite willing to try to satisfy her in a variety of ways, but more "
                            + "fucking is a physical impossibility at this point. Your spent penis has completely wilted by now, and it'll be a little while before there's any possibility "
                            + "of it recovering. Angel gives you a pitiless smile and reaches behind her. <i>\"Don't worry. I know a good trick.\"</i><br/><br/>Whoa! You jerk in surprise as you feel "
                            + "her spit-coated finger probing at your anus. <i>\"Don't complain,\"</i> She says, sliding the digit into your ass. <i>\"It's your own fault for being such a quick "
                            + "shot.\"</i> As she moves her finger around, it creates an indescrible sensation. You dick starts to react immediately and returns to full mast faster than you "
                            + "ever would have imagined. Angel wastes no time impaling herself on your newly recovered member and rides you with renewed vigor. Fortunately she removes "
                            + "the invading finger from your anus so you can focus on the pleasure of being back in her wonderful pussy. <br/><br/>She grinds against you, clearly turned on and "
                            + "enjoying being filled again. She moans passionately and her vaginal walls rub and squeeze your cock. You move your hips to match Angel's movements and "
                            + "her voice jumps in pitch. She's obviously enjoying your efforts much more this time, but she's so good too. You've just recently cum, but she's riding "
                            + "through your endurance at an alarming rate. If you end up cumming again before she finishes, you're going to get the finger treatment again or worse. "
                            + "Fortunately, you don't have to worry about that. Angel throws back her head and practically screams out her orgasm. Her love canal squeezes tightly, milking "
                            + "out your second ejaculation. <br/><br/>Angel quickly recovers, standing up as a double load of cum leaks out between her thighs. <i>\"That'll do... for now.\"</i>";
        } else {
            message = "It's too much. You can't focus on the fight with the wonderful sensations Angel is giving you. She smiles triumphantly and mercilessly teases your "
                            + "twitching dick. Your orgasm is imminent, but you concentrate on holding it back as long as you can, determined not to give up until the end. Angel's "
                            + "expression gradually changes to one of impatience. <i>\"Just cum already!\"</i> She slaps your dick and the shock breaks your concentration. Your pent-up "
                            + "ejaculation bursts forth and covers her hands. <br/><br/>Without giving you a chance to recover, Angel pushes you on your back and positions her soaking "
                            + "pussy over your face. <i>\"Show me you're good for more than cumming on command.\"</i> She grinds against your mouth as you eat her out. She reaches behind her "
                            + "and roughly grabs your balls, encouraging you to focus more on pleasing her. Soon her writhing grows more passionate and her moans express her building "
                            + "pleasure. She rewards your efforts by moving her hand to your dick, which is already starting to harden again. She jerks you off, using your previous climax "
                            + "for lubricant. The growing volume of Angel's cries reveal that she's close to the end, so you focus on licking and sucking her clit, quickly bringing her to "
                            + "a loud climax. Your own peak isn't far behind and soon you shoot another jet of cum into the air.\n\nAngel licks her semen covered hands clean and walks away "
                            + "with your clothes, having seemingly forgotten about you.";
        }
        return message;
    }

    @Override
    public String defeat(Combat c, Result flag) {
        Character opponent = c.getOpponentCharacter(character);

        if (c.getStance().vaginallyPenetrated(c, character)) {
            dominance=Math.max(dominance-1, minDominance);

            return "You thrust your cock continously into Angel's dripping pussy. Her hot insides feel amazing, but you're sure you have enough of an advantage to risk "
                            + "it. She lets out breathy moans in time to your thrusts and her arms are trembling too much to hold herself up. She's clearly about to cum, you just "
                            + "need to push her over the edge. You maul her soft, heavy boobs and suck on her neck. Angel closes her eyes tightly and whimpers in pleasure. <br/><br/>You keep "
                            + "going, sure that your victory is near, but after awhile there's no change in her reactions. How has she not cum yet? She's obviously loving your efforts, "
                            + "but you can't seem to finish her off. Worse yet, if you keep going at this pace, your own control is going to give out. You'll have to pull out so you can "
                            + "switch to your fingers and tongue. It'd be way more satisfying to win by fucking her, but right now you just have to focus on winning at all. When you try "
                            + "to pull out, Angel's legs wrap around you and keep you from escaping. Her heels jab you in the butt, forcing you to thrust back inside and you feel her pussy "
                            + "squeeze your cock tightly. <br/><br/>Oh God, she's actually going to make you cum while you're on top of her. You were overconfident in your dominant position, you "
                            + "underestimated Angel's remarkable staying power, and now you've lost. Despite your desperate attempts to hang on, you're overwhelmed by pleasure and cum "
                            + "inside her tight womanhood. You slump down on top of her as you both catch your breath. Pretty soon Angel is fully recovered and back on her feet, but you "
                            + "continue to lie on the floor, too despirited to move. Angel gives you a sharp prod with her foot. <i>\"How long are you going to lay there? You only came once. "
                            + "I had a continuous orgasm for at least two minutes and that's way more exhausting. It's been a long time since anyone's made me do that.\"</i> Wait, what? You'd "
                            + "never have guessed that she came if she hadn't said anything. <i>\"Just because you managed to beat me this time doesn't mean you can suddenly start acting "
                            + "lazy. If you let your guard down, I'm going to turn you into my own personal toy.\"</i> At that, she walks away naked.";
        }
        if (opponent.hasDick()) {
            return "Angel trembles and moans as you guide her closer and closer to orgasm. You pump two fingers in and out of her pussy and lick her sensitive nether lips. "
                            + "Her swollen clit peeks out from under its hood and you pinch it gently between your teeth. Angel instantly screams in pleasure and arches her back. A "
                            + "flood of feminine juice sprays you as she loses control of her body.<br/><br/>It takes her a little while to catch her breath. She quickly pushes you on your "
                            + "back and begins blowing you, never once meeting your eyes. What you can see of her face and ears is completely red. If you didn't know better, you'd "
                            + "say that she's embarrassed about the one-sided orgasm you gave her earlier. You don't have much attention to devote to it though, Angel is a very good "
                            + "cock-sucker. Her tongue finds all your most sensitive areas and soon you're filling her mouth with your seed.<br/><br/>Angel swallows your load and happily "
                            + "licks the stray drops from her lips. <i>\"Did you enjoy that?\"</i> She asks, looking a lot more composed. <i>\"You weren't bad either.\"</i>";
        } else {
            return "Angel trembles and moans as you guide her closer and closer to orgasm. You pump two fingers in and out of her pussy and lick her sensitive nether lips. "
                            + "Her swollen clit peeks out from under its hood and you pinch it gently between your teeth. Angel instantly screams in pleasure and arches her back. A "
                            + "flood of feminine juice sprays you as she loses control of her body.<br/><br/>It takes her a little while to catch her breath. She quickly pushes you on your "
                            + "back and begins licking you, never once meeting your eyes. What you can see of her face and ears is completely red. If you didn't know better, you'd "
                            + "say that she's embarrassed about the one-sided orgasm you gave her earlier. You don't have much attention to devote to it though, Angel is a very good "
                            + "pussy-licker. Her tongue finds all your most sensitive areas and soon you are shuddering with pleasure.<br/><br/>Angel clamps down on your clit and gently "
                            + "keeps you going until you moan \"enough\". <i>\"Did you enjoy that?\"</i> She asks, looking a lot more composed. <i>\"You weren't bad either.\"</i>";
        }
    }

    @Override
    public String draw(Combat c, Result flag) {
        if (flag == Result.intercourse) {
            return "Angel pins you on your back, riding you with passion. You're close to the edge, but she's too far gone to take advantage of it. She's fucking you "
                            + "for her own pleasure rather than trying to win. Just as you feel your climax hit, Angel cries out in ecstasy and her pussy tightens to milk your "
                            + "dick dry. <br/><br/>Angel stays on top of you as you both recover, and as your wilting penis starts to slip out of her, her vagina squeezes again to hang "
                            + "onto it. <i>\"I hope you're not finished yet,\"</i> she whispers sultrily. <i>\"I won't be satisfied with just one time.\"</i> You're already starting to harden "
                            + "again inside her. She pushes her perky breasts into your face and lets you lick and suck her nipples. By the time you're completely erect, she's "
                            + "acting noticeably pleasure drunk again. She grinds her hips against yours and soon she reaches her second orgasm. She only slows down for a moment, "
                            + "riding you as quickly as when she started. Your next climax builds faster than hers. She grabs your balls, pinching and squeezing to delay your "
                            + "ejaculation each time you get close. As she nears her peak, she lets you go. You cum inside her again, setting off her third screaming orgasm. <br/><br/>"
                            + "By the time Angel's finally satisfied, you're exhausted, but very content.";
        }
        return "You and Angel lie on the floor in 69 position, desperately pleasuring each other. Angel is extremely good at giving blowjobs and each flick of her tongue "
                        + "tests your self-control. Fortunately, she's quite receptive to your oral minstrations. Her pussy trembles as you polish her clit with your tongue. For a "
                        + "moment, you think you have the upper hand, but then her tongue finds a particularly sensitive bit of flesh under your cockhead, and her hand fondles your "
                        + "balls. Your hips jerk involuntarily as you cum in her mouth. Fortunately, a flood of Angel's love juice hits your face, indicating she orgasmed at the same "
                        + "time.<br/><br/>You wipe the juice from your mouth, but Angel doesn't give you any time to rest. She continues licking and sucking your cock in the aftermath of your "
                        + "ejaculation. Your penis is extremely sensitive right now, but she keeps it from softening. She gives your balls a light squeeze, which you interpret as a "
                        + "demand to keep eating her out. You shove your tongue into her pussy and feel her tremble as she lets out a stifled moan. Angel redoubles her efforts and blows "
                        + "you even more intensely. You retaliate by focusing on your tongue work, exploring her labia and clit to find her weaknesses.<br/><br/>You and Angel continue servicing "
                        + "each other until you both cum again. She still shows no sign of stopping and continues sucking your painfully overstimulated dick. You were sensitive after the "
                        + "first time you ejaculated, but now it almost feels like you're being shocked. This is practically torture. "
                        + "You pull away from her slit and beg her to stop.<br/><br/>Angel gives you a few more very intentional licks before releasing you, as if to make a point. She sits on "
                        + "your torso and looks down at you with a superior smirk. <i>\"Since we came at the same time, I was worried you might get the crazy idea that you're a match "
                        + "for me. I figured I should prove to you which of us has the most staying power.\"</i> She strokes your hair with a surprising amount of affection. <i>\"Don't "
                        + "worry if you can't keep up. As long as you keep making me cum, I'll let you be my pet.\"</i>";
    }

    @Override
    public boolean fightFlight(Character opponent) {
        return !character.mostlyNude() || opponent.mostlyNude();
    }

    @Override
    public boolean attack(Character opponent) {
        return true;
    }

    @Override
    public String victory3p(Combat c, Character target, Character assist) {
        if (target.human()) {
            return "Angel looks over your helpless body like a predator ready to feast. She kneels between your legs and teasingly licks your erection. She circles her "
                            + "tongue around the head, coating your penis thoroughly with saliva. When she's satisfied that it is sufficiently lubricated and twitching with need, "
                            + "she squeezes her ample breasts around your shaft. Even before she moves, the soft warmth surrounding you is almost enough to make you cum. When she "
                            + "does start moving, it's like heaven. It takes all of your willpower to hold back your climax against the sensation of her wonderful bust rubbing against "
                            + "your slick dick. When her tongue attacks your glans, poking out of her cleavage, it pushes you past the limit. You erupt like a fountain into her face, "
                            + "while she tries to catch as much of your seed in her mouth as she can.";
        }
        if (target.hasDick()) {
            return String.format(
                            "You present %s's naked, helpless form to Angel's tender "
                                            + "minstrations. Angel licks her lips and begins licking and stroking %s's"
                                            + " body. She's hitting all the right spots, because soon %s is squirming "
                                            + "and moaning in pleasure, and Angel hasn't even touched %s cock yet."
                                            + " Angel meets your eyes to make sure you're paying attention and slowly"
                                            + " moves her fingers down the front of %s's body. You can't see her hands"
                                            + " from this position, but you know when she reaches her target, because "
                                            + "%s immediately jumps as if %s's been shocked. Soon it takes all of your"
                                            + " energy to control %s who is violently shaking in the throes of orgasm."
                                            + " You ease %s to the floor as %s goes completely limp, while Angel licks"
                                            + " the cum from her fingers.",
                            target.getName(), target.getName(), target.getName(), target.possessiveAdjective(),
                            target.getName(), target.getName(), target.pronoun(), target.getName(),
                            target.objectPronoun(), target.pronoun());
        }
        return "You present " + target.getName()
                        + "'s naked, helpless form to Angel's tender minstrations. Angel licks her lips and begins licking and stroking "
                        + target.getName() + "'s body. She's " + "hitting all the right spots, because soon "
                        + target.getName()
                        + " is squirming and moaning in pleasure, and Angel hasn't even touched her pussy yet. "
                        + "Angel meets your eyes to focus your attention and slowly moves her fingers down the front of "
                        + target.getName() + "'s body. You can't see her hands from "
                        + "this position, but you know when she reaches her target, because " + target.getName()
                        + " immediately jumps as if she's been shocked. Soon it takes all of "
                        + "your energy to control " + target.getName()
                        + " who is violently shaking in the throes of orgasm. You ease her to the floor as she goes completely limp, "
                        + "while Angel licks the juice from her fingers.";

    }

    @Override
    public String intervene3p(Combat c, Character target, Character assist) {
        if (target.human()) {
            return "You manage to overwhelm " + assist.getName()
                            + " and bring her to the floor. You're able to grab both her arms and pin her helplessly beneath you. "
                            + "Before you can take advantage of your position, pain explodes below your waist. "
                            + assist.getName() + " shouldn't have been able to reach your groin "
                            + "from her position, but you're in too much pain to think about it. You are still lucid enough to feel large, perky breasts press against your back "
                            + "and a soft whisper in your ear. <i>\"Surprise, lover.\"</i> The voice is unmistakably Angel's. She rolls you onto your back and positions herself over your face,"
                            + " with her legs pinning your arms. Her bare pussy is right in front of you, just out of reach of your tongue. It's weird that she's naked, considering "
                            + "she caught you by surprise, but this is Angel after all.<br/><br/>";
        } else {
            return "You and " + target.getName()
                            + " grapple back and forth for several minutes. Soon you're both tired, sweaty, and aroused. You catch her hands for a moment and "
                            + "run your tongue along her neck and collarbone. Recognizing her disadvantage, she jumps out of your grasp and directly into Angel. Neither of you "
                            + "noticed Angel approach. Before " + target.getName()
                            + " can react, Angel pulls her into a passionate kiss. " + target.getName()
                            + " forgets to resist and goes limp " + "long enough for Angel to pin her arms.<br/><br/>";
        }
    }

    @Override
    public boolean fit() {
        return !character.mostlyNude() && character.stamina
                                                   .percent() >= 50;
    }

    public void advance() {
        character.getGrowth().addTrait(10, Trait.demigoddess);
        character.getGrowth().addTrait(10, Trait.divinity);
        character.getGrowth().addTrait(10, Trait.proheels);
        if (character.hasPussy()) {
            character.body.getRandomPussy().addMod(new DivineMod());
        }
        if (character.hasDick()) {
            character.body.getRandomCock().addMod(new BlessedCockMod());
        }
        var wings = new WingsPart();
        wings.addMod(new AngelicWingsMod());
        character.body.add(wings);
        character.unequipAllClothing();
        character.outfitPlan.add(Clothing.getByID("translucentshawl"));
        character.outfitPlan.add(Clothing.getByID("bikinitop"));
        character.outfitPlan.add(Clothing.getByID("bikinibottoms"));
        character.outfitPlan.add(Clothing.getByID("highheels"));
        character.modAttributeDontSaveData(Attribute.Divinity, 1);

        character.getGrowth().addOrRemoveTraits(character);
    }

    @Override
    public boolean checkMood(Combat c, Emotion mood, int value) {
        switch (mood) {
            case horny:
                return value >= 50;
            case nervous:
                return value >= 150;
            default:
                return value >= 100;
        }
    }
    
    /**Helper method to Add this character's first Combat focus scene 
     * ANGEL: Sex or Nymphomania Focus.
     * 
     * */
    private void addFirstFocusScene(){
        character.addCombatScene(new CombatScene((c, self, other) -> {
            return self.getProgression().getLevel() >= 10 && !Global.checkFlag(ANGEL_SEX_FOCUS)
                            && !Global.checkFlag(ANGEL_NYMPHOMANIA_FOCUS);
        }, (c, self, player) -> Global.format(
                        "After another exhausting fight with her where you've been pretty much been driven insane with her sexual prowess, "
                                        + "Angel shows a rare moment of vulnerability and slides into your arms. <i>\"Hey {other:name}, I've been thinking, do you think this is wrong?\"</i> "
                                        + "Puzzled, you ask her what she means. <i>\"Well... you know, fucking like rabbits every day. "
                                        + "I love feeling good and I know I'm good at making other people feel good. Sometimes though, I wonder if I'm being too overbearing. "
                                        + "Most " + Shorthand.guyOrGirl(player)
                                        + "s can't even keep up with me, but I feel like I always want more. I feel like sometimes I'm no longer in control you know?\"</i>"
                                        + "<br/><br/>" + "",
                        self, player),
                        Arrays.asList(new CombatSceneChoice("Tell her you love her technique.", (c, self, other) -> {
                            c.write("You hug Angel, and tell her that you love how she makes you feel, and sex with her is akin to a religious experience for you. "
                                            + "Angel looks surprised for a second before giving you a wry grin. <i>\"You're not so bad yourself. But you're right, I am better.\"</i>"
                                            + "<br/><br/>"
                                            + "As if to prove a point, she sits down again on your rehardened cock. "
                                            + "You groan as she slides rocks herself back and forth yet again, fucking you with a soft smile. "
                                            + "<i>\"Quality over quantity as they say. I'll train you so you'll only be able to cum inside me. Wouldn't that be amusing "
                                            + other.getName() + "?\" </i>"
                                            + "It's too much for your poor oversensitive cock, as you pump what remains in you into Angel a final time."
                                            + "<br/>"
                                            + "Angel gives you a quick kiss on the lips before leaving. <i>\"Thanks for listening to me "
                                            + other.getName() + ", but you better be ready for me next time.\"</i>");
                            useSex();
                            return true;
                        }), new CombatSceneChoice("Tell her you love her sex drive", (c, self, other) -> {
                            c.write("You tell Angel that her insatiable sex drive is one of the things you like best about her. "
                                            + "After all, there's nothing else quite like fucking someone as enthusiastic as her. "
                                            + "Angel looks up and makes eye contact with you. She seems rather... amused? "
                                            + "<i>\"That's what all " + Shorthand.boyOrGirl(other)
                                            + "s say before they're squeezed dry. "
                                            + "You're not going to be the same are you?\"</i>" + "<br/><br/>"
                                            + "You swallow your saliva a bit worriedly. What have you gotten yourself into?");
                            useNymphomania();
                            return true;
                        }), new CombatSceneChoice("Tell her you love both [Hard Mode]", (c, self, other) -> {
                            c.write("You tell Angel that her insatiable sex drive and amazing sex technique are both part of what makes her unique and amazing(ly attractive). "
                                            + "There are people out there with one or the other, but no one but Angel is as good at both. Angel grins and licks her lips"
                                            + "<i>\"From most people, that would sound like a pathetic wishy-washy non-answer, but the way you put that made it sound like "
                                            + "a challenge. I'll take that challenge, but think you've just doomed yourself to being my sex toy.\"</i>"
                                            + "<br/><br/>"
                                            + "Angel looks like she's found a whole new level of resolve- you suddenly realize that you've made a terrible mistake. You resolve to step up training yourself, if that's possible.");
                            useNymphomania();
                            useSex();
                            character.getGrowth().additionalExtraAttributePoint();
                            // some compensation for the added difficulty. She gets 6 traits and 2 attribute points/level, and you only get 2 traits, but you are fighting more people than just her.
                            Global.getPlayer()
                                  .getGrowth()
                                  .addTraitPoints(new int[] {12, 39}, Global.getPlayer());
                            return true;
                        }))));
    }
    /**Helper method to Add this character's second Combat focus scene 
     * ANGEL: Follower or worship Focus
     * 
     * */
    private void addSecondFocusScene(){
        character.addCombatScene(new CombatScene((c, self, other) -> {
            return self.getProgression().getLevel() >= 20 && !Global.checkFlag(ANGEL_FOLLOWERS_FOCUS)
                            && !Global.checkFlag(ANGEL_WORSHIP_FOCUS)
                            && (Global.checkFlag(ANGEL_SEX_FOCUS) || Global.checkFlag(ANGEL_NYMPHOMANIA_FOCUS));
        }, (c, self, player) -> "After another session of fantastic sex with your favorite sex goddess, Angel looks a bit reluctant to leave. "
                        + "You do want to try to get some more matches in, but her broody look invites some discussion. "
                        + "Patting your own lap, you motion for Angel to sit down. Luckily for you, she doesn't leave you hanging and does just that. "
                        + "<br/><br/>"
                        + "The two of you spend a few moments relaxing before Angel decides to bring up her worries. "
                        + "<i>\"Say " + player.getName() + ", what do you really think of me become a real goddess? "
                        + "To be honest, I'm not even sure how it started. One night after the games, I started feeling a heat in my chest. "
                        + "The next night when we started the match, I already had my wings. "
                        + "I didn't think too much of it, with all the other strange things going on, "
                        + "but recently I've started hearing a voice in my head asking me to gather faith. "
                        + "I don't know what to think of it. I'm pretty happy with who I am now, and my friends are very important to me. "
                        + "If I do try and build a following, I'm worried that I'll start leaving them behind.\"</i>",
                        Arrays.asList(new CombatSceneChoice("Answer: Focus on your friends", (c, self, other) -> {
                            c.write("You answer that it just doesn't seem like Angel to abandon her friends even if she does become a full Goddess. "
                                            + "There's no need to pick right? If she tells them, Mei, Sarah and Caroline would probably volunteer to be "
                                            + "her followers numbers 1, 2 and 3." + "<br/><br/>"
                                            + "Angel scowls a bit at you and questions, <i>\"You mean you're not going to be my follower?\"</i> "
                                            + "You hastily deny that and try to resolve the misunderstanding before you realize Angel's stern "
                                            + "expression melts into a smile and you realize she was just joking. "
                                            + "<i>\"Thanks " + other.getName()
                                            + ", that took a lot off my mind. I'll pay you back, don't worry. "
                                            + "A Goddess always keeps her promises.\"</i>");
                            useFollowers();
                            return true;
                        }), new CombatSceneChoice("Answer: Focus on gathering a following", (c, self, other) -> {
                            c.write(Global.format(
                                            "You reply that you would her first follower if she was looking for them. "
                                                            + "You jokingly kneel in front of her and kiss her feet. "
                                                            + "Angel seems surprised for a moment before a familiar lascivious smile creeps into her expression. "
                                                            + "<i>\"{other:name}, {other:name}, {other:name}, what do we have here? A willing supplicant? "
                                                            + "Do you want to serve your Goddess? Groveling, begging, pleading, does that thrill you?\"</i>"
                                                            + "<br/><br/>"
                                                            + "There's a bit of a dangerous tone in Angel's voice, and you motion to get back up. "
                                                            + "However, a shapely foot forces your head onto the ground again. "
                                                            + "<i>\"Stand down!\"</i> she demands. This started off as a joke, but something in Angel's voice demands your obedience. "
                                                            + "Trembling, you do as she says and avert your eyes, looking only at the lower half of her perfect body. Angel chuckles as she inspects your prone form. "
                                                            + "<i>\"You know {other:name}, I wasn't sure I was cut out for this deity business, but I have to admit, this feels pretty good right now. "
                                                            + "Well what are you waiting for? Show me your faith!\"</i> "
                                                            + "You scramble to your feet and start lapping at Angel's heavenly orfice. "
                                                            + "Only after you bring her to two consecutive climaxes does she let you go. "
                                                            + "<br/><br/>"
                                                            + "Before she leaves, Angel kneels down to face you, and gives you a big kiss on the lips."
                                                            + "<i>\"Thanks {other:name} for being patient with me. This is all pretty so new, but I feel good with you by my side.\"</i>",
                                            self, other));
                            useWorship();
                            return true;
                        }), new CombatSceneChoice("Answer: Why not both? [Hard Mode]", (c, self, other) -> {
                            c.write(Global.format(
                                            "You tell her that the Angel you know wouldn't even think of choosing between either of them. "
                                                            + "The Angel you know could gather a following within a day even while maintaining her close friendships. She's just amazing like that. "
                                                            + "<br/>For the first time since you've gotten to know her, Angel seems to blush. \"<i>Enough flattery "
                                                            + other.getName()
                                                            + ", it wont make me go any easier on you. "
                                                            + "But you know, you're right. Why should I choose? They should be happy to grovel for me. I <b>am</b> a Goddess after all.</i>\"",
                                            self, other));
                            useWorship();
                            useFollowers();
                            character.getGrowth().additionalExtraAttributePoint();
                            // some compensation for the added difficulty. She gets 6 traits and 1 attribute point/level, and you only get 2 traits, but you are fighting more people than just her.
                            Global.getPlayer()
                                  .getGrowth()
                                  .addTraitPoints(new int[] {21, 48}, Global.getPlayer());
                            return true;
                        }))));
    }
    
}
