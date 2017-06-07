import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static ArrayList<Factory> factories;
    public static ArrayList<Distance> distances;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        final int factoryCount = in.nextInt(); // the number of factories
        System.err.println("factorycount: " + factoryCount);
        int linkCount = in.nextInt(); // the number of links between factories
        for (int i = 0; i < linkCount; i++) {
            int factory1 = in.nextInt();
            int factory2 = in.nextInt();
            int distance = in.nextInt();
            // Distance distHeen = new Distance(factory1, factory2, distance);
            // distances.add(distHeen);
            // Distance distTerug = new Distance(factory2, factory1, distance);
            // distances.add(distTerug);
            System.err.println("distances size: " + distances.size());
        }

//        Hieronder wat spul uit de spel avond
        SortedSet<Integer> ourFacts = new TreeSet<>();
        Set<Integer> enemyFacts = new HashSet<>();
        Map<Integer, Integer> factTroop = new HashMap<>();
        Map<Integer, Integer> production = new HashMap<>();
        int loopCount = 0;

        // game loop
        while (true) {
            int entityCount = in.nextInt(); // the number of entities (e.g. factories and troops)
            enemyFacts.clear();
            ourFacts.clear();
            for (int i = 0; i < entityCount; i++) {
                int entityId = in.nextInt();
                String entityType = in.next();
                System.err.println("entityId " + entityId + " entityType " + entityType);
                int arg1 = in.nextInt();
                int arg2 = in.nextInt();
                int arg3 = in.nextInt();
                int arg4 = in.nextInt();
                int arg5 = in.nextInt();

                if ( entityType.equalsIgnoreCase("factory")){
                    if ( arg1 == 1 ){
                        // for ( int j = 0; j<factoryCount; j++){
                        //     if (j != 1 && j != entityId){
                        //          System.out.println("MOVE " + entityId + " "  +j + " 3");
                        //     }
                        ourFacts.add(entityId);
                        factTroop.put(entityId, arg2);

                    }else{
                        enemyFacts.add(entityId);
                        production.put(entityId, arg3);

                    }

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



        }
    }
}

class Factory extends Entity{
    private int cyborgs;
    private int production;

    Factory(int entityId){
        super(entityId);
    }
    Factory(){}


}
class Entity{
    private int entityId;
    public enum EntityType
    {
        FACTORY, TROOP;
    }
    private int owner;
    public Map<Integer, Integer> distance = new HashMap<>();

    public Entity(){}
    public Entity(int entityId){
        this.entityId = entityId;
    }
}

class Distance{
    private int factory1;
    private int factory2;
    private int distance;

    Distance(int factory1, int factory2, int turns){
//        this.factory1 = factory1;
//        this.factory1 = factory1;
//        this.factory1 = factory1;
    }

    public int getFactory1() {
        return factory1;
    }

    public void setFactory1(int factory1) {
        this.factory1 = factory1;
    }

    public int getFactory2() {
        return factory2;
    }

    public void setFactory2(int factory2) {
        this.factory2 = factory2;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}