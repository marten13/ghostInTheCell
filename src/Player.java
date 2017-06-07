import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static ArrayList<Factory> factories;
    public static int factoryCount;
    public static Factory factory;
    public static ArrayList<Troop> troops;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        //Initialization of the factories.
        factoryCount = in.nextInt(); // the number of factories
        System.err.println("factorycount: " + factoryCount);
        factories = new ArrayList<Factory>();
        for (int x = 0; x < factoryCount; x++){
            factories.add(x, new Factory(x));
        }
        int linkCount = in.nextInt(); // the number of links between factories
        for (int i = 0; i < linkCount; i++) {
            int factory1 = in.nextInt();
            int factory2 = in.nextInt();
            int distance = in.nextInt();
            addDistanceToFactory(factory1, factory2, distance);
            addDistanceToFactory(factory2, factory1, distance);
            System.err.println("distances " + factory1 + " " + factory2 + " " + distance);
        }
       
//        Hieronder wat spul uit de spel avond
        SortedSet<Integer> ourFacts = new TreeSet<>();
        Set<Integer> enemyFacts = new HashSet<>();
        Map<Integer, Integer> factTroop = new HashMap<>();
        Map<Integer, Integer> production = new HashMap<>();
        int loopCount = 0;
//        Hierboven wat spul uit de spel avond

        troops = new ArrayList<Troop>();
        // game loop
        while (true) {
            int entityCount = in.nextInt(); // the number of entities (e.g. factories and troops)
            int troopCount = entityCount - factoryCount;
            System.err.println("troopcount " + troopCount);
            for (int i = 0; i < entityCount; i++) {
                int entityId = in.nextInt();
                String entityType = in.next();
                // variables in args:       Factory     Troop
                int arg1 = in.nextInt();//  owner       owner
                int arg2 = in.nextInt();//  cyborgs     cyborgs
                int arg3 = in.nextInt();//  production  homeFactory
                int arg4 = in.nextInt();//              targetFactory
                int arg5 = in.nextInt();//              turnsTillArrival

                if ( entityType.equalsIgnoreCase("FACTORY")){
                    //Set the args to the right factory
                    setArgsInFactory(entityId, arg1, arg2, arg3);
                    switch (arg1) {
                        case 1:
                            ourFacts.add(entityId);
                            factTroop.put(entityId, arg2);
                            break;
                        case 0:
                            enemyFacts.add(entityId);
                            production.put(entityId, arg3);
                            break;
                        case -1:
                            enemyFacts.add(entityId);
                            production.put(entityId, arg3);
                            break;
                    }
                }
                else
                {
                    //the entity is a TROOP
                     troops.add( new Troop(entityId, "TROOP", arg1, arg2, arg3, arg4, arg5));
                }
            }


            // for (Integer fact : ourFacts){
            //     // if (factTroop.get(fact) > 10){
            //         for ( Integer efact : enemyFacts ){
            //                 // System.err.println(efact);
            //                 if (!ourFacts.contains(efact)){
            //                     System.out.println("MOVE " + fact + " " + efact + " 10");
            //                 }
            //         // }
            //     }
            // }

            List<Integer> ourList = new ArrayList<>();
            List<Integer> enemyList = new ArrayList<>();

            ourList.addAll(ourFacts);
            enemyList.addAll(enemyFacts);

            System.err.println("Ours: " + ourList);
            System.err.println("Theirs: " + enemyList);

            int lastToSend = -1;
            int numToSend = 10;


            for ( int j = 0 ; j < ourList.size(); j++){
                // System.err.println(factTroop.get(ourList.get(j))/enemyList.size());
                numToSend = factTroop.get(ourList.get(j))/enemyList.size()+loopCount;
                // System.err.println(numToSend);
                // if ( j != lastToSend ){
                // for ( int k = 0; k < enemyList.size(); k++){
                // System.err.println(numToSend);
                long i = 0;
                for (Map.Entry<Integer, Integer> pair : production.entrySet()) {
                    if( pair.getValue()>0){
                        if (enemyFacts.contains(pair.getKey())){
                            System.out.println("MOVE " + ourList.get(j) + " " + pair.getKey() + " " + numToSend);
                        }
                    }

                    // System.out.println("MOVE " + ourList.get(j) + " "
                    // + enemyList.get(k) + " " + numToSend);
                    // lastToSend = j;
                    // System.err.println("Ours: " + ourFacts);
                    // System.err.println("Theirs: " + enemyFacts);
                    // System.err.println("Troops: " + factTroop);
                }
            }
            // }

            loopCount++;
            // Iterator ourIter = ourFacts.iterator();
            // while (ourIter.hasNext()) {
            //     Iterator enemyIter = enemyFacts.iterator();
            //     while (enemyIter.hasNext()){
            //         System.out.println("MOVE " + ourIter.next() + " " + enemyIter.next() + " 10");
            //     }
            // }



            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            // for (int i = 0; i < entityCount; i++){
            //     System.err.println(in.next());
            // }

            // Any valid action, such as "WAIT" or "MOVE source destination cyborgs"
            // for ( int i = 0; i<factoryCount; i++){
            //     if (i != 1){

            //         System.out.println("MOVE 1 " +i + " 3");

            //     }
            // }
            // for ( int i = 0; i < entityCount; i++ ){

            // }

            enemyFacts.clear();
            ourFacts.clear();
            troops.clear();

        }
    }
    private static void addDistanceToFactory(int factory1, int factory2, int distance)
    {
        Iterator itFact = factories.iterator();
        while(itFact.hasNext()){
            Factory distFact = (Factory) itFact.next();
            if (distFact.getEntityId() == factory1){
                distFact.factoriesDistances.put(factory2, distance);
            }
        }
    }
    
    private static void setArgsInFactory(int entityId, int arg1, int arg2, int arg3){
        Iterator itFact = factories.iterator();
        while (itFact.hasNext()) {
            Factory f = (Factory) itFact.next();
            if (f.getEntityId() == entityId){
                f.setEntityType("FACTORY");
                f.setOwner(arg1);
                f.setCyborgs(arg2);
                f.setProduction(arg3);
            }
        }
    }

}

class Troop extends Entity{
    private int homeFactory;
    private int targetFactory;
    private int turnsTillArrival;
    
    Troop(){}
    Troop(int entityId, String entityType, int owner, int homeFactory, int targetFactory, int cyborgs, int turnsTillArrival){
//        super(entityId);
//        super(entityType);
//        super(owner);
//        super(cyborgs);
        super();
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
        this.turnsTillArrival = turnsTillArrival;
    }

    public int getHomeFactory() {
        return homeFactory;
    }

    public void setHomeFactory(int homeFactory) {
        this.homeFactory = homeFactory;
    }

    public int getTargetFactory() {
        return targetFactory;
    }

    public void setTargetFactory(int targetFactory) {
        this.targetFactory = targetFactory;
    }

    public int getTurnsTillArrival() {
        return turnsTillArrival;
    }

    public void setTurnsTillArrival(int turnsTillArrival) {
        this.turnsTillArrival = turnsTillArrival;
    }


}

class Factory extends Entity{
    private int production;
    //Distance from this factory to all others:
    public Map<Integer, Integer> factoriesDistances = new HashMap<>();

    Factory(int entityId){
        super(entityId);
    }

    // public int getId() {
    //     return getEntityId();
    // }

    public int getProduction() {
        return production;
    }

    public void setProduction(int production) {
        this.production = production;
    }

    public int getDistanceToFactory2(int factory2){
        if (factoriesDistances.containsKey(factory2)){
            return factoriesDistances.getOrDefault(factory2, 0 );
        }
        return 0;
    }

}

class Entity{
    public int entityId;
    public String entityType;
    public int owner;
    public int cyborgs;
    
    public Entity(){}
    public Entity(int entityId, String entityType, int owner, int cyborgs){
        this.entityId = entityId;
        this.entityType = entityType;
        this.owner = owner;
        this.cyborgs = cyborgs;
    }
    public Entity(int entityId){
        this.entityId = entityId;
    }
    public int getEntityId() {
        return entityId;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }
    
    public int getCyborgs() {
        return cyborgs;
    }

    public void setCyborgs(int cyborgs) {
        this.cyborgs = cyborgs;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getEntityType() {
        return entityType;
    }
}
