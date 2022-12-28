package nightgames.start;

import static nightgames.start.ConfigurationUtils.mergeOptionals;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nightgames.characters.CharacterSex;
import nightgames.characters.NPC;
import nightgames.characters.body.CockPart;
import nightgames.characters.body.PussyPart;
import nightgames.json.JsonUtils;

public class NpcConfiguration extends CharacterConfiguration {
    // Optional because NpcConfiguration is used for both NPCs and adjustments common to all NPCs
    protected String type;
    public Optional<Boolean> isStartCharacter;

    public NpcConfiguration() {
        isStartCharacter = Optional.empty();
    }

    /** Makes a new NpcConfiguration from merging two others.
     * @param primaryConfig Will override field values from secondaryConfig.
     * @param secondaryConfig Field values will be overridden by primaryConfig.
     */
    public NpcConfiguration(NpcConfiguration primaryConfig, NpcConfiguration secondaryConfig) {
        super(primaryConfig, secondaryConfig);
        isStartCharacter = mergeOptionals(primaryConfig.isStartCharacter, secondaryConfig.isStartCharacter);
        type = primaryConfig.type;
    }

    public static NpcConfiguration mergeOptionalNpcConfigs(NpcConfiguration primaryConfig,
                    NpcConfiguration secondaryConfig) {
        if (primaryConfig == null) {
            return secondaryConfig;
        } else if (secondaryConfig != null) {
            return new NpcConfiguration(primaryConfig, secondaryConfig);

            } else {
                return primaryConfig;
            }

    }
    public final void apply(NPC base) {
        if (gender.isPresent()) {
            CharacterSex sex = gender.get();
            base.initialGender = sex;
            // If gender is present in config, make genitals conform to it. This will be overridden if config also supplies genitals.
            if (!sex.hasCock()) {
                base.body.removeAll(CockPart.TYPE);
            }
            if (!sex.hasPussy()) {
                base.body.removeAll(PussyPart.TYPE);
            }
            base.body.makeGenitalOrgans(sex);
        } else {
            base.body.makeGenitalOrgans(base.initialGender);
        }
        super.apply(base);
        if (isStartCharacter.isPresent()) {
            base.isStartCharacter = isStartCharacter.get();
        }
    }

    /** Parse fields from the all_npcs section.
     * @param object The configuration from the JSON config file.
     * @return A new NpcConfiguration as specified in the config file.
     */
    public static NpcConfiguration parseAllNpcs(JsonObject object) {
        NpcConfiguration config = new NpcConfiguration();
        config.isStartCharacter = JsonUtils.getOptional(object, "start").map(JsonElement::getAsBoolean);
        config.parseCommon(object);
        return config;
    }

    /** Parse a character-specific NPC config.
     * @param object The configuration from the JSON config file.
     * @return A new NpcConfiguration as specified in the config file.
     */
    public static NpcConfiguration parse(JsonObject object) {
        NpcConfiguration config = NpcConfiguration.parseAllNpcs(object);
        config.type = JsonUtils.getOptional(object, "type").map(JsonElement::getAsString)
                        .orElseThrow(() -> new RuntimeException("Tried parsing NPC without a type."));

        return config;
    }
    @Override public String toString() {return "type: "+type+", start: "+isStartCharacter+"; "+super.toString();}
}
