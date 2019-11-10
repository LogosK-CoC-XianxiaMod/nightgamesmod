package nightgames.characters.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nightgames.characters.Attribute;
import nightgames.characters.CharacterSex;
import nightgames.characters.Emotion;
import nightgames.characters.Growth;
import nightgames.characters.MaxAttribute;
import nightgames.characters.Plan;
import nightgames.characters.PreferredAttribute;
import nightgames.characters.Trait;
import nightgames.characters.body.Body;
import nightgames.characters.custom.effect.CustomEffect;
import nightgames.characters.custom.effect.MoneyModEffect;
import nightgames.items.Item;
import nightgames.items.ItemAmount;
import nightgames.items.clothing.Clothing;
import nightgames.json.JsonUtils;
import nightgames.requirements.JsonRequirementLoader;
import nightgames.skills.Skill;
import nightgames.stance.Stance;
import nightgames.status.Stsflag;
import nightgames.utilities.DebugHelper;
import org.jtwig.JtwigTemplate;

public class JsonSourceNPCDataLoader {
    private static JsonRequirementLoader requirementLoader = new JsonRequirementLoader();

    protected static void loadResources(JsonObject resources, Stats stats) {
        stats.stamina = resources.get("stamina").getAsInt();
        stats.arousal = resources.get("arousal").getAsInt();
        stats.mojo = resources.get("mojo").getAsInt();
        stats.willpower = resources.get("willpower").getAsInt();
    }

    public static DataBackedNPCData load(InputStream in) throws JsonParseException {
        JsonObject object = JsonUtils.rootJson(new InputStreamReader(in)).getAsJsonObject();
        return load(object);
    }

    public static DataBackedNPCData load(JsonObject object) {
        DataBackedNPCData data = new DataBackedNPCData();
        data.name = object.get("name").getAsString();
        data.type = object.get("type").getAsString();
        data.trophy = Item.valueOf(object.get("trophy").getAsString());
        data.plan = Plan.valueOf(object.get("plan").getAsString());

        // load outfit
        JsonObject outfit = object.getAsJsonObject("outfit");
        JsonArray top = outfit.getAsJsonArray("top");
        for (JsonElement clothing : top) {
            data.top.push(Clothing.getByID(clothing.getAsString()));
        }
        JsonArray bottom = outfit.getAsJsonArray("bottom");
        for (JsonElement clothing : bottom) {
            data.bottom.push(Clothing.getByID(clothing.getAsString()));
        }

        // load stats
        JsonObject stats = object.getAsJsonObject("stats");
        // load base stats
        JsonObject baseStats = stats.getAsJsonObject("base");
        data.stats.level = baseStats.get("level").getAsInt();
        // load attributes
        data.stats.attributes.putAll(JsonUtils
                        .mapFromJson(baseStats.getAsJsonObject("attributes"), Attribute.class, Integer.class));

        loadResources(baseStats.getAsJsonObject("resources"), data.stats);
        loadTraits(baseStats.getAsJsonArray("traits"), data.stats.traits);
        data.setGrowth(new Growth(stats.getAsJsonObject("growth")));
        loadPreferredAttributes(stats.getAsJsonObject("growth").getAsJsonArray("preferredAttributes"),
                        data.preferredAttributes);
        loadItems(object.getAsJsonObject("items"), data);
        loadTemplates(object.getAsJsonObject("templates"), data.templates);
        loadAllLines(object.getAsJsonObject("lines"), data.characterLines);
        loadPortraits(object.getAsJsonObject("portraits"), data.portraitMap);
        loadRecruitment(object.getAsJsonObject("recruitment"), data.recruitment);
        data.body = Body.load(object.getAsJsonObject("body"), null);
        data.sex = CharacterSex.valueOf(object.get("sex").getAsString());

        JsonUtils.getOptionalArray(object, "ai-modifiers").ifPresent(arr -> loadAiModifiers(arr, data.aiModifiers));

        JsonUtils.getOptional(object, "start").map(JsonElement::getAsBoolean).ifPresent(b -> data.isStartCharacter = b);

        data.aiModifiers.setMalePref(JsonUtils.getOptional(object, "male-pref").map(JsonElement::getAsDouble));

        JsonUtils.getOptionalArray(object, "comments").ifPresent(arr -> loadComments(arr, data));

        return data;
    }

    protected static void loadRecruitment(JsonObject object, RecruitmentData recruitment) {
        recruitment.introduction = object.get("introduction").getAsString();
        recruitment.action = object.get("action").getAsString();
        recruitment.confirm = object.get("confirm").getAsString();
        recruitment.requirement = requirementLoader.loadRequirements(object.getAsJsonObject("requirements"));
        loadEffects(object.getAsJsonArray("cost"), recruitment.effects);
    }

    protected static void loadEffects(JsonArray jsonArray, List<CustomEffect> effects) {
        for (JsonElement element : jsonArray) {
            JsonObject obj = element.getAsJsonObject();
            JsonUtils.getOptional(obj, "modMoney").ifPresent(e -> effects.add(new MoneyModEffect(e.getAsInt())));
        }
    }

    private static void loadAllLines(JsonObject linesObj, Map<String, List<CustomStringEntry>> characterLines) {
        for (Map.Entry<String, JsonElement> e : linesObj.entrySet()) {
            String key = e.getKey();
            List<CustomStringEntry> lines = loadLines(linesObj.getAsJsonArray(key));
            characterLines.put(key, lines);
        }
    }

    private static List<CustomStringEntry> loadLines(JsonArray linesArr) {
        List<CustomStringEntry> entries = new ArrayList<>();
        for (JsonElement element : linesArr) {
            entries.add(readLine(element.getAsJsonObject()));
        }
        return entries;
    }

    protected static CustomStringEntry readLine(JsonObject object) {
        CustomStringEntry entry = new CustomStringEntry(object.get("text").getAsString());
        entry.requirements = JsonUtils.getOptionalObject(object, "requirements")
                        .map(obj -> requirementLoader.loadRequirements(obj)).orElse(new ArrayList<>());
        return entry;
    }

    private static void loadItems(JsonObject obj, DataBackedNPCData data) {
        loadItemsArray(obj.getAsJsonArray("initial"), data.startingItems);
        loadItemsArray(obj.getAsJsonArray("purchase"), data.purchasedItems);
    }

    private static void loadItemsArray(JsonArray arr, List<ItemAmount> items) {
        for (Object mem : arr) {
            JsonObject obj = (JsonObject) mem;
            items.add(readItem(obj));
        }
    }

    protected static ItemAmount readItem(JsonObject obj) {
        return JsonUtils.getGson().fromJson(obj, ItemAmount.class);
    }

    public static void loadGrowthResources(JsonObject object, Growth growth) {
        JsonArray points = object.getAsJsonArray("points");
        int defaultPoints = 3;
        for (int i = 0; i < growth.attributes.length; i++) {
            if (i < points.size()) {
                growth.attributes[i] = points.get(i).getAsInt();
                defaultPoints = growth.attributes[i];
            } else {
                growth.attributes[i] = defaultPoints;
            }
        }
    }

    protected static void loadPreferredAttributes(JsonArray arr, List<PreferredAttribute> preferredAttributes) {
        for (JsonElement element : arr) {
            JsonObject obj = element.getAsJsonObject();
            Attribute att = JsonUtils.getGson().fromJson(obj.get("attribute"), Attribute.class);
            final int max = JsonUtils.getOptional(obj, "max").map(JsonElement::getAsInt).orElse(Integer.MAX_VALUE);
            preferredAttributes.add(new MaxAttribute(att, max));
        }
    }

    public static void loadGrowthTraits(JsonArray arr, Growth growth) {
        for (JsonElement element : arr) {
            JsonObject obj = element.getAsJsonObject();
            Trait trait = JsonUtils.getGson().fromJson(obj.get("trait"), Trait.class);
            if (trait != null) {
                growth.addTrait(obj.get("level").getAsInt(), trait);
            } else {
                System.err.println("Tried to load a null trait into growth!");
                DebugHelper.printStackFrame(3, 1);
            }
        }
    }

    private static void loadTraits(JsonArray array, List<Trait> traits) {
        traits.addAll(JsonUtils.collectionFromJson(array, Trait.class));
    }

     @SuppressWarnings("unchecked")
    protected static void loadAiModifiers(JsonArray arr, AiModifiers mods) {
        for (Object aiMod : arr) {
            JsonObject obj = (JsonObject) aiMod;
            String value = obj.get("value").getAsString();
            double weight = obj.get("weight").getAsFloat();
            String type = obj.get("type").getAsString();
            switch (type) {
                case "skill":
                    try {
                        mods.getAttackMods().put((Class<? extends Skill>) Class.forName(value), weight);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Skill not found: " + value);
                    }
                    break;
                case "position":
                    mods.getPositionMods().put(Stance.valueOf(value), weight);
                    break;
                case "self-status":
                    mods.getSelfStatusMods().put(Stsflag.valueOf(value), weight);
                    break;
                case "opponent-status":
                    mods.getOppStatusMods().put(Stsflag.valueOf(value), weight);
                    break;
                default:
                    throw new IllegalArgumentException("Type of AiModifier must be one of \"skill\", "
                                    + "\"position\", \"self-status\", or \"opponent-status\", " + "but was \"" + type
                                    + "\".");
            }
        }
    }

     private static void loadComments(JsonArray arr, DataBackedNPCData data) {
        arr.forEach(e -> CommentSituation.parseComment(e.getAsJsonObject(), data.comments));
    }

    private static void loadTemplates(JsonObject obj, Map<String, JtwigTemplate> templates) {
        for (var e : obj.entrySet()) {
            templates.put(e.getKey(), JtwigTemplate.classpathTemplate(e.getValue().getAsString()));
        }
    }

    private static void loadPortraits(JsonObject obj, Map<Emotion, String> portraits) {
        obj.entrySet().forEach(e -> {
                portraits.put(Emotion.valueOf(e.getKey()), e.getValue().getAsString());
            });
    }
}
