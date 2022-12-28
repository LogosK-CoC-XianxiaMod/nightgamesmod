package nightgames.debug;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.global.Global;
import nightgames.items.Item;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DebugGUIPanel extends JPanel {
    /**
     * These commands use 'Player' to refer to the human controlled character
     * rather than their name, e.g. Player.addItem Talisman 2.
     */
    private static final long serialVersionUID = 9001844139147018883L;

    private static List<DebugCommand> consoleCommands = new ArrayList<>();

    {
        
        consoleCommands.add(new DebugCommand("help", (output, list) -> {
            output.setText("Available commands are:all.(Command)\n(Charactername).setXp\n(Charactername).setMoney\n(Charactername).addTrait\n(Charactername).removeTrait\n"
                            + "(Charactername).addItem\n(Charactername).addAtt\n(Charactername).addAffection\n(Charactername).addAttraction\n(Charactername).ding\n(Charactername).list\n");
        }));
        
        //TODO: What is the correct usage of this with a modifier?
        consoleCommands.add(new DebugCommand("all\\.(.*)", (output, list) -> {
            Global.getParticipants().stream().forEach(participant -> {
                consoleCommands.stream().filter(cc -> cc.checkAndExecute(output, participant.type + "." + list.get(1))).findFirst();
            });
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.setMoney (\\d+)", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                target.setMoney(Integer.valueOf(list.get(2)));
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            }
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.addTrait (\\w+)", (output, list) -> {
            try {
            	Character target = Global.getCharacterByType(list.get(1));
            	if (list.get(2).equals("all")) {
            		for (Trait t : Trait.values()) {
            			target.add(t);
            		}
            	} else {
	                target.add(Trait.valueOf(list.get(2)));
            	}
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            } catch (IllegalArgumentException e) {
                output.setText(list.get(2) + " is not a valid trait");
            }
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.removeTrait (\\w+)", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                target.remove(Trait.valueOf(list.get(2)));
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            } catch (IllegalArgumentException e) {
                output.setText(list.get(2) + " is not a valid trait");
            }
        }));

        // This command uses the name of the enum value of the item, not
        // its display name, e.g. 'Talisman' rather than 'Dark Talisman'.
        consoleCommands.add(new DebugCommand("(\\w+)\\.addItem (\\w+) ?(\\d+)?", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                int amt = 1;
                if (list.size() > 3 && list.get(3) != null) {
                    amt = Integer.valueOf(list.get(3));
                }
                target.gain(Item.valueOf(list.get(2)), amt);
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            } catch (IllegalArgumentException e) {
                output.setText(list.get(2) + " is not a valid item");
            }
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.addAtt (\\w+) ?(\\d+)?", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                int amt = 1;
                if (list.size() > 3 && list.get(3) != null) {
                    amt = Integer.valueOf(list.get(3));
                }
                target.modAttributeDontSaveData(Attribute.valueOf(list.get(2)), amt);
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            } catch (IllegalArgumentException e) {
                output.setText(list.get(2) + " is not a valid attribute");
            }
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.addAffection (\\d+)?", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                int amt = 1;
                if (list.size() > 2 && list.get(2) != null) {
                    amt = Integer.valueOf(list.get(2));
                }
                target.gainAffection(Global.getPlayer(), amt);
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            } catch (IllegalArgumentException e) {
                output.setText(list.get(2) + " is not a valid amount of affection");
            }
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.addAttraction (\\d+)?", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                int amt = 1;
                if (list.size() > 2 && list.get(2) != null) {
                    amt = Integer.valueOf(list.get(2));
                }
                target.gainAttraction(Global.getPlayer(), amt);
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            } catch (IllegalArgumentException e) {
                output.setText(list.get(2) + " is not a valid amount of attraction");
            }
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.ding( \\d+)?", (output, list) -> {
            Character target = Global.getCharacterByType(list.get(1));
            if (target == null) {
                output.setText(list.get(1) + " is not a valid character");
                return;
            }
            int times = 1;
            if (list.size() > 2 && list.get(2) != null) {
                try {
                    times = Integer.valueOf(list.get(2).trim());
                } catch (NumberFormatException e) {
                }
            }
            IntStream.range(0, times).forEach(i -> target.ding(Global.gui().combat));
        }));
        consoleCommands.add(new DebugCommand("(\\w+)\\.list", (output, list) -> {
            try {
                Character target = Global.getCharacterByType(list.get(1));
                StringBuilder sb = new StringBuilder();
                sb.append("Level: " + target.getProgression().getLevel() + "\n");
                List<Trait> traits = new ArrayList<>(target.getTraits());
                for (int i = 0; i < traits.size(); i++) {
                    sb.append(traits.get(i));
                    if (i % 4 == 2) {
                        sb.append("\n");
                    } else if (i != traits.size() - 1) {
                        sb.append(", ");
                    }
                }
                String attString = Arrays.stream(Attribute.values()).filter(att -> target.get(att) != 0).map(att -> String.format("%s: %d", att, target.get(att))).collect(Collectors.joining("\n"));
                output.setText(String.format("Stamina [%s]\nArousal [%s]\nMojo [%s]\nWillpower [%s]\nAttractiveness: %.01f\n%s\n%s",
                                target.stamina.toString(), target.arousal.toString(),
                                target.getMojo().toString(), target.willpower.toString(), target.body.getHotness(Global.getPlayer()), attString, sb.toString()));
            } catch (NullPointerException e) {
                output.setText(list.get(1) + " is not a valid character");
            }
        }));
    }

    private JTextArea out;
    private static List<String> oldCommands = new ArrayList<>();
    private static int index = 0 ;

    public DebugGUIPanel() {
        add(new JLabel("Debuggers"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        out = new JTextArea();
        out.setOpaque(false);
        out.setRows(20);
        out.setEditable(false);
        out.setText("");
        add(out);

        JTextField console = new JTextField();
        console.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                index = oldCommands.size();
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP && !oldCommands.isEmpty()) {
                    if (index == oldCommands.size() && !console.getText().isEmpty()) {
                        oldCommands.add(console.getText());
                    }
                    if (index > 0) {
                        index -= 1;
                    }
                    console.setText(oldCommands.get(index));
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN && !oldCommands.isEmpty()) {
                    if (index < oldCommands.size() - 1) {
                        index += 1;
                    }
                    console.setText(oldCommands.get(Math.min(oldCommands.size() - 1, index)));
                }
            }
        });
        console.addActionListener(action -> {
            out.setText("");
            String command = console.getText();
            if (!command.isEmpty()) {
                oldCommands.add(command);
            }
            index = oldCommands.size();
            Optional<DebugCommand> opt =
                            consoleCommands.stream().filter(cc -> cc.checkAndExecute(out, command)).findFirst();
            if (!opt.isPresent()) {
                out.setText("Invalid Command");
            } else if (out.getText().isEmpty()) {
                out.setText("OK");
            }
            console.setText("");
        });
        add(console);
    }
}
