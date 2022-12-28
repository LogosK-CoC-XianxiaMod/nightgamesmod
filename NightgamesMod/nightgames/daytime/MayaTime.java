package nightgames.daytime;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.characters.body.CockPart;
import nightgames.characters.body.CockPart.Size;
import nightgames.characters.body.mods.pitcher.BionicCockMod;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.requirements.BodyPartRequirement;
import nightgames.requirements.NotRequirement;
import nightgames.requirements.RequirementShortcuts;
import nightgames.status.addiction.Addiction;
import nightgames.status.addiction.AddictionType;

import java.util.ArrayList;
import java.util.Optional;

public class MayaTime extends BaseNPCTime {
    public MayaTime(Character player) {
        super(player, Global.getNPC("Maya"));
        knownFlag = "MayaKnown";        
        //TODO: MayaKnown - You have to pay Aesop to know where the hell she works, Aesop says it won't be cheap because he doesn't wanna piss her off. "No one should be talking to Maya."
        giftedString = "\"Well, thank you!\"";
        giftString = "\"A present? That's very nice of you.\"";
        transformationOptionString = "Hypnotic Suggestions";
        advTrait = null;
        transformationIntro = "[Placeholder]Maya explains that she can use her powers to affect you during the games...";
        loveIntro = "";
        transformationFlag = "";
    }

    @Override
    public void buildTransformationPool() {
        transformationOptions = new ArrayList<>();
        {
            TransformationOption growCock = new TransformationOption();
            growCock.ingredients.put(Item.PriapusDraft, 2);
            growCock.addRequirement(RequirementShortcuts.rev(new NotRequirement(new BodyPartRequirement(CockPart.TYPE))), "Has no penis");
            growCock.option = "Maya: Grow a cock";
            growCock.scene = "[Placeholder]<br/>Maya does some sort of thing and grows a cock.";
            growCock.effect = (c, self, other) -> {
                var cock = new CockPart(Size.Big);
                cock.addMod(new BionicCockMod());
                other.body.add(cock);
                return true;
            };
            transformationOptions.add(growCock);
        }
        {
            TransformationOption removeCock = new TransformationOption();
            removeCock.ingredients.put(Item.FemDraft, 2);
            removeCock.addRequirement(RequirementShortcuts.rev(new BodyPartRequirement(CockPart.TYPE)), "Has a penis");
            removeCock.option = "Maya: Remove her cock";
            removeCock.scene = "[Placeholder]<br/>Maya reverses whatever she did to get a cock to remove her cock.";
            removeCock.effect = (c, self, other) -> {
                other.body.removeAll(CockPart.TYPE);
                return true;
            };
            transformationOptions.add(removeCock);
        }

    
    }

    @Override
    public void subVisitIntro(String choice) {
        ArrayList<String> choices = new ArrayList<>();
        if (npc.getAffection(player) > 0) {
            Global.gui().message("You go to a nearby post-doctoral office to find Maya.");
            choices.add("Games");
            choices.add("Sparring");
            choices.add("Sex");
            
            if(player.getPure(Attribute.Hypnosis)>=3){
                choices.add("Faerie play");
            }
            if (Global.getPlayer().checkAddiction(AddictionType.MIND_CONTROL,npc)) {
                choices.add("Confront about control");
            }
        } else if (Global.getPlayer().checkAddiction(AddictionType.MIND_CONTROL,npc)) {
            Global.gui().message("Maya low-affection addiction intro");
            if (npc.getAttraction(player) < 15) {
                npc.gainAttraction(player, 2); 
                player.gainAttraction(npc, 2);
            } else {
                npc.gainAffection(player, 1);
                player.gainAffection(npc, 1);
                choices.add("Games");
                choices.add("Sparring");
                //choices.add("Sex");
            }
            choices.add("Get Hypnotized");
        } else if (npc.getAttraction(player) < 15) {
            Global.gui().message("You eventually find Maya in one of the post-doctoral research offices, engrossed with work. Maya's office is perfectly arranged, and her face seems to be engrossed in reading and writing.<br/><br/>"
                            + "<i>\"Oh, hello, " + player.getTrueName() + ".\"</i> She doesn't bother looking at you, but continues typing while going through some kind of book. "
                            + "<i>\"What brings you here?\"</i><br/><br/>"
                            + "You ask her if she'd like to take a break and hang out for a while.<br/><br/>"
                            + "<i>\"No thanks. I have important work to do here, and I'm not leaving until it's done.\"</i><br/><br/> "
                                  + "She picks up what looks like a piping hot cup of creamed coffee and sips it.<br/><br/>"
                                  + "<i>\"I'm sorry, but if you want me to play with you tonight, the answer is no. Go play with the others - that's why they are there. I am in here, working and helping to run the games.\"</i>"
                                  + "<br/><br/>She doesn't say much more and you leave the room, wondering if it was something you said or if you overstepped your bounds.<br/>");
            npc.gainAttraction(player, 1);
            player.gainAttraction(npc, 1);
        } else {
            Global.gui().message("You find Maya in one of the post-doctoral research offices, engrossed with work. "
                            + "<i>\"Hello, " + player.getTrueName() + ". I'm working on something games-related, so if you sit down and stay quiet you can stay.\"</i>, says Maya while motioning to a nearby chair.<br/><br/>"
                            + " You grab a chair and sit down, occasionally chatting with Maya about various administrative tasks that she handles while recruiting. Observing her work, you notice that Maya is very smart and seems very committed "
                            + "to doing a good job.<br/><br/>"
                            + "Eventually curiosity gets the better of you and you have to ask why she seems so dedicated to the games.<br/> "
                            + "<i>\"The games mean a lot to me. I owe everything to the games.\"</i><br/><br/>"
                            + "Well, considering there's all sorts of strange supernatural stuff going on, that makes sense, but you press her for more details: "
                            + "<i>\"I used to compete, just like you. I almost died once, but I was thankfully saved.\"</i><br/><br/>"
                            + "She glances over at you, her sharp eyes softening a bit before re-hardening before her face returns to her work. "
                            + "<i>\"So...We may be lucky enough to enjoy The Benefactor's protection, but I still expect you to take the games seriously.\"</i><br/><br/>"
                            + "You get the feeling there's more to this story, but you're pretty sure pressing further is a mistake. You assure Maya that you're on her side and won't cause trouble.<br/><br/>"
                            + "<i>\"Good. I care a lot about the games, so don't make me have to discipline you myself for screwing up.\"</i><br/><br/>"
                            + "It seems Maya's harder to get to open up than the other girls, but she's definitely very interesting.<br/>");
            npc.gainAffection(player, 1);
            player.gainAffection(npc, 1);
            choices.add("Games");
            choices.add("Sparring");
            //choices.add("Sex");
        }
        choices.add("Leave");
        player.chooseActivitySubchoices(this, choices);
    }

    @Override
    public void subVisit(String choice) {
        ArrayList<String> choices = new ArrayList<>();
        if (choice.equals("Get Hypnotized")) {
            if (npc.getAffection(player) == 0) {
                Global.gui().message("[Placeholder] Maya has some fun making you do embarassing things in front of her. You feel like she enjoys being just a little cruel to you to amuse herself.");
            // The buttslut scene isn't fully written yet
            } else /* if (!player.hasStatus(Stsflag.buttsluttraining)) */ {
                Global.gui().message("[Placeholder] Maya Addiction Sate Scene.");
            } /*else {
               
            }*/
            choices.add("Leave");
            Global.getPlayer().addict(null, AddictionType.MIND_CONTROL, npc, Addiction.MED_INCREASE);
            Addiction mc = Global.getPlayer().getAddiction(AddictionType.MIND_CONTROL);
            if (mc != null) mc.flagDaytime();
        }
        if (choice.equals("Sex")) {
            if (npc.getAffection(player) >= 8 && (!player.has(Trait.ticklemonster) || Global.random(2) == 1)) {
                Global.gui()
                      .message("[Placeholder] Maya Sex Scene - If you even ever have the chance to get it - Gives Trait.");
                /*//GIVE TRAIT FOR SEX SCENE
                if (!player.has(Trait.ticklemonster)) {
                    Global.gui().message("<br/><br/><b>You've gotten better at finding sensitive spots when tickling nude opponents.</b>");
                    player.add(Trait.ticklemonster);
                    npc.getGrowth().addTrait(0, Trait.ticklemonster);
                }
                */
            } else {
                Global.gui().message("[Placeholder] Maya Sex Scene - If you even ever have the chance to get it.");

            }
            choices.add("Leave");
            Daytime.train(player, npc, Attribute.Seduction);
            npc.gainAffection(player, 1);
            player.gainAffection(npc, 1);
        } else if (choice.equals("Games")) {
            if (npc.getAffection(player) >= 16 && (!player.has(Trait.spider) || Global.random(2) == 1)) {
                Global.gui().message("[Placeholder] Maya games scene - this one gives you a trait.");
                /*
                //Gain trait
                if (!player.has(Trait.spider)) {
                    Global.gui()
                          .message("<br/><br/><b>Mara has taught you to make the brilliant and insanely complex Spiderweb Trap.</b>");
                    player.add(Trait.spider);
                    npc.getGrowth().addTrait(0, Trait.spider);
                }
                */
            } else {
                Global.gui().message("[Placeholder] Maya games scene - normal version.");
            }
            choices.add("Leave");
            Daytime.train(player, npc, Attribute.Cunning);
            npc.gainAffection(player, 1);
            player.gainAffection(npc, 1);
        } else if (choice.equals("Sparring")) {
            if (npc.getAffection(player) >= 12 && (!player.has(Trait.heeldrop) || Global.random(2) == 1)) {
                Global.gui().message("[Placeholder] Maya Sparring scene - Trait gain version.");
                
                /*
                if (!player.has(Trait.heeldrop)) {
                   // Global.gui().message("<br/><br/><b>You've experienced Mara's most painful technique and learned how to use it yourself.</b>");
                    player.add(Trait.heeldrop);
                    npc.getGrowth().addTrait(0, Trait.heeldrop);
                }
                */
            } else {
                Global.gui().message("[Placeholder] Maya Sparring scene - normal version.");

            }
            choices.add("Leave");
            Daytime.train(player, npc, Attribute.Power);
            npc.gainAffection(player, 1);
            player.gainAffection(npc, 1);
            
        } else if (choice.equals("Lunar Event")){
            Global.gui().message("[Placeholder] Functional scene for Maya.");
            /*
            if(!player.has(Trait.faefriend)){
                //Global.gui().message("");
                //player.add(Trait.faefriend);
            }
            */
            choices.add("Leave");
            npc.gainAffection(player,1);
            player.gainAffection(npc,1);
        } else if (choice.equals("Leave")) {
            done(true);
            return;
        }
        player.chooseActivitySubchoices(this, choices);
    }
    
    @Override
    public Optional<String> getAddictionOption() {
        return Global.getPlayer().checkAddiction(AddictionType.MIND_CONTROL,npc) ?
                        Optional.of("Confront about control") : Optional.empty();
    }
    
    private void choiceTraitBolster(){}

    @Override
    public void doPlayGamesWith() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doHaveSex() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doSparring() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doAddictionScene() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doGivePresent() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doSpecialFunction1() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doSpecialFunction2() {
        // TODO Auto-generated method stub
        
    }
}
