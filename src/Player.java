import java.util.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static List<Factory> factories = new ArrayList<>();
    public static int factoryCount;
    public static List<Troop> troops = new ArrayList<>();

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        //Initialization of the factories.
        factoryCount = in.nextInt(); // the number of factories
        System.err.println("factorycount: " + factoryCount);
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
        }
       
        // game loop
        while (true) {
//        Hieronder wat spul uit de spel avond
            SortedSet<Integer> ourFacts = new TreeSet<>();
            Set<Integer> enemyFacts = new HashSet<>();
            Map<Integer, Integer> factTroop = new HashMap<>();
            Map<Integer, Integer> production = new HashMap<>();
            int loopCount = 0;
//        Hierboven wat spul uit de spel avond

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

            int numToSend;

//****** NEW Stuff

            String move = "";

            //Which factories are mine?
            List<Factory> myFactories = listMyFact();
            //Which factories are NOT mine?
            List<Factory> notMyFactories = listEnemyFact();
            for (Factory myFact: myFactories) {
                int id;
                for (Factory otherFact: notMyFactories) {
                    id = otherFact.getEntityId();
                    int dist = myFact.getDistanceToOtherFactory(id);
                    int prod = otherFact.getProduction();
                    int cyb  = otherFact.getCyborgs();
                    for (Troop trp : troops){
                        if (trp.getTurnsTillArrival()<= dist){
                            if(trp.getTargetFactory() == id ){
                                if (trp.getOwner() == otherFact.getOwner()){
                                    cyb = cyb + trp.getCyborgs();
                                }
                                else{
                                    cyb = cyb - trp.getCyborgs();
                                }
                            }
                        }
                    }
                    int cybNeedForConquer = cyb + prod * otherFact.getOwner()* -1 * dist + 1;
                    System.err.println(cybNeedForConquer + " cyborgs Needed For Conquering " + id + " from: " + myFact.getEntityId());
                }
                //hier een sorted map maken met otherfacts  en cybNeedFor Conquer

                //collect wat op je af komt dus hoeveel cyb je moet houden. Of hoeveel hulp heb je nodig.
                // De rest kan uitgezonden naar hulp vragers en ten tweede naar de makkelijkste otherfacts.
            }



            // oude code
            for ( int j = 0 ; j < ourList.size(); j++){
                // System.err.println(factTroop.get(ourList.get(j))/enemyList.size());
                numToSend = factTroop.get(ourList.get(j))/enemyList.size()+loopCount;
                long i = 0;
                for (Map.Entry<Integer, Integer> pair : production.entrySet()) {
                    if( pair.getValue()>0){
                        if (enemyFacts.contains(pair.getKey())){
                            System.out.println("MOVE " + ourList.get(j) + " " + pair.getKey() + " " + numToSend);
                        }
                    }
                }
            }
            // }

            loopCount++;

            enemyFacts.clear();
            ourFacts.clear();
            troops.clear();

        }
    }
    private static List listMyFact(){
        //Which factories are mine?
        List <Factory> myFact = new ArrayList<>();
        Iterator it = factories.iterator();
        while ( it.hasNext()) {
            Factory tmpFact = (Factory) it.next();
            if (tmpFact.getOwner() == 1) {
                myFact.add(tmpFact);
            }
        }
        return myFact;
    }
    private static List listEnemyFact(){
        //Which factories are not mine?
        List <Factory> notMyFact = new ArrayList<>();
        Iterator it = factories.iterator();
        while ( it.hasNext()) {
            Factory tmpFact = (Factory) it.next();
            if (tmpFact.getOwner() < 1) {
                notMyFact.add(tmpFact);
            }
        }
        return notMyFact;
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

    public int getProduction() {
        return production;
    }

    public void setProduction(int production) {
        this.production = production;
    }

    public int getDistanceToOtherFactory(int factory2){
        System.err.println(" # other facts" + factoriesDistances.size());
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
    private  int cyborgs;
    
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

class MapUtil
{
// Van https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
//    Dus gewoon aanroepen met b.v. testMap = MapUtil.sortByValue( testMap );
    public static <K, V extends Comparable<? super V>> Map<K, V>
    sortByValue( Map<K, V> map )
    {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return (o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }
}
