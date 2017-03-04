package nightgames.quest;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import nightgames.characters.Character;
import nightgames.json.JsonUtils;

public abstract class Quest {
    private String name;
    private int numPointTypes;
    protected Map<Character,int[]> points;
    
    public Quest() {
        this.name="ERROR";
        this.numPointTypes=0;
        this.points=new HashMap<Character, int[]>();
    }
    
    public Quest(String name, int numPointTypes) {
        this.name=name;
        this.numPointTypes=numPointTypes;
        this.points=new HashMap<Character, int[]>();
    }
    
    protected Quest(Map<Character, int[]> points, String name, int numPointTypes) {
        this.name=name;
        this.numPointTypes=numPointTypes;
        this.points=points;
    }
    
    public void pointTo(Character charto, int pointType) {
        if (!points.containsKey(charto)) {points.put(charto, new int[numPointTypes]);}
        points.get(charto)[pointType]+=1;
    }
    
    public int getPointsForOfType(Character charFor, int pointType) {
        if (points.containsKey(charFor)) {return points.get(charFor)[pointType];}
        return 0;
    }
    
    public String getName() {return name;}
    
    public JsonObject saveToJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("numPointTypes", numPointTypes);
        obj.add("points", JsonUtils.JsonFromMap(points));
        return obj;
    }
    
    public static Quest load(JsonObject object) {
        String name = object.get("name").getAsString();
//        int numPointTypes = object.get("numPointTypes").getAsInt();
        Map<Character,int[]> points = JsonUtils.mapFromJson(object.getAsJsonObject("points"), Character.class, int[].class);
        //return new Quest(name, numPointTypes, points);
        if (name.equals("Buttslut Training")) {return new ButtslutQuest(points);}
        return null;
    }
}
