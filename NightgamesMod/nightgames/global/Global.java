package nightgames.global;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import nightgames.Resources.ResourceLoader;
import nightgames.characters.Character;
import nightgames.characters.*;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.CockPart;
import nightgames.characters.body.StraponPart;
import nightgames.characters.custom.CustomNPC;
import nightgames.characters.custom.DataBackedNPCData;
import nightgames.characters.custom.JsonSourceNPCDataLoader;
import nightgames.combat.Combat;
import nightgames.daytime.Daytime;
import nightgames.grammar.Shorthand;
import nightgames.gui.GUI;
import nightgames.gui.HeadlessGui;
import nightgames.items.Item;
import nightgames.items.clothing.Clothing;
import nightgames.json.JsonUtils;
import nightgames.match.Action;
import nightgames.match.Match;
import nightgames.match.MatchType;
import nightgames.match.actions.Masturbate;
import nightgames.match.actions.Wait;
import nightgames.match.actions.*;
import nightgames.modifier.BaseModifier;
import nightgames.modifier.standard.*;
import nightgames.pet.PetCharacter;
import nightgames.pet.Ptype;
import nightgames.skills.Struggle;
import nightgames.skills.*;
import nightgames.start.NpcConfiguration;
import nightgames.start.PlayerConfiguration;
import nightgames.start.StartConfiguration;
import nightgames.trap.Tripwire;
import nightgames.trap.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.parboiled.common.Tuple2;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Global {
    private static Random rng;                                      //Isn't the convention for static variables at this level is to put them in all caps? -DSM
    private static GUI gui;    
    public static Scene current;
    public static Scene previous;
    private static final int LINEUP_SIZE = 5;           
    public static int debugSimulation = 0;
    public static double moneyRate = 1.0;
    public static double xpRate = 1.0;
    public static final Path COMBAT_LOG_DIR = new File("combatlogs").toPath();
    public static boolean debug[] = new boolean[DebugFlags.values().length];
    public static ContextFactory factory;
    public static Context cx;
    
    //EXTRACT TO TIMEKEEPING COMPONENT
    public static Daytime day;                                    
    protected static int date;
    private static Time time;
    private static Date jdate;



    //THE FOLLOWING ITEMS ARE CANDIDATES FOR EXTRACTION TO A GAMEDATA CLASS - DSM
    private static Map<String, NPC> characterPool;
    private static Set<Skill> skillPool = new HashSet<>();          //These central peices of data are not going to change. so they should be gathered and separated for better management. - DSM 
    private static Set<Action> actionPool;
    private static Set<Trait> featPool;
    private static Set<BaseModifier> modifierPool;
    private static Set<Character> players;
    private static Set<Character> debugChars;
    private static Set<Character> resting;
    private static Set<String> flags;                               //Global flags - 
    private static Map<String, Float> counters;
    public static Player human;                                     //Useful for many reasons, redundant in a game where all elements are stored equally. There's many ways to get the player. - DSM
    private static Match match;                                     //Only a complete program flow restructure would change this, but many matches are going on as the player may be fighting - DSM

    //public static Map<Trait, Resistance> RESISTANCEMAP;
    //public static Resistance nullResistance;                      //Why is this required? 
    //public static final Map<Trait, Collection<Trait>> OVERRIDES;  
    private static TraitTree traitRequirements;                     //Traits can and probably should carry their own requirements with them. -DSM
    private static HashMap<String, MatchAction> matchActions;           //Static Naming conventions -DSM

     private static Character noneCharacter = new NPC("none", 1, null);     
     
    static {
        hookLogwriter();
        rng = new Random();
        flags = new HashSet<>();
        players = new HashSet<>();
        debugChars = new HashSet<>();
        resting = new HashSet<>();
        counters = new HashMap<>();
        jdate = new Date();
        counters.put(Flag.malePref.name(), 0.f);
        Clothing.buildClothingTable();
        PrintStream fstream;
        try {
            File logfile = new File("nightgames_log.txt");
            // append the log if it's less than 2 megs in size.
            fstream = new PrintStream(new FileOutputStream(logfile, logfile.length() < 2L * 1024L * 1024L));
            OutputStream estream = new TeeStream(System.err, fstream);
            OutputStream ostream = new TeeStream(System.out, fstream);
            System.setErr(new PrintStream(estream));
            System.setOut(new PrintStream(ostream));
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream("build.properties");

            System.out.println("=============================================");
            System.out.println("Nightgames Mod");
            if (stream != null) {
                Properties prop = new Properties();
                prop.load(stream);
                System.out.println("version: " + prop.getProperty("version"));
                System.out.println("buildtime: " + prop.getProperty("buildtime"));
                System.out.println("builder: " + prop.getProperty("builder"));
            } else {
                System.out.println("dev-build");
            }
            System.out.println(new Timestamp(jdate.getTime()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        setTraitRequirements(new TraitTree(ResourceLoader.getFileResourceAsStream("data/TraitRequirements.xml")));
        current = null;
        factory = new ContextFactory();
        cx = factory.enterContext();
        buildParser();
        buildActionPool();
        buildFeatPool();
        buildSkillPool(noneCharacter);
        buildModifierPool();
    }

    protected static void makeGUI() {
        gui = new GUI();
    }
    
    private static void makeTestGUI() {
        gui = new HeadlessGui();
    }

    public static boolean meetsRequirements(Character c, Trait t) {
        return getTraitRequirements().meetsRequirements(c, t);
    }

    /**Begins a new game, given the various conditions for start. Builds all required sets and members of the player and participating characters.
     * 
     * @param playerName
     * The name of the player. More than likely grabbed from the new game screen GUI elements. 
     * 
     * @param config
     * The starting configuration for the game. 
     * 
     * @param pickedTraits
     * A list of traits that the player has picked at chargen.
     * 
     * */
    public static void newGame(String playerName, StartConfiguration config, List<Trait> pickedTraits,
                    CharacterSex pickedGender, Map<Attribute, Integer> selectedAttributes) {
        PlayerConfiguration playerConfig = config != null ? config.player : null;
        Collection<String> cfgFlags = config != null ? config.getFlags() : new ArrayList<>();
        human = new Player(playerName, gui, pickedGender, playerConfig, pickedTraits, selectedAttributes);
        if(human.has(Trait.largereserves)) {
            human.willpower.gain(20);
        }
        players.add(human);
        if (gui != null) {
            gui.populatePlayer(human);
        }
        buildSkillPool(human);
        Clothing.buildClothingTable();
        learnSkills(human);
        rebuildCharacterPool(config);
        // Add starting characters to players
        players.addAll(characterPool.values().stream().filter(npc -> npc.isStartCharacter).collect(Collectors.toList()));
        if (!cfgFlags.isEmpty()) {
            flags = new HashSet<>(cfgFlags);
            System.out.println("flags: "+flags.toString());
        }      
        Map<String, Boolean> configurationFlags = JsonUtils.oldMapFromJson(JsonUtils.rootJson(new InputStreamReader(ResourceLoader.getFileResourceAsStream("data/globalflags.json"))).getAsJsonObject(), String.class, Boolean.class);
        configurationFlags.forEach(Global::setFlag);

        time = Time.NIGHT;
        date = 1;
        setCharacterDisabledFlag(getNPCByType("Yui"));
        setFlag(Flag.systemMessages, true);
    }

    public static int random(int start, int end) {
        return rng.nextInt(end - start) + start;
    }

    public static int random(int d) {
        if (d <= 0) {
            return 0;
        }
        return rng.nextInt(d);
    }

    // finds a centered random number from [0, d] (inclusive)
    public static int centeredrandom(int d, double center, double sigma) {
        int val = 0;
        center = Math.max(0, Math.min(d, center));
        for (int i = 0; i < 10; i++) {
            double f = rng.nextGaussian() * sigma + center;
            val = (int) Math.round(f);
            if (val >= 0 && val <= d) {
                return val;
            }
        }
        return Math.max(0, Math.min(d, val));
    }

    public static GUI gui() {
        return gui;
    }

    /**
     * WARNING DO NOT USE THIS IN ANY COMBAT RELATED CODE.
     * IT DOES NOT TAKE INTO ACCOUNT THAT THE PLAYER GETS CLONED. WARNING. WARNING.
     * 
     * NOTE: This is a "global accessor" to a private static, it's called across the project as a peice of vital data. 
     * For this reason it's a piece that should be accessed more properly and without the danger originally posted. Very good example of something that can change. -DSM
     * 
     * @return
     */
    public static Player getPlayer() {
        return human;
    }

    /**Helper method that Builds the pool of skills. Called by newgame() and reserforLoad().
     * 
     * */
    public static void buildSkillPool(Character ch) {
        getSkillPool().clear();
        getSkillPool().add(new Slap(ch));
        getSkillPool().add(new Tribadism(ch));
        getSkillPool().add(new PussyGrind(ch));
        getSkillPool().add(new Slap(ch));
        getSkillPool().add(new ArmBar(ch));
        getSkillPool().add(new Blowjob(ch));
        getSkillPool().add(new Cunnilingus(ch));
        getSkillPool().add(new Escape(ch));
        getSkillPool().add(new Flick(ch));
        getSkillPool().add(new ToggleKnot(ch));
        getSkillPool().add(new LivingClothing(ch));
        getSkillPool().add(new LivingClothingOther(ch));
        getSkillPool().add(new Engulf(ch));
        getSkillPool().add(new CounterFlower(ch));
        getSkillPool().add(new Knee(ch));
        getSkillPool().add(new LegLock(ch));
        getSkillPool().add(new LickNipples(ch));
        getSkillPool().add(new Maneuver(ch));
        getSkillPool().add(new Paizuri(ch));
        getSkillPool().add(new PerfectTouch(ch));
        getSkillPool().add(new Restrain(ch));
        getSkillPool().add(new Reversal(ch));
        getSkillPool().add(new LeechEnergy(ch));
        getSkillPool().add(new SweetScent(ch));
        getSkillPool().add(new Spank(ch));
        getSkillPool().add(new Stomp(ch));
        getSkillPool().add(new StandUp(ch));
        getSkillPool().add(new WildThrust(ch));
        getSkillPool().add(new SuckNeck(ch));
        getSkillPool().add(new Tackle(ch));
        getSkillPool().add(new Taunt(ch));
        getSkillPool().add(new Trip(ch));
        getSkillPool().add(new Whisper(ch));
        getSkillPool().add(new Kick(ch));
        getSkillPool().add(new PinAndBlow(ch));
        getSkillPool().add(new PinningPaizuri(ch));
        getSkillPool().add(new Footjob(ch));
        getSkillPool().add(new FootPump(ch));
        getSkillPool().add(new HeelGrind(ch));
        getSkillPool().add(new Handjob(ch));
        getSkillPool().add(new Squeeze(ch));
        getSkillPool().add(new Nurple(ch));
        getSkillPool().add(new Finger(ch));
        getSkillPool().add(new Aphrodisiac(ch));
        getSkillPool().add(new Lubricate(ch));
        getSkillPool().add(new Dissolve(ch));
        getSkillPool().add(new Sedate(ch));
        getSkillPool().add(new Tie(ch));
        getSkillPool().add(new nightgames.skills.Masturbate(ch));
        getSkillPool().add(new Piston(ch));
        getSkillPool().add(new Grind(ch));
        getSkillPool().add(new Thrust(ch));
        getSkillPool().add(new UseDildo(ch));
        getSkillPool().add(new UseOnahole(ch));
        getSkillPool().add(new UseCrop(ch));
        getSkillPool().add(new Carry(ch));
        getSkillPool().add(new Tighten(ch));
        getSkillPool().add(new ViceGrip(ch));
        getSkillPool().add(new HipThrow(ch));
        getSkillPool().add(new SpiralThrust(ch));
        getSkillPool().add(new Bravado(ch));
        getSkillPool().add(new Diversion(ch));
        getSkillPool().add(new Undress(ch));
        getSkillPool().add(new StripSelf(ch));
        getSkillPool().add(new StripTease(ch));
        getSkillPool().add(new Sensitize(ch));
        getSkillPool().add(new EnergyDrink(ch));
        getSkillPool().add(new Strapon(ch));
        getSkillPool().add(new AssFuck(ch));
        getSkillPool().add(new Turnover(ch));
        getSkillPool().add(new Tear(ch));
        getSkillPool().add(new Binding(ch));
        getSkillPool().add(new Bondage(ch));
        getSkillPool().add(new WaterForm(ch));
        getSkillPool().add(new DarkTendrils(ch));
        getSkillPool().add(new Dominate(ch));
        getSkillPool().add(new Illusions(ch));
        getSkillPool().add(new Glamour(ch));
        getSkillPool().add(new LustAura(ch));
        getSkillPool().add(new MagicMissile(ch));
        getSkillPool().add(new Masochism(ch));
        getSkillPool().add(new NakedBloom(ch));
        getSkillPool().add(new ShrinkRay(ch));
        getSkillPool().add(new SpawnFaerie(ch, Ptype.fairyfem));
        getSkillPool().add(new SpawnFaerie(ch, Ptype.fairyherm));
        getSkillPool().add(new SpawnImp(ch, Ptype.impfem));
        getSkillPool().add(new SpawnFaerie(ch, Ptype.fairyherm));
        getSkillPool().add(new SpawnFaerie(ch, Ptype.fairymale));
        getSkillPool().add(new SpawnImp(ch, Ptype.impmale));
        getSkillPool().add(new SpawnFGoblin(ch, Ptype.fgoblin));
        getSkillPool().add(new SpawnSlime(ch));
        getSkillPool().add(new StunBlast(ch));
        getSkillPool().add(new Fly(ch));
        getSkillPool().add(new Command(ch));
        getSkillPool().add(new Obey(ch));
        getSkillPool().add(new OrgasmSeal(ch));
        getSkillPool().add(new DenyOrgasm(ch));
        getSkillPool().add(new Drain(ch));
        getSkillPool().add(new StoneForm(ch));
        getSkillPool().add(new FireForm(ch));
        getSkillPool().add(new Defabricator(ch));
        getSkillPool().add(new TentaclePorn(ch));
        getSkillPool().add(new Sacrifice(ch));
        getSkillPool().add(new Frottage(ch));
        getSkillPool().add(new FaceFuck(ch));
        getSkillPool().add(new VibroTease(ch));
        getSkillPool().add(new TailPeg(ch));
        getSkillPool().add(new CommandDismiss(ch));
        getSkillPool().add(new CommandDown(ch));
        getSkillPool().add(new CommandGive(ch));
        getSkillPool().add(new CommandHurt(ch));
        getSkillPool().add(new CommandInsult(ch));
        getSkillPool().add(new CommandMasturbate(ch));
        getSkillPool().add(new CommandOral(ch));
        getSkillPool().add(new CommandStrip(ch));
        getSkillPool().add(new CommandStripPlayer(ch));
        getSkillPool().add(new CommandUse(ch));
        getSkillPool().add(new ShortCircuit(ch));
        getSkillPool().add(new IceForm(ch));
        getSkillPool().add(new Barrier(ch));
        getSkillPool().add(new CatsGrace(ch));
        getSkillPool().add(new Charm(ch));
        getSkillPool().add(new Tempt(ch));
        getSkillPool().add(new EyesOfTemptation(ch));
        getSkillPool().add(new ManipulateFetish(ch));
        getSkillPool().add(new TailJob(ch));
        getSkillPool().add(new FaceSit(ch));
        getSkillPool().add(new Smother(ch));
        //getSkillPool().add(new BreastSmother(ch));
        getSkillPool().add(new MutualUndress(ch));
        getSkillPool().add(new Surrender(ch));
        getSkillPool().add(new ReverseFuck(ch));
        getSkillPool().add(new ReverseCarry(ch));
        getSkillPool().add(new ReverseFly(ch));
        getSkillPool().add(new CounterRide(ch));
        getSkillPool().add(new CounterPin(ch));
        getSkillPool().add(new ReverseAssFuck(ch));
        getSkillPool().add(new Nurse(ch));
        getSkillPool().add(new Suckle(ch));
        getSkillPool().add(new UseDraft(ch));
        getSkillPool().add(new ThrowDraft(ch));
        getSkillPool().add(new ReverseAssFuck(ch));
        getSkillPool().add(new FondleBreasts(ch));
        getSkillPool().add(new Fuck(ch));
        getSkillPool().add(new Kiss(ch));
        getSkillPool().add(new Struggle(ch));
        getSkillPool().add(new Tickle(ch));
        getSkillPool().add(new nightgames.skills.Wait(ch));
        getSkillPool().add(new Bluff(ch));
        getSkillPool().add(new StripTop(ch));
        getSkillPool().add(new StripBottom(ch));
        getSkillPool().add(new Shove(ch));
        getSkillPool().add(new Recover(ch));
        getSkillPool().add(new Straddle(ch));
        getSkillPool().add(new ReverseStraddle(ch));
        getSkillPool().add(new Stunned(ch));
        getSkillPool().add(new Distracted(ch));
        getSkillPool().add(new PullOut(ch));
        getSkillPool().add(new ThrowDraft(ch));
        getSkillPool().add(new UseDraft(ch));
        getSkillPool().add(new TentacleRape(ch));
        getSkillPool().add(new Anilingus(ch));
        getSkillPool().add(new UseSemen(ch));
        getSkillPool().add(new Invitation(ch));
        getSkillPool().add(new SubmissiveHold(ch));
        getSkillPool().add(new BreastGrowth(ch));
        getSkillPool().add(new CockGrowth(ch));
        getSkillPool().add(new BreastRay(ch));
        getSkillPool().add(new FootSmother(ch));
        getSkillPool().add(new FootWorship(ch));
        getSkillPool().add(new BreastWorship(ch));
        getSkillPool().add(new CockWorship(ch));
        getSkillPool().add(new PussyWorship(ch));
        getSkillPool().add(new SuccubusSurprise(ch));
        getSkillPool().add(new TemptressHandjob(ch));
        getSkillPool().add(new TemptressBlowjob(ch));
        getSkillPool().add(new TemptressTitfuck(ch));
        getSkillPool().add(new TemptressRide(ch));
        getSkillPool().add(new TemptressStripTease(ch));
        getSkillPool().add(new Blindside(ch));
        getSkillPool().add(new LeechSeed(ch));
        getSkillPool().add(new Beg(ch));
        getSkillPool().add(new Cowardice(ch));
        getSkillPool().add(new Kneel(ch));
        getSkillPool().add(new Dive(ch));
        getSkillPool().add(new Offer(ch));
        getSkillPool().add(new OfferAss(ch));
        getSkillPool().add(new ShamefulDisplay(ch));
        getSkillPool().add(new Stumble(ch));
        getSkillPool().add(new TortoiseWrap(ch));
        getSkillPool().add(new FaerieSwarm(ch));
        getSkillPool().add(new DarkTalisman(ch));
        getSkillPool().add(new HeightenSenses(ch));
        getSkillPool().add(new LewdSuggestion(ch));
        getSkillPool().add(new Suggestion(ch));
        getSkillPool().add(new ImbueFetish(ch));
        getSkillPool().add(new AssJob(ch));
        getSkillPool().add(new TailSuck(ch));
        getSkillPool().add(new ToggleSlimeCock(ch));
        getSkillPool().add(new ToggleSlimePussy(ch));
        getSkillPool().add(new Spores(ch));
        getSkillPool().add(new EngulfedFuck(ch));
        getSkillPool().add(new Pray(ch));
        getSkillPool().add(new Prostrate(ch));
        getSkillPool().add(new DarkKiss(ch));
        getSkillPool().add(new SlimeMimicry(ch));
        getSkillPool().add(new MimicAngel(ch));
        getSkillPool().add(new MimicCat(ch));
        getSkillPool().add(new MimicDryad(ch));
        getSkillPool().add(new MimicSuccubus(ch));
        getSkillPool().add(new MimicWitch(ch));
        getSkillPool().add(new Parasite(ch));
        getSkillPool().add(new Bite(ch));
        getSkillPool().add(new PlaceBlindfold(ch));
        getSkillPool().add(new RipBlindfold(ch));
        getSkillPool().add(new ToggleBlindfold(ch));
        getSkillPool().add(new BunshinAssault(ch));
        getSkillPool().add(new BunshinService(ch));
        getSkillPool().add(new GoodnightKiss(ch));
        getSkillPool().add(new NeedleThrow(ch));
        getSkillPool().add(new StealClothes(ch));
        getSkillPool().add(new Substitute(ch));
        getSkillPool().add(new AttireShift(ch));
        getSkillPool().add(new CheapShot(ch));
        getSkillPool().add(new EmergencyJump(ch));
        getSkillPool().add(new Haste(ch));
        getSkillPool().add(new Rewind(ch));
        getSkillPool().add(new Unstrip(ch));
        getSkillPool().add(new WindUp(ch));
        getSkillPool().add(new ThrowSlime(ch));
        getSkillPool().add(new Edge(ch));
        getSkillPool().add(new SummonYui(ch));
        getSkillPool().add(new Divide(ch));
        getSkillPool().add(new PetThreesome(ch));
        getSkillPool().add(new ReversePetThreesome(ch));
        getSkillPool().add(new PetInitiatedThreesome(ch));
        getSkillPool().add(new PetInitiatedReverseThreesome(ch));
        getSkillPool().add(new FlyCatcher(ch));
        getSkillPool().add(new Honeypot(ch));
        getSkillPool().add(new TakeOffShoes(ch));
        getSkillPool().add(new LaunchHarpoon(ch));
        getSkillPool().add(new ThrowBomb(ch));
        getSkillPool().add(new RemoveBomb(ch));
        getSkillPool().add(new MagLock(ch));
        getSkillPool().add(new Collar(ch));
        getSkillPool().add(new HypnoVisorPlace(ch));
        getSkillPool().add(new HypnoVisorRemove(ch));
        getSkillPool().add(new StripMinor(ch));
        getSkillPool().add(new DemandArousal(ch));
        getSkillPool().add(new Embrace(ch));
        getSkillPool().add(new SuccubusNurse(ch));
        getSkillPool().add(new WingWrap(ch));
        getSkillPool().add(new ComeHither(ch));
        getSkillPool().add(new KiShout(ch));
        getSkillPool().add(new PressurePoint(ch));
        getSkillPool().add(new Deepen(ch));

        getSkillPool().add(new ManipulateFetish(ch));
        getSkillPool().add(new BreastGrowthSuper(ch));
        getSkillPool().add(new Kneel(ch));
        getSkillPool().add(new OfferAss(ch));
    }

    /**
     * */
    public static void buildActionPool() {
        actionPool = new HashSet<>();
        actionPool.add(new Wait());
        actionPool.add(new Locate());
        actionPool.add(new Masturbate());
        actionPool.add(new Disguise());
        actionPool.add(new nightgames.match.actions.Struggle());
        newTrapPool().forEach(t -> actionPool.add(new SetTrap(t)));
    }

    public static Set<Trap> newTrapPool() {
        Set<Trap> trapPool = new HashSet<>();
        trapPool.add(new Alarm());
        trapPool.add(new Tripwire());
        trapPool.add(new Snare());
        trapPool.add(new SpringTrap());
        trapPool.add(new AphrodisiacTrap());
        trapPool.add(new DissolvingTrap());
        trapPool.add(new Decoy());
        trapPool.add(new Spiderweb());
        trapPool.add(new EnthrallingTrap());
        trapPool.add(new IllusionTrap());
        trapPool.add(new StripMine());
        trapPool.add(new TentacleTrap());
        trapPool.add(new RoboWeb());
        return trapPool;
    }

    public static void buildFeatPool() {
        featPool = new HashSet<>();
        for (Trait trait : Trait.values()) {
            if (trait.isFeat()) {
                featPool.add(trait);
            }
        }
    }

    public static void buildModifierPool() {
        modifierPool = new HashSet<>();
        modifierPool.add(new NoModifier());
        modifierPool.add(new NoItemsModifier());
        modifierPool.add(new NoToysModifier());
        modifierPool.add(new NoRecoveryModifier());
        modifierPool.add(new NudistModifier());
        modifierPool.add(new PacifistModifier());
        modifierPool.add(new UnderwearOnlyModifier());
        modifierPool.add(new VibrationModifier());
        modifierPool.add(new VulnerableModifier());
        modifierPool.add(new MayaModifier());           //Checks its own condition, so it should be fine, here. - DSM
    }

    public static Set<Action> getActions() {
        return actionPool;
    }

    public static List<Trait> getFeats(Character c) {
        List<Trait> a = getTraitRequirements().availTraits(c);
        a.sort(Comparator.comparing(Trait::toString));
        return a;
    }

    public static Time getTime() {
        return time;
    }

    public static Match getMatch() {
        return match;
    }

    public static Daytime getDay() {
        return day;
    }

    public static void startDay() {
        match = null;
        day = new Daytime(human);
        day.plan();
    }

    /**
     * Sets the time to DAY, since the order of operations changed and manual end-of-match
     * saves got flagged as NIGHT instead.
     */
    public static void endNightForSave() {
        time = Time.DAY;
    }
    
    public static void endNight() {
        double level = 0;
        int maxLevelTracker = 0;
        
        for (Character player : players) {
            player.stamina.renew();
            player.arousal.renew();
            player.getMojo().renew();
            player.change();
            level += player.getProgression().getLevel();
            if (!player.has(Trait.unnaturalgrowth) && !player.has(Trait.naturalgrowth)) {
                maxLevelTracker = Math.max(player.getProgression().getLevel(), maxLevelTracker);
            }
        }
        final int maxLevel = maxLevelTracker;
        players.stream().filter(c -> c.has(Trait.naturalgrowth)).filter(c -> c.getProgression().getLevel() < maxLevel + 2).forEach(c -> {
            while (c.getProgression().getLevel() < maxLevel + 2) {
                c.ding(null);
            }
        });
        /*  
         * TODO: CONSIDER USING THIS INSTEAD - requires a accompanying trait naturalgrowth  to all regular characters or some similar marking
         * players.stream().filter(c -> c.has(Trait.naturalgrowth)).filter(c -> c.getLevel() < maxLevel - 1).forEach(c -> {
                while (c.getLevel() < maxLevel - 1) {
                    c.ding(null);
                }
           });
         * 
         * 
         * */
        
        
        players.stream().filter(c -> c.has(Trait.unnaturalgrowth)).filter(c -> c.getProgression().getLevel() < maxLevel + 5)
                        .forEach(c -> {
                            while (c.getProgression().getLevel() < maxLevel + 5) {
                                c.ding(null);
                            }
                        });

        level /= players.size();

        for (Character rested : resting) {
            rested.gainXP(100 + Math.max(0, (int) Math.round(10 * (level - rested.getProgression().getLevel()))));
        }
        date++;
        time = Time.DAY;
        if (Global.checkFlag(Flag.autosave)) {
            Global.autoSave();
        }
        Global.gui().endMatch();
    }
    
    private static Set<Character> pickCharacters(Collection<Character> avail, Collection<Character> added, int size) {
        List<Character> randomizer = avail.stream()
                        .filter(c -> !c.human())
                        .filter(c -> !added.contains(c))
                        .collect(Collectors.toList());
        Collections.shuffle(randomizer);
        Set<Character> results = new HashSet<>(added);
        results.addAll(randomizer.subList(0, Math.min(Math.max(0, size - results.size())+1, randomizer.size())));
        return results;
    }

    public static void endDay() {
        day = null;
        time = Time.NIGHT;
        if (checkFlag(Flag.autosave)) {
            autoSave();
        }
        startNight();
    }

    
    public static void startNight() {
        var currentMatchType = MatchType.NORMAL;
        if (getPlayer().getProgression().getLevel() >= 15 && random(10) < 2) {
            currentMatchType = MatchType.FTC;
        }
        currentMatchType.runPrematch();
    }

    public static List<Character> getMatchParticipantsInAffectionOrder() {
        if (match == null) {
            return Collections.emptyList();
        }
        return getInAffectionOrder(match.getCombatants().stream()
                        .filter(c -> !c.human()).collect(Collectors.toList()));
    }

    public static List<Character> getInAffectionOrder(List<Character> viableList) {
        List<Character> results = new ArrayList<>(viableList);
        results.sort((a, b) -> a.getAffection(getPlayer()) - b.getAffection(getPlayer()));
        return results;
    }

    private static class MatchCommonData {
        Set<Character> lineup;
        List<Character> participants;
    }

    private static MatchCommonData setUpMatchCommon() {
        assert day == null;
        var result = new MatchCommonData();
        result.lineup = new HashSet<>(debugChars);
        Character lover = null;
        int maxaffection = 0;
        for (Character player : players) {
            player.stamina.renew();
            player.arousal.renew();
            player.getMojo().renew();
            player.willpower.renew();
            if (player.getPure(Attribute.Science) > 0) {
                player.chargeBattery();
            }
            if (human.getAffection(player) > maxaffection && !checkCharacterDisabledFlag(player)) {
                maxaffection = human.getAffection(player);
                lover = player;
            }
        }
        result.participants = new ArrayList<>();
        // Disable characters flagged as disabled
        for (Character c : players) {
            // Disabling the player wouldn't make much sense, and there's no PlayerDisabled flag.
            if (c.type.equals("Player") || !checkCharacterDisabledFlag(c)) {
                result.participants.add(c);
            }
        }
        if (lover != null) {
            result.lineup.add(lover);
        }
        result.lineup.add(human);
        return result;
    }

    public static void setUpFTCMatch(BaseModifier matchmod) {
        var data = setUpMatchCommon();
        Character prey = ((FTCModifier) matchmod).getPrey();
        if (!prey.human()) {
            data.lineup.add(prey);
        }
        data.lineup = pickCharacters(data.participants, data.lineup, LINEUP_SIZE);
        resting = new HashSet<>(players);
        resting.removeAll(data.lineup);
        match = MatchType.FTC.buildMatch(data.lineup, matchmod);
    }

    /**Sets up a match by assigning the player lineup.
     *
     * FIXME: Also includes code that checks for Maya and adds her. This should be extracted out into some kind of event. - DSM*/
    public static void setUpMatch(MatchType matchType, BaseModifier matchmod) {
        var data = setUpMatchCommon();
        //TODO: This really should be taken out of this in favor of something that processes extra events of this kind. - DSM
        if (matchmod.name().equals(MayaModifier.NAME)) {
            if (!checkFlag(Flag.Maya)) {
                newChallenger(new Maya(human.getProgression().getLevel()));
                flag(Flag.Maya);
            }
            NPC maya = Optional.ofNullable(getNPC("Maya")).orElseThrow(() -> new IllegalStateException(
                            "Maya data unavailable when attempting to add her to lineup."));
            data.lineup.add(maya);
            data.lineup = pickCharacters(data.participants, data.lineup, LINEUP_SIZE);
            resting = new HashSet<>(players);
            resting.removeAll(data.lineup);
            maya.gain(Item.Aphrodisiac, 10);
            maya.gain(Item.DisSol, 10);
            maya.gain(Item.Sedative, 10);
            maya.gain(Item.Lubricant, 10);
            maya.gain(Item.BewitchingDraught, 5);
            maya.gain(Item.FeralMusk, 10);
            maya.gain(Item.ExtremeAphrodisiac, 5);
            maya.gain(Item.ZipTie, 10);
            maya.gain(Item.SuccubusDraft, 10);
            maya.gain(Item.Lactaid, 5);
            maya.gain(Item.Handcuffs, 5);
            maya.gain(Item.Onahole2);
            maya.gain(Item.Dildo2);
            maya.gain(Item.Strapon2);
            match = Match.newMatch(data.lineup, matchmod);
        } else if (data.participants.size() > LINEUP_SIZE) {
            data.lineup = pickCharacters(data.participants, data.lineup, LINEUP_SIZE);
            resting = new HashSet<>(players);
            resting.removeAll(data.lineup);
            match = matchType.buildMatch(data.lineup, matchmod);
        } else {
            match = matchType.buildMatch(data.participants, matchmod);
        }
        match.start();
    }

    public static String colorizeMessage(Character speaker, String message) {
        if (message.length() > 0) {
            if (speaker.human()) {
                return "<br/><font color='rgb(200,200,255)'>" + message + "<font color='white'>";
            } else if (speaker.isPet() && speaker.isPetOf(Global.getPlayer())) {
                return "<br/><font color='rgb(130,225,200)'>" + message + "<font color='white'>";
            } else if (speaker.isPet()) {
                return "<br/><font color='rgb(210,130,255)'>" + message + "<font color='white'>";
            } else {
                return "<br/><font color='rgb(255,200,200)'>" + message + "<font color='white'>";
            }
        }
        return "";
    }

    public static String gainSkills(Character c) {
        String message = "";
        if (c.getPure(Attribute.Dark) >= 6 && !c.has(Trait.darkpromises)) {
            c.add(Trait.darkpromises);
        } else if (!(c.getPure(Attribute.Dark) >= 6) && c.has(Trait.darkpromises)) {
            c.remove(Trait.darkpromises);
        }
        boolean pheromonesRequirements = c.getPure(Attribute.Animism) >= 2 || c.has(Trait.augmentedPheromones);
        if (pheromonesRequirements && !c.has(Trait.pheromones)) {
            c.add(Trait.pheromones);
        } else if (!pheromonesRequirements && c.has(Trait.pheromones)) {
            c.remove(Trait.pheromones);
        }
        return message;
    }

    public static void learnSkills(Character c) {
        for (Skill skill : getSkillPool()) {
            c.learn(skill);
        }
    }

    public static String capitalizeFirstLetter(String original) {
        if (original == null) {
            return "";
        }
        if (original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public static NPC getNPCByType(String type) {
        NPC results = characterPool.get(type);
        if (results == null) {
            System.err.println("failed to find NPC for type " + type);
        }
        return results;
    }

    public static Character getCharacterByType(String type) {
        if (type.equals(human.getType())) {
            return human;
        }
        return getNPCByType(type);
    }

    public static void flag(String f) {
        flags.add(f);
    }

    public static void unflag(String f) {
        flags.remove(f);
    }

    public static void flag(Flag f) {
        flags.add(f.name());
    }

    public static void unflag(Flag f) {
        flags.remove(f.name());
    }

    public static void setFlag(String f, boolean value) {
        if (value) { 
            flag(f);
        } else {
            unflag(f);
        }
    }

    public static void setFlag(Flag f, boolean value) {
        if (value) { 
            flags.add(f.name()); 
        } else { 
            flags.remove(f.name()); 
        }
    }

    public static boolean checkFlag(Flag f) {
        return flags.contains(f.name());
    }

    public static boolean checkFlag(String key) {
        return flags.contains(key);
    }

    public static float getValue(Flag f) {
        if (!counters.containsKey(f.name())) {
            return 0;
        } else {
            return counters.get(f.name());
        }
    }

    public static void modCounter(Flag f, float inc) {
        counters.put(f.name(), getValue(f) + inc);
    }

    public static void setCounter(Flag f, float val) {
        counters.put(f.name(), val);
    }

    public static void autoSave() {
        save(new File("./auto.ngs"));
    }

    public static void saveWithDialog() {
        Optional<File> file = gui().askForSaveFile();
        if (file.isPresent()) {
            save(file.get());
        }
    }

    protected static SaveData saveData() {
        SaveData data = new SaveData();
        data.players.addAll(players);
        data.flags.addAll(flags);
        data.counters.putAll(counters);
        data.time = time;
        data.date = date;
        data.fontsize = gui.fontsize;
        return data;
    }

    public static void save(File file) {
        SaveData data = saveData();
        JsonObject saveJson = data.toJson();

        try (JsonWriter saver = new JsonWriter(new FileWriter(file))) {
            saver.setIndent("  ");
            JsonUtils.getGson().toJson(saveJson, saver);
        } catch (IOException | JsonIOException e) {
            System.err.println("Could not save file " + file + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static NpcConfiguration findNpcConfig(String type, StartConfiguration startConfig) {
        return startConfig!= null ? startConfig.findNpcConfig(type) : null;
    }

    /**Rebuilds the character pool using the starting configuration. 
     * 
     * TODO: Refactor into function and unify with CustomNPC handling.
     * 
     * 
     * 
     * */
    public static void rebuildCharacterPool(StartConfiguration startConfig) {
        characterPool = new HashMap<>();
        debugChars.clear();

        NpcConfiguration commonConfig =
                        startConfig != null ? startConfig.npcCommon : null;

        try (InputStreamReader reader = new InputStreamReader(
                        ResourceLoader.getFileResourceAsStream("characters/included.json"))) {
            JsonArray characterSet = JsonUtils.rootJson(reader).getAsJsonArray();
            for (JsonElement element : characterSet) {
                String name = element.getAsString();
                try {
                    DataBackedNPCData data = JsonSourceNPCDataLoader
                                    .load(ResourceLoader.getFileResourceAsStream("characters/" + name));
                    NpcConfiguration npcConfig = findNpcConfig(CustomNPC.TYPE_PREFIX + data.getName(), startConfig);
                    BasePersonality npc = new CustomNPC(data, npcConfig, commonConfig);
                    characterPool.put(npc.character.getType(), npc.character);
                    System.out.println("Loaded " + name);
                } catch (JsonParseException e1) {
                    System.err.println("Failed to load NPC " + name);
                    e1.printStackTrace();
                }
            }
        } catch (JsonParseException | IOException e1) {
            System.err.println("Failed to load character set");
            e1.printStackTrace();
        }

        // TODO: Refactor into function and unify with CustomNPC handling.
        BasePersonality cassie = new Cassie(findNpcConfig("Cassie", startConfig), commonConfig);
        BasePersonality angel = new Angel(findNpcConfig("Angel", startConfig), commonConfig);
        BasePersonality reyka = new Reyka(findNpcConfig("Reyka", startConfig), commonConfig);
        BasePersonality kat = new Kat(findNpcConfig("Kat", startConfig), commonConfig);
        BasePersonality mara = new Mara(findNpcConfig("Mara", startConfig), commonConfig);
        BasePersonality jewel = new Jewel(findNpcConfig("Jewel", startConfig), commonConfig);
        BasePersonality airi = new Airi(findNpcConfig("Airi", startConfig), commonConfig);
        BasePersonality eve = new Eve(findNpcConfig("Eve", startConfig), commonConfig);
        BasePersonality maya = new Maya(1, findNpcConfig("Maya", startConfig), commonConfig);
        BasePersonality yui = new Yui(findNpcConfig("Yui", startConfig), commonConfig);
        characterPool.put(cassie.character.getType(), cassie.character);
        characterPool.put(angel.character.getType(), angel.character);
        characterPool.put(reyka.character.getType(), reyka.character);
        characterPool.put(kat.character.getType(), kat.character);
        characterPool.put(mara.character.getType(), mara.character);
        characterPool.put(jewel.character.getType(), jewel.character);
        characterPool.put(airi.character.getType(), airi.character);
        characterPool.put(eve.character.getType(), eve.character);
        characterPool.put(maya.character.getType(), maya.character);
        characterPool.put(yui.character.getType(), yui.character);
    }
    
    public static void loadWithDialog() {
        JFileChooser dialog = new JFileChooser("./");
        FileFilter savesFilter = new FileNameExtensionFilter("Nightgame Saves", "ngs");
        dialog.addChoosableFileFilter(savesFilter);
        dialog.setFileFilter(savesFilter);
        dialog.setMultiSelectionEnabled(false);
        int rv = dialog.showOpenDialog(gui);
        if (rv != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = dialog.getSelectedFile();
        if (!file.isFile()) {
            file = new File(dialog.getSelectedFile().getAbsolutePath() + ".ngs");
            if (!file.isFile()) {
                // not a valid save, abort
                JOptionPane.showMessageDialog(gui, "Nightgames save file not found", "File not found",
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        load(file);
    }

    protected static void resetForLoad() {
        players.clear();
        flags.clear();
        gui.clearText();
        human = new Player("Dummy", gui);
        gui.purgePlayer();
        buildSkillPool(human);
        Clothing.buildClothingTable();
        rebuildCharacterPool(null);
        day = null;
    }

    public static void load(File file) {
        resetForLoad();

        JsonObject object;
        try (Reader loader = new InputStreamReader(new FileInputStream(file))) {
            object = new JsonParser().parse(loader).getAsJsonObject();

        } catch (IOException e) {
            e.printStackTrace();
            // Couldn't load data; just get out
            return;
        }
        SaveData data = new SaveData(object);
        loadData(data);
        gui.populatePlayer(human);
        if (time == Time.DAY) {
            startDay();
        } else {
            startNight();
        }
    }

    /**
     * Loads game state data into static fields from SaveData object.
     *
     * @param data A SaveData object, as loaded from save files.
     */
    protected static void loadData(SaveData data) {
        players.addAll(data.players);
        players.stream().filter(c -> c instanceof NPC).forEach(
                        c -> characterPool.put(c.type, (NPC) c));
        flags.addAll(data.flags);
        counters.putAll(data.counters);
        date = data.date;
        time = data.time;
        gui.fontsize = data.fontsize;
    }

    public static Set<Character> everyone() {
        return players;
    }

    public static boolean newChallenger(BasePersonality challenger) {
        if (!players.contains(challenger.character)) {
            int targetLevel = human.getProgression().getLevel();
            while (challenger.character.getProgression().getLevel() <= targetLevel) {
                challenger.character.ding(null);
            }
            players.add(challenger.character);
            return true;
        } else {
            return false;
        }
    }

    public static NPC getNPC(String name) {
        for (Character c : allNPCs()) {
            if (c.type.equalsIgnoreCase(name)) {
                return (NPC) c;
            }
        }
        System.err.println("NPC \"" + name + "\" is not loaded.");
        return null;
    }
    
    public static void main(String[] args) {
        hookLogwriter();
        init();
    }
    
    public static void init() {
        makeGUI();
        gui.createCharacter();
    }
    
    public static void initForTesting() {
        makeTestGUI();
        gui.createCharacter();
    }

    public static void hookLogwriter() {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            System.err.println(stacktrace);
        });
    }
    
    /**Returns the introductory text. 
     * 
     * NOTE: This should probably be moved into something more modular. -DSM
     * */
    public static String getIntro() {
        return "You don't really know why you're going to the Student Union in the middle of the night."
                        + " You'd have to be insane to accept the invitation you received this afternoon."
                        + " Seriously, someone is offering you money to sexfight a bunch of other students?"
                        + " You're more likely to get mugged (though you're not carrying any money) or murdered if you show up."
                        + " Best case scenario, it's probably a prank for gullible freshmen."
                        + " You have no good reason to believe the invitation is on the level, but here you are, walking into the empty Student Union."
                        + "\n\n" + "Not quite empty, it turns out."
                        + " The same woman who approached you this afternoon greets you and brings you to a room near the back of the building."
                        + " Inside, you're surprised to find three quite attractive girls."
                        + " After comparing notes, you confirm they're all freshmen like you and received the same invitation today."
                        + " You're surprised, both that these girls would agree to such an invitation."
                        + " For the first time, you start to believe that this might actually happen."
                        + " After a few minutes of awkward small talk (though none of these girls seem self-conscious about being here), the woman walks in again leading another girl."
                        + " Embarrassingly you recognize the girl, named Cassie, who is a classmate of yours, and who you've become friends with over the past couple weeks."
                        + " She blushes when she sees you and the two of you consciously avoid eye contact while the woman explains the rules of the competition."
                        + "\n\n" + "There are a lot of specific points, but the rules basically boil down to this: "
                        + " competitors move around the empty areas of the campus and engage each other in sexfights."
                        + " When one competitor orgasms and doesn't have the will to go on, the other gets a point and can claim the loser's clothes."
                        + " Those two players are forbidden to engage again until the loser gets a replacement set of clothes at either the Student Union or the first floor of the dorm building."
                        + " It seems to be customary, but not required, for the loser to get the winner off after a fight, when it doesn't count."
                        + " After three hours, the match ends and each player is paid for each opponent they defeat, each set of clothes turned in, and a bonus for whoever scores the most points."
                        + "\n\n"
                        + "After the explanation, she confirms with each participant whether they are still interested in participating."
                        + " Everyone agrees." + " The first match starts at exactly 10:00.";
    }

    public static void reset() {
        players.clear();
        flags.clear();
        day = null;
        match = null;
        human = new Player("Dummy", null);
        gui.purgePlayer();
        xpRate = 1.0;
        gui.createCharacter();
    }

    public static boolean inGame() {
        return !players.isEmpty();
    }

    public static boolean characterTypeInGame(String type) {
        return players.stream().anyMatch(c -> type.equals(c.type));
    }

    public static float randomfloat() {
        return (float) rng.nextDouble();
    }

    @SafeVarargs
    public static <T> Optional<T> pickRandom(T ... arr) {
        if (arr == null || arr.length == 0) return Optional.empty();
        return Optional.of(arr[Global.random(arr.length)]);
    }

    public static <T> Optional<T> pickRandom(List<T> list) {
        if (list == null || list.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(list.get(random(list.size())));
        }
    }
    
    public static <T> Optional<T> pickWeighted(Collection<Tuple2<T, Double>> weights) {
        // Normalize the weights so they sum to 1.0, sort them low->high,
        // then partition the range [0, 1) such that values with greater
        // weight get a larger 'section'. Finally, pick a random value in
        // [0, 1) and see what partition it's in. Return the corresponding value.
        
        final double totalWeight = weights.stream().map(t -> t.b).reduce(0.0, Double::sum);
        final double threshold = rng.nextDouble();
        final var wrapper = new Object() {
            double sumSoFar = 0.0;
        };

        return weights.stream()
                .map(t -> new Tuple2<>(t.a, t.b / totalWeight))
                .sorted(Comparator.comparing(t -> t.b))
                .dropWhile(t -> {
                    if (t.b + wrapper.sumSoFar >= threshold) {
                        return false;
                    }
                    wrapper.sumSoFar += t.b;
                    return true;
                })
                .findFirst().map(t -> t.a);
    }

    public static int getDate() {
        return date;
    }

    interface MatchAction {
        String replace(Character self, String first, String second, String third);
    }

    /**Builds the parser responsible for taking special tags and forming them into the correct english word.
     * 
     * 
     * 
     * */
    public static void buildParser() {
        matchActions = new HashMap<>();
        matchActions.put("possessive", (self, first, second, third) -> {
            if (self != null) {
                return self.possessiveAdjective();
            }
            return "";
        });
        matchActions.put("name-possessive", (self, first, second, third) -> {
            if (self != null) {
                return self.nameOrPossessivePronoun();
            }
            return "";
        });
        matchActions.put("name", (self, first, second, third) -> {
            if (self != null) {
                return self.getName();
            }
            return "";
        });
        matchActions.put("subject-action", (self, first, second, third) -> {
            if (self != null && third != null) {
                String verbs[] = third.split("\\|");
                if (verbs.length > 1) {
                    return self.subjectAction(verbs[0], verbs[1]);
                } else {
                    return self.subjectAction(verbs[0]);
                }
            }
            return "";
        });
        matchActions.put("pronoun-action", (self, first, second, third) -> {
            if (self != null && third != null) {
                String verbs[] = third.split("\\|");
                if (verbs.length > 1) {
                    return self.pronoun() + " " + self.action(verbs[0], verbs[1]);
                } else {
                    return self.pronoun() + " " + self.action(verbs[0]);
                }
            }
            return "";
        });
        matchActions.put("action", (self, first, second, third) -> {
            if (self != null && third != null) {
                String verbs[] = third.split("\\|");
                if (verbs.length > 1) {
                    return self.action(verbs[0], verbs[1]);
                } else {
                    return self.action(verbs[0]);
                }
            }
            return "";
        });
        matchActions.put("if-female", (self, first, second, third) -> {
            if (self != null && third != null) {
                return self.useFemalePronouns() ? third : "";
            }
            return "";
        });
        matchActions.put("if-male", (self, first, second, third) -> {
            if (self != null && third != null) {
                return self.useFemalePronouns() ? "" : third;
            }
            return "";
        });
        matchActions.put("if-human", (self, first, second, third) -> {
            if (self != null && third != null) {
                return self.human() ? third : "";
            }
            return "";
        });

        matchActions.put("if-nonhuman", (self, first, second, third) -> {
            if (self != null && third != null) {
                return !self.human() ? third : "";
            }
            return "";
        });
        matchActions.put("subject", (self, first, second, third) -> {
            if (self != null) {
                return self.subject();
            }
            return "";
        });
        matchActions.put("direct-object", (self, first, second, third) -> {
            if (self != null) {
                return self.objectPronoun();
            }
            return "";
        });
        matchActions.put("name-do", (self, first, second, third) -> {
            if (self != null) {
                return self.nameDirectObject();
            }
            return "";
        });
        matchActions.put("body-part", (self, first, second, third) -> {
            if (self != null && third != null) {
                BodyPart part = self.body.getRandom(third);
                if (part == null && third.equals(CockPart.TYPE) && self.has(Trait.strapped)) {
                    part = new StraponPart();
                }
                if (part != null) {
                    return part.describe(self);
                }
            }
            return "";
        });
        matchActions.put("pronoun", (self, first, second, third) -> {
            if (self != null) {
                return self.pronoun();
            }
            return "";
        });
        matchActions.put("reflective", (self, first, second, third) -> {
            if (self != null) {
                return self.reflexivePronoun();
            }
            return "";
        });

        matchActions.put("main-genitals", (self, first, second, third) -> {
            if (self != null) {
                if (self.hasDick()) {
                    return "dick";
                } else if (self.hasPussy()) {
                    return "pussy";
                } else {
                    return "crotch";
                }
            }
            return "";
        });

        matchActions.put("balls-vulva", (self, first, second, third) -> {
            if (self != null) {
                if (self.hasBalls()) {
                    return "testicles";
                } else if (self.hasPussy()) {
                    return "vulva";
                } else {
                    return "crotch";
                }
            }
            return "";
        });

        matchActions.put("master", (self, first, second, third) -> {
            if (self.useFemalePronouns()) {
                return "mistress";
            } else {
                return "master";
            }
        });

        matchActions.put("mister", (self, first, second, third) -> {
            if (self.useFemalePronouns()) {
                return "miss";
            } else {
                return "mister";
            }
        });

        matchActions.put("true-name", (self, first, second, third) -> {
            return self.getTrueName();
        });

        matchActions.put("girl", (self, first, second, third) -> {
            return Shorthand.guyOrGirl(self);
        });
        matchActions.put("guy", (self, first, second, third) -> {
            return Shorthand.guyOrGirl(self);
        });
        matchActions.put("man", (self, first, second, third) -> {
            return self.useFemalePronouns() ? "woman" : "man";
        });
        matchActions.put("boy", (self, first, second, third) -> {
            return Shorthand.boyOrGirl(self);
        });
        matchActions.put("poss-pronoun", (self, first, second, third) -> {
            if (self != null) {
                return self.possessivePronoun();
            }
            return "";
        });
        matchActions.put("reflexive", (self, first, second, third) -> {
           if (self.useFemalePronouns()) {
               return "herself";
           } else {
               return "himself";
           }
        });
    }

    /**Returns a formatted string for use with the tag parsing system. 
     * */
    public static String format(String format, Character self, Character target, Object... strings) {
        // pattern to find stuff like {word:otherword:finalword} in strings
        Pattern p = Pattern.compile("\\{((?:self)|(?:other)|(?:master))(?::([^:}]+))?(?::([^:}]+))?\\}");
        format = String.format(format, strings);

        Matcher matcher = p.matcher(format);
        StringBuffer b = new StringBuffer();
        while (matcher.find()) {
            String first = matcher.group(1);
            String second = matcher.group(2);
            if (second == null) {
                second = "";
            }
            String third = matcher.group(3);
            Character character = null;
            if (first.equals("self")) {
                character = self;
            } else if (first.equals("other")) {
                character = target;
            } else if (first.equals("master") && self instanceof PetCharacter) {
                character = ((PetCharacter)self).getSelf().owner();
            }
            String replacement = matcher.group(0);
            boolean caps = false;
            if (second.toUpperCase().equals(second)) {
                second = second.toLowerCase();
                caps = true;
            }
            MatchAction action = matchActions.get(second);

            if (action == null) {
                System.out.println(second);
            }
            if (action != null) {
                replacement = action.replace(character, first, second, third);
                if (caps) {
                    replacement = Global.capitalizeFirstLetter(replacement);
                }
            }
            matcher.appendReplacement(b, replacement);
        }
        matcher.appendTail(b);
        return b.toString();
    }

    public static Character noneCharacter() {
        return noneCharacter;
    }

    public static double randomdouble() {
        return rng.nextDouble();
    }

    public static double randomdouble(double to) {
        return rng.nextDouble() * to;
    }

    public static String prependPrefix(String prefix, String fullDescribe) {
        if (prefix.equals("a ") && "aeiou".contains(fullDescribe.substring(0, 1).toLowerCase())) {
            return "an " + fullDescribe;
        }
        return prefix + fullDescribe;
    }

    public static Collection<NPC> allNPCs() {
        return characterPool.values();
    }

    private static DecimalFormat formatter = new DecimalFormat("#.##");

    public static String formatDecimal(double val) {
        return formatter.format(val);
    }

    public static Set<Skill> getSkillPool() {
        return skillPool;
    }

    public static Set<BaseModifier> getModifierPool() {
        return modifierPool;
    }

    public static HashSet<Character> getParticipants() {
        return new HashSet<>(players);
    }

    public static int clamp(int number, int min, int max) {
        return Math.min(Math.max(number, min), max);
    }

    public static double clamp(double number, double min, double max) {
        return Math.min(Math.max(number, min), max);
    }

    public static long randomlong() {
        return rng.nextLong();
    }

    public static Character getParticipantsByName(String name) {
        return players.stream().filter(c -> c.getTrueName().equals(name)).findAny().get();
    }

    private static String DISABLED_FORMAT = "%sDisabled";
    private static Random FROZEN_RNG = new Random();
    public static boolean checkCharacterDisabledFlag(Character self) {
        return checkFlag(String.format(DISABLED_FORMAT, self.getTrueName()));
    }

    public static void setCharacterDisabledFlag(Character self) {
        flag(String.format(DISABLED_FORMAT, self.getTrueName()));
    }    

    public static void unsetCharacterDisabledFlag(Character self) {
        unflag(String.format(DISABLED_FORMAT, self.getTrueName()));
    }

    public static TraitTree getTraitRequirements() {
        return traitRequirements;
    }

    public static void setTraitRequirements(TraitTree traitRequirements) {
        Global.traitRequirements = traitRequirements;
    }

    public static void updateIfCombat(Combat c) {
        if (c != null) {
            c.updateMessage();
        }
    }

	public static void writeIfCombat(Combat c, Character self, String string) {
	    if (c != null) {
	        c.write(self, string);
	    } else if (self.human()) {
			gui().message(string);
		}
	}

	public static Optional<String> getFlagStartingWith(Collection<String> collection,
	                String start) {
        return collection.stream().filter(s -> s.startsWith(start)).findFirst();
    }
	

	/**
	 * TODO Huge hack to freeze status descriptions.
	 */
    public static void freezeRNG() {
        FROZEN_RNG = rng;
        rng = new Random(0);
    }

    /**
     * TODO Huge hack to freeze status descriptions.
     */
    public static void unfreezeRNG() {
        FROZEN_RNG = new Random();
        rng = FROZEN_RNG;
    }

    public static boolean randomBool() {
        return rng.nextBoolean();
    }
}
