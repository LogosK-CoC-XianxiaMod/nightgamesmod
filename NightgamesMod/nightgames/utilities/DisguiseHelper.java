package nightgames.utilities;

import java.util.ArrayList;
import java.util.Collection;

import nightgames.characters.Character;
import nightgames.characters.NPC;
import nightgames.characters.body.BodyPart;
import nightgames.combat.Combat;
import nightgames.status.Disguised;

public class DisguiseHelper {
    public static void disguiseCharacter(Character self, NPC target) {
        self.addNonCombat(new Disguised(self, target));
        self.body.clearReplacements();
        Collection<BodyPart> currentParts = new ArrayList<>(self.body.getCurrentParts()); 
        currentParts.forEach(part -> self.body.temporaryRemovePart(part, 1000));
        target.body.getCurrentParts().forEach(part -> self.body.temporaryAddPart(part, 1000));
        self.getTraits().forEach(t -> self.removeTemporaryTrait(t, 1000));
        target.getTraits().forEach(t -> self.addTemporaryTrait(t, 1000));
        self.completelyNudify(null);
        target.outfitPlan.forEach(self.outfit::equip);
    }

    public static void unmaskCharacter(Combat c, Character self) {
        self.purge(c);
    }
}