package nightgames.characters;

import nightgames.nskills.tags.AttributeSkillTag;
import nightgames.nskills.tags.SkillTag;

public enum Attribute {
    Power,
    Seduction,
    Cunning,
    Perception,
    Speed,
    Arcane,
    Science,
    Dark,
    Fetish,
    Animism,
    Ki,
    Bio,
    Divinity,
    Willpower,
    Medicine,
    Technique,
    Submissive,
    Hypnosis,
    Nymphomania,
    Slime,
    Ninjutsu,
    Temporal;

    private final SkillTag skillTag;
    private Attribute() {
        skillTag = new AttributeSkillTag(this);
    }
    
    public SkillTag getSkillTag() {
        return skillTag;
    }

    public static boolean isBasic(Character self, Attribute a) {
        return a == Power || a == Seduction || a == Perception || (self.has(Trait.nymphomania) && a == Attribute.Nymphomania);
    }

    public static boolean isTrainable(Character self, Attribute a) {
        if (a == Willpower) {
            return self.getWillpower().max() + 2 <= self.getMaxWillpowerPossible();
        }
        return a != Speed && a != Perception && (self.has(Trait.divinity) || a != Divinity);
    }
}
