package nightgames.combat;

import nightgames.characters.Character;
import nightgames.stance.Position;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Optional;

class CombatLog {

    private final Character p1, p2;
    private Writer writer;


    CombatLog(Combat cbt) {
        this.p1 = cbt.getP1Character();
        this.p2 = cbt.getP2Character();
        try {
            Character last1 = p1.clone();
            Character last2 = p2.clone();
            last1.finishClone(p1);
            last2.finishClone(p2);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        Position lastP = cbt.getStance();
        try {
            File dir = new File("combatlogs");
            if (!dir.isDirectory())
                dir.mkdir();
            File file = new File(String.format("combatlogs/%s VS %s - %d.log", p1.getTrueName(), p2.getTrueName(),
                            System.currentTimeMillis()));
            file.createNewFile();
            writer = Files.newBufferedWriter(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void logHeader() {
        StringBuilder sb = new StringBuilder("Combat Log - ");
        sb.append(p1.getTrueName())
          .append(" versus ")
          .append(p2.getTrueName())
          .append("\n");
        describeForHeader(p1, p2, sb);
        describeForHeader(p2, p1, sb);
        sb.append("____________________________").append("\n").append("\n");
        try {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void logEnd(@Nullable Character winner) {
        StringBuilder sb = new StringBuilder("\nMATCH OVER: ");
        if (winner != null) {
            sb.append(winner.getTrueName())
              .append(" WINS");
        } else {
            sb.append("DRAW");
        }
        try {
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void describeForHeader(Character c, Character other, StringBuilder sb) {
        sb.append(c.getTrueName())
          .append(" at start:").append("\n");
        sb.append(c.att.toString());
        sb.append(c.getTraits().toString());
        sb.append(c.status.toString());
        sb.append(", ");
        c.body.describe(sb, other, " ", false);
        sb.append(" -- ");
        sb.append(c.outfit.describe(c));
        sb.append("\n");
    }

}
