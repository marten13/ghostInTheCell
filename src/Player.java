import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static List<Factory> factories = new ArrayList<>();
    public static int factoryCount;
    public static Map<Integer, Troop> troops = new HashMap<>();
    public static List<Bomb> bombs = new ArrayList<>();
    public static int loopCount;
    public static int myBombCount = 2;
    public static int enemyBombCount = 2;
    public static List<String> outputStrings = new ArrayList<>();
    public static List<Integer> currentBombs = new ArrayList<>();

    public static void main(String args[]) {
//        MapUtilTest.testSortByValue();
        Player player = new Player();
        player.run();
    }

    private void run(){
        Scanner in = new Scanner(System.in);

        //Initialization of the factories.
        factoryCount = in.nextInt(); // the number of factories
        for (int x = 0; x < factoryCount; x++){
            factories.add(new Factory(x));
        }
        int linkCount = in.nextInt(); // the number of links between factories
        for (int i = 0; i < linkCount; i++) {
            int factory1 = in.nextInt();
            int factory2 = in.nextInt();
            int distance = in.nextInt();
            factories.get(factory1).addDistanceToOtherFactory(factory2, distance);
            factories.get(factory2).addDistanceToOtherFactory(factory1, distance);

        }
        //Other initialisations.
        long maxTurnTime = 0;
        boolean firstTurn = true;
        for (Factory f: factories) {
            f.setMaxDistance();
        }

        // game loop
        while (true) {
            long starttime = System.currentTimeMillis();
            outputStrings.clear();
            currentBombs.clear();
            loopCount++;
            
            troops.forEach((integer, troop) -> troop.setTurnsTillArrival(troop.getTurnsTillArrival()-1));
            troops.entrySet().removeIf(entry->entry.getValue().getTurnsTillArrival()<0);

//            for (Map.Entry<Integer, Troop> t: troops.entrySet()){
//                drukAf("Na aanpassing troops " + t.getKey() + " id " + t.getValue().getEntityId() + " turns " + t.getValue().getTurnsTillArrival());
//            }


            int entityCount = in.nextInt(); // the number of entities (e.g. factories and troops)
            for (int i = 0; i < entityCount; i++) {
                int entityId = in.nextInt();
                String entityType = in.next();
                // variables in args:       Factory             Troop               Bomb
                int arg1 = in.nextInt();//  owner               owner               owner
                int arg2 = in.nextInt();//  cyborgs             cyborgs             homeFactory
                int arg3 = in.nextInt();//  production          homeFactory         targetFactory
                int arg4 = in.nextInt();//  turnsTillResumeProd targetFactory       turnsTillArrival
                int arg5 = in.nextInt();//                      turnsTillArrival

                switch (entityType){
                    case "FACTORY":
                        factories.get(entityId).setArgsInFactory(arg1, arg2, arg3, arg4);
                        break;
                    case "TROOP":
                        troops.put(entityId, new Troop(entityId, arg1, arg2, arg3, arg4, arg5));
                        break;
                    case "BOMB":
                        addBombOrFlightTime( entityId,  arg1,  arg2,  arg3,  arg4);
                        currentBombs.add(entityId);
                }
            }//End of scanner input

            updateBomblist(currentBombs);
            //Which factories are mine?
            List<Factory> myFactories = new ArrayList<>();
            List<Factory> notMyFactories = new ArrayList<>();
            factories.forEach(factory -> {if(factory.getOwner()==1) myFactories.add(factory); else notMyFactories.add(factory);});
            troops.forEach((integer, troop) -> factories.get(troop.getTargetFactory()).addTroopArrivalsToFactory(troop));

            //ACTIONS ARE PLANNED FROM HERE ON

            // komt eerst de bom? Dan kan je production bepalen, dan defense, help en attack.
            //organise own defense
            if (loopCount < 3) {
                myFactories.forEach(myFact -> myFact.setEnemyBombOnTheWay(true)); //to prevend immediate upgrade
            } else {
                myFactories.forEach(myFact -> myFact.canBombArriveNow(bombs));
            }
            myFactories.forEach(myFact -> myFact.setTurnsAndProducedCyb(bombs));
            drukAf("f1 na turns and produced " + myFactories.get(0).getSurplusCyb(0));
            myFactories.forEach(Factory::setTurnsAndSurplusCyb);
            drukAf("f1 na surplus " + myFactories.get(0).getSurplusCyb(0));
            myFactories.forEach(Factory::adjustTurnsAndSurplusCybForLaterEnemies);
            drukAf("f1 na adjust for enemies " + myFactories.get(0).getSurplusCyb(0));
            myFactories.forEach(myFact -> myFact.setBombSender(false));
            myFactories.forEach(Factory::adjustTurnsAndSurplusCybForProdZero);
            drukAf("f1 na adjust for prod 0 " + myFactories.get(0).getSurplusCyb(0));

            //Send first bomb immediately
            if (myBombCount == 2){
                int target = notMyFactories.stream().filter(f -> f.getOwner() == -1).findFirst().get().getEntityId();
                drukAf("my b " + myBombCount + " target id " + target);
                myFactories.get(0).setBombSender(true);
                outputStrings.add("BOMB " + myFactories.get(0).getEntityId() + " " + target);
            }
            //Help Friends
            for (Factory myFact: myFactories) {
                int sendableCyb = myFact.getCurrentSurplusCybAvailableForSending();
                //todo deze administratie klopt niet. Vooral goed te zien als je wint en niet aangevallen wordt. Misschien voor 1 fact hele turns and surplus afdrukken.
                drukAf("Factory " + myFact.getEntityId() + " can help with " + sendableCyb);
                if (sendableCyb > 0) {
                    //Help if I can send help in time
                    if (!myFact.isBombSender()){
                        outputStrings.addAll(myFact.getFactoriesICanHelp(myFactories));
                    }
                }else{
                    drukAf("my bombs " + myBombCount);
                    if (myBombCount > 0){
                        int target = myFact.chooseBombTarget(notMyFactories, bombs);
                        if (target > -1){
                            outputStrings.add("BOMB " + myFact.getEntityId() + " " + target);
                        }
                    }
                }
            }
            //TODO is a factory not survivable? evacuate instead of fight

            //upgrade self
            for (Factory myFact: myFactories) {
                if (!myFact.isBombSender()) {
                    int sendableCyb = myFact.getCurrentSurplusCybAvailableForSending();
                    drukAf("Factory " + myFact.getEntityId() + " could use for upgrade " + sendableCyb);
                    if (sendableCyb > 0) {
                        if (myFact.willIncreaseProd()) {
                            outputStrings.add("INC " + myFact.getEntityId());
                        }
                    }
                }
            }

            //Help friends to upgrade
            for (Factory myFact: myFactories) {
                if (!myFact.isBombSender()) {
                    int sendableCyb = myFact.getCurrentSurplusCybAvailableForSending();
                    drukAf("Factory " + myFact.getEntityId() + " could use for upgrading friends " + sendableCyb);
                    if (sendableCyb > 0) {
                        outputStrings.addAll(myFact.getFactoriesICanHelpToUpgrade(myFactories));
                    }
                }
            }


            //Conquer
            for (Factory notMyFact: notMyFactories) {
                notMyFact.canBombArriveNow(bombs);
                notMyFact.setTurnsAndProducedCyb(bombs);
                notMyFact.setTurnsAndSurplusCyb();
            }
            for (Factory myFact: myFactories) {
                if (!myFact.isBombSender()) {
                    int sendableCyb = myFact.getCurrentSurplusCybAvailableForSending();
                    drukAf("Factory " + myFact.getEntityId() + " could use for attacking " + sendableCyb + " max " + myFact.getMaxDistance());
                    if (sendableCyb > 0) {
                        outputStrings.addAll(myFact.getConqueringMoves(notMyFactories));
                    }
                }
            }


            sendMoves(outputStrings);

            long duration = System.currentTimeMillis() - starttime;
            drukAf("time: " + duration);
            if (!firstTurn && duration > maxTurnTime) {maxTurnTime = duration;}
            drukAf("maxTurnTime " + maxTurnTime);
            firstTurn = false;
        }
    }


    private void sendMoves(List mvs){
        if (!mvs.isEmpty()) {
            String outputMoves = mvs.toString();
            outputMoves = outputMoves.replace(",", ";");
            outputMoves = outputMoves.replace("]", "");
            outputMoves = outputMoves.replace("[", "");
            System.out.println(outputMoves);
        }else{
            System.out.println("WAIT");
        }
    }

    private void setInventoryOfHelp(List<Factory> myFactories) {
        Map<Integer, Integer> myFactAndHelpTurns = new HashMap<>();
        Map<Integer, Integer> myFactAndHelpAmount = new HashMap<>();
        Map<Integer, Integer> myFactAndSurplus = new HashMap<>();
        Map<Integer, Integer> helpDemandersAndProdAtHelpturn = new HashMap<>();

        for (Factory myFact: myFactories){
            myFactAndHelpTurns.put(myFact.getEntityId(), myFact.getHelpTurns());
            myFactAndHelpAmount.put(myFact.getEntityId(), myFact.getHelpAmount());
            myFactAndSurplus.put(myFact.getEntityId(), myFact.getCurrentSurplusCybAvailableForSending());
        }
        for (Factory myFact: myFactories){
            //TODO structure help
            //maak hashmap met hulpvragers en production op moment helpturns mits geen bombontheway, sortDesc
            for (Factory f: myFactories){
                if (myFactAndHelpTurns.getOrDefault(f.getEntityId(), 0) > 0 && !f.isEnemyBombOnTheWay()){
                    helpDemandersAndProdAtHelpturn.put(f.getEntityId(),f.getProducedCyb(myFactAndHelpTurns.getOrDefault(f.getEntityId(),0)));
                }
            }
            helpDemandersAndProdAtHelpturn = Sorter.sortByDescValue(helpDemandersAndProdAtHelpturn);
            //haal voor hulpvragers alle factoriesAndDistances in local hashmap, sortAscVal, remove Fact met dist>helpturns
            //remove fact zonder surplus
            //bijwerken myFactAndSurplus

        }

    }

    private void addBombOrFlightTime(int entityId, int owner, int homeFactory, int targetFactory, int turnsTillArrival) {
        boolean knownBomb = bombs.stream()
                .anyMatch((Bomb b) -> entityId == b.getEntityId());
        if (!knownBomb) {
            bombs.add( new Bomb(entityId, owner, homeFactory, targetFactory, turnsTillArrival));
            if (owner == 1) myBombCount --;
            else {enemyBombCount--;
                drukAf("Bomb!" );
            }
        }
    }

    private void updateBomblist(List<Integer> currentBombs){
        bombs.forEach(Bomb::upFlightTime);
        bombs.removeIf(bomb -> !currentBombs.contains(bomb.getEntityId()));
    }

    private void drukAf(String input){
        System.err.println(input);
    }
}
class Bomb extends Troop{
//     Each player possesses 2 bombs for each game. A bomb can be sent from any factory you control to any factory. 
// The corresponding action is: BOMB source destination

// When a bomb reaches a factory, half of the cyborgs in the factory are destroyed (floored), 
// for a minimum of 10 destroyed cyborgs. For example, if there are 33 cyborgs, 16 will be destroyed.
// But if there are only 13 cyborgs, 10 will be destroyed.

// Following a bomb explosion, the factory won't be able to produce any new cyborgs during 5 turns.

// Be careful, your radar is able to detect the launch of a bomb but you don't know where its target is!
// It is impossible to send a bomb and a troop at the same time from the same factory and to the same destination.
// If you try to do so, only the bomb will be sent.

//    Idee: ontvlucht de bomb: Je kent de homefact. hou de vluchtduur bij. If vluchtduur = dist  Than stuur alles weg
//          naar andere fact.
// TODO Maar voor andereEigenFact geldt: dist tot bombHomefact <> vluchtduur+dist this.fact tot andereEigenFact.
//          Anders kunnen ze tegelijk aankomen.

//      Aanval: voorwaarden: enemyFact cyb > 30. Tijdje niets naartoe sturen.
//              Zorg dat mijn troops 1 beurt later kunnen veroveren
    private int flightTime;
    Bomb(int entityId, int owner, int homeFactory, int targetFactory, int turnsTillArrival){
        super(entityId, "BOMB", owner, homeFactory, targetFactory, turnsTillArrival);
    }

    public void upFlightTime(){
        flightTime++;
        System.err.println("bomb nr " + getEntityId() + " flightTime: " + flightTime);
    }

    public int getFlightTime() {
        return flightTime;
    }
}

class Troop extends Entity{
    private int homeFactory;
    private int targetFactory;
    private int turnsTillArrival;
    
    Troop(){}
    //for Bomb
    Troop(int entityId, String entityType, int owner, int homeFactory, int targetFactory, int turnsTillArrival){
        super(entityId, entityType, owner);
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
        this.turnsTillArrival = turnsTillArrival;
    }
    // for Troop itself
    Troop(int entityId, int owner, int homeFactory, int targetFactory, int cyborgs, int turnsTillArrival){
        super(entityId, "TROOP", owner, cyborgs);
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
        this.turnsTillArrival = turnsTillArrival;
    }

    public int getHomeFactory() {
        return homeFactory;
    }

    public int getTargetFactory() {
        return targetFactory;
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
    private int turnsTillResumeProd;
    private Map<Integer, Integer> factoriesDistances = new HashMap<>();
    private int maxDistance;

    //misschien een inner class voor turns
    private Map<Integer, Integer> turnsToArriveAndFriendlyCyborgs = new LinkedHashMap<>();
    private Map<Integer, Integer> turnsAndProducedCyb = new LinkedHashMap<>();
    private Map<Integer, Integer> turnsAndSurplusCyb = new LinkedHashMap<>();
    private int helpAmount;
    private int helpTurns;
    private boolean enemyBombOnTheWay;
    private int currentSurplusCybAvailableForSending;
    private boolean bombMightArrive;
    private boolean BombSender;


    Factory(int entityId){
        super(entityId);
        this.setEntityType("FACTORY");
    }
    public void canBombArriveNow(List<Bomb> bombs) {
        setBombMightArrive(false);
        setEnemyBombOnTheWay(false);
        for (Bomb bmb : bombs) {
            if (bmb.getOwner() == -1 && this.getOwner() == 1) {
                if (getDistanceToOtherFactory(bmb.getHomeFactory()) > bmb.getFlightTime()) {
                    setEnemyBombOnTheWay(true);
                }
                if (getDistanceToOtherFactory(bmb.getHomeFactory()) == bmb.getFlightTime()) {
                    setEnemyBombOnTheWay(true);
                    setBombMightArrive(true);
                    drukAf("bomb CAN arrive at factory " + this.getEntityId());
                    setHelpAmount(0);
                    setCurrentSurplusCybAvailableForSending(getCyborgs());
                }
            }else if (bmb.getTurnsTillArrival() == 0 && this.getEntityId() == bmb.getTargetFactory()) {
                setBombMightArrive(true);
                setHelpAmount(0);
            }
        }
    }

    //included possible bomb effects. Enemy has negative production
    public void setTurnsAndProducedCyb(List<Bomb> bombs){
        //max turns to calculate is max Distance to other factories. Base = 0 = this turn
        for (int turn = 0; turn < maxDistance + 1; turn++){
            int p = 0;
            if (turnsTillResumeProd - turn <= 0){
                //TODO account for future upgrades
                p = getProduction() * getOwner();
            }
            if (getOwner() == 1) {
                if(this.isBombMightArrive() && turn > 0 && turn < 6) p=0;
            }else {
                for (Bomb bmb : bombs) {
                    if (this.getEntityId() == bmb.getTargetFactory() &&
                            turn > bmb.getTurnsTillArrival() &&
                            turn <= bmb.getTurnsTillArrival() + 6) {
                        p = 0;
                    }
                }
            }
            this.turnsAndProducedCyb.put(turn, p);
        }
    }

    public int getProducedCyb(int turn){return turnsAndProducedCyb.getOrDefault(turn, 0);}
    //amount of cyb after (troops arrive && production)
    public void setTurnsAndSurplusCyb() {
        turnsAndSurplusCyb.clear();
        int friendlyCyborgsInFactory;
        if (getOwner() == 1){friendlyCyborgsInFactory = getCyborgs();} else {friendlyCyborgsInFactory = getCyborgs() * -1;}
        this.turnsToArriveAndFriendlyCyborgs = Sorter.sortByAscKey(turnsToArriveAndFriendlyCyborgs);
        for (int turn = 0; turn < maxDistance + 1; turn++) {
            friendlyCyborgsInFactory += getTurnsToArriveAndFriendlyCyborgs(turn);
            if (turn>0) friendlyCyborgsInFactory += getProducedCyb(turn);
            turnsAndSurplusCyb.put(turn, friendlyCyborgsInFactory);
        }
        currentSurplusCybAvailableForSending = getSurplusCyb(0);
    }

    public int getSurplusCyb(int turn) {return turnsAndSurplusCyb.getOrDefault(turn, 0);}

    //save cyb for attacks in transit.
    // Set Help amount and turns.
    // Set initial currentSurplusCybAvailableForSending
    public void adjustTurnsAndSurplusCybForLaterEnemies() {
        //Walk backwards through set to account for attacks to determine how much I should save this turn.
        int surplus = 0;
        setHelpAmount(0);
        setHelpTurns(0);
        for (int turn = maxDistance; turn > -1 ; turn--) {
            surplus = turnsAndSurplusCyb.getOrDefault(turn, 0);
            if (surplus < 0) {
                helpAmount = (surplus * -1);
                helpTurns = (turn);
            }
            if (helpAmount > 0) {
                turnsAndSurplusCyb.put(turn, 0); // make sure I don't send anything
            }
        }
        currentSurplusCybAvailableForSending = getSurplusCyb(0);
    }

    public void adjustTurnsAndSurplusCybForProdZero(){
        if (getProduction() == 0){
            //find first turn with max available cyb.
            int turnWithMax = Collections.max(turnsAndSurplusCyb.entrySet(), Map.Entry.comparingByValue()).getKey();
            int max = turnsAndSurplusCyb.getOrDefault(turnWithMax,0);
            //will my factory be conquered and is help necessary?
            if (max < 10 && helpAmount == 0){
                    setHelpTurns(turnWithMax);
                    setHelpAmount(10 - max);
            }
        }
    }

    //Help Friends
    public List<String> getFactoriesICanHelp(List<Factory> myfactories){
        List<String> moves = new ArrayList<>();
        for (Factory f: myfactories){
            if (f.getEntityId() != this.getEntityId()){
                //Only help if I can help enough TODO: help together
                if(f.getHelpAmount() <= this.getCurrentSurplusCybAvailableForSending() &&
                        f.getHelpTurns() <= this.getDistanceToOtherFactory(f.getEntityId()) &&
                        f.getHelpAmount() > 0){
                    moves.add("MOVE " + this.getEntityId() + " " + f.getEntityId() + " " + f.getHelpAmount());
                    this.setCurrentSurplusCybAvailableForSending(this.getCurrentSurplusCybAvailableForSending() - f.getHelpAmount());
                    f.setHelpAmount(0);
                    f.setHelpTurns(0);
                }
            }
        }
        return moves;
    }

    //decide about increase of production
    public boolean willIncreaseProd(){
        if (canIncreaseProd() && getCurrentSurplusCybAvailableForSending() > 9 ){
            setCurrentSurplusCybAvailableForSending(getCurrentSurplusCybAvailableForSending() - 10);
            return true;
        }
        return false;
    }

    public boolean canIncreaseProd(){
        return this.getProduction() < 3
                && !isEnemyBombOnTheWay()
                && helpAmount == 0;
    }

    public List<String> getFactoriesICanHelpToUpgrade(List<Factory> myfactories){
        List<String> moves = new ArrayList<>();
        for (Factory f: myfactories){
            if (f.getEntityId() != this.getEntityId()){
                //Only help if I can help enough TODO: help together
                if(f.canIncreaseProd()){
                    int dist = this.getDistanceToOtherFactory(f.getEntityId());
                    int shouldSend = f.getSurplusCyb(dist);
                    if (this.getCurrentSurplusCybAvailableForSending() >= shouldSend && shouldSend > 0){
                        moves.add("MOVE " + this.getEntityId() + " " + f.getEntityId() + " " + shouldSend);
                        this.setCurrentSurplusCybAvailableForSending(this.getCurrentSurplusCybAvailableForSending() - shouldSend);
                        f.setSurplusCyb(dist, 0);
                    }
                }
            }
        }
        return moves;
    }
    //Conquer!
    public List<String> getConqueringMoves(List<Factory> notMyFactories){
        List<String> moves = new ArrayList<>();
        for (Factory ef: notMyFactories){
            //TODO sort on dist
            int dist = this.getDistanceToOtherFactory(ef.getEntityId());
            drukAf("fact " + ef.getEntityId() + " surplus on arrival " + ef.getSurplusCyb(dist));
            int shouldSend = ( (ef.getSurplusCyb(dist) * -1) + 1);
            if (getCurrentSurplusCybAvailableForSending() >= shouldSend && shouldSend > 0 ){
                drukAf("fact " + this.getEntityId() + " shouldsend " + shouldSend + " to fact " + ef.getEntityId());
                moves.add("MOVE " + this.getEntityId() + " " + ef.getEntityId() + " " + shouldSend);
                setCurrentSurplusCybAvailableForSending(getCurrentSurplusCybAvailableForSending() - shouldSend);
                ef.setSurplusCyb(dist, 0);
            }
        }

        return moves;
    }

    public boolean isEnemyBombOnTheWay() {
        return enemyBombOnTheWay;
    }

    public void setEnemyBombOnTheWay(boolean enemyBombOnTheWay) {
        this.enemyBombOnTheWay = enemyBombOnTheWay;
    }

    public int getCurrentSurplusCybAvailableForSending(){
    return this.currentSurplusCybAvailableForSending;
}

    public void setCurrentSurplusCybAvailableForSending(int currentSurplus) {
        this.currentSurplusCybAvailableForSending = currentSurplus;
    }
    public int getProduction() {
        return production;
    }
    public int getTurnsTillResumeProd(){
        return turnsTillResumeProd;
    }
    public void addDistanceToOtherFactory(int factory2, int distance){
        this.factoriesDistances.put(factory2, distance);
    }
    public int getDistanceToOtherFactory(int factory2){
        return factoriesDistances.getOrDefault(factory2, 0 );
    }
    public void addTroopArrivalsToFactory(Troop troop){
//        System.err.println(this.getEntityId() + " troop " + troop.getOwner() + " " + troop.getCyborgs() + " " + troop.getTargetFactory()+ " " + troop.getTurnsTillArrival() );
        int updateWaarde = troop.getOwner() * troop.getCyborgs();
        int oudeWaarde = turnsToArriveAndFriendlyCyborgs.getOrDefault(troop.getTurnsTillArrival(), 0);
        turnsToArriveAndFriendlyCyborgs.put(troop.getTurnsTillArrival(), oudeWaarde + updateWaarde);
    }
    public int getHelpAmount() {
        return helpAmount;
    }
    public void setHelpAmount(int helpAmount) {
        this.helpAmount = helpAmount;
    }
    public int getHelpTurns() {
        return helpTurns;
    }
    public void setHelpTurns(int helpTurns) {
        this.helpTurns = helpTurns;
    }
    public void setSurplusCyb(int turn, int surplus){
        this.turnsAndSurplusCyb.put(turn, surplus);
    }
    public boolean isBombMightArrive() {
        return bombMightArrive;
    }

    public void setBombMightArrive(boolean bombMightArrive) {
        this.bombMightArrive = bombMightArrive;
    }
    public int getTurnsToArriveAndFriendlyCyborgs(int turnsTillArrival){
        return turnsToArriveAndFriendlyCyborgs.getOrDefault(turnsTillArrival, 0);
    }

    public Set<Map.Entry<Integer, Integer>> getAllTurnsToArriveAndFriendlyCyborgs(){
        return this.turnsToArriveAndFriendlyCyborgs.entrySet();
    }
    public void clearTurnsToArriveAndFriendlyCyborgs() {
        turnsToArriveAndFriendlyCyborgs.clear();
    }
    public void setArgsInFactory(int arg1, int arg2, int arg3, int arg4){
        this.setOwner(arg1);
        this.setCyborgs(arg2);
        production = (arg3);
        turnsTillResumeProd =(arg4);
        clearTurnsToArriveAndFriendlyCyborgs();
    }

    private void drukAf(String input){System.err.println(input);}

    public void setMaxDistance(){maxDistance = Sorter.sortByDescValue(factoriesDistances).entrySet().iterator().next().getValue();}

    public int getMaxDistance() {
        return maxDistance;
    }

    public int chooseBombTarget(List<Factory> enemyFactories, List<Bomb> bombs) {
        //TODO sort on prod.
        for(Factory ef: enemyFactories){
            if (ef.getProduction() > 1 && ef.getCurrentSurplusCybAvailableForSending() < -1 && ef.getOwner() == -1){
                drukAf(ef.getEntityId() + " prod " + ef.getProduction() + " cyb avail " + ef.getCurrentSurplusCybAvailableForSending() + " own " + ef.getOwner());
                boolean bombedAllready = false;
                for (Bomb b: bombs){
                    if (ef.getEntityId() == b.getTargetFactory() || ef.getTurnsTillResumeProd() > 0){
                        bombedAllready = true;
                    }
                }
                if (!bombedAllready) {
                    this.setBombSender(true);
                    return ef.getEntityId();
                }
            }
        }
        return -1;
    }

    public void setBombSender(boolean b) {
        this.BombSender = b;
    }

    public boolean isBombSender() {
        return BombSender;
    }
}

@SuppressWarnings("WeakerAccess")
class Entity{
    private int entityId;
    private String entityType;
    private int owner;
    private int cyborgs;
    
    public Entity(){}
    //for Troop
    public Entity(int entityId, String entityType, int owner, int cyborgs){
        this.entityId = entityId;
        this.entityType = entityType;
        this.owner = owner;
        this.cyborgs = cyborgs;
    }
    //for Factory
    public Entity(int entityId){
        this.entityId = entityId;
    }
    //for Bomb
    public Entity(int entityId, String entityType, int owner){
        this.entityId = entityId;
        this.entityType = entityType;
        this.owner = owner;
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
}

class MapUtilTest
{
    public static void testSortByValue()
    {
        Random random = new Random(System.currentTimeMillis());
        int size = 10;
        Map<Integer, Integer> testMap = new HashMap<>(size);
        for(int i = 0 ; i < size ; ++i) {
            testMap.put( random.nextInt()/10000, random.nextInt()/10000);
        }
//        for(int i = 0 ; i < 10 ; ++i) {
            System.out.println( "" + testMap.entrySet());
//        }
        System.out.println( "sorted by  value");
        testMap = Sorter.sortByAscValue( testMap );
//        for(int i = 0 ; i < 10 ; ++i) {
            System.out.println( "" + testMap.entrySet());
//        }
        System.out.println( "sorted by inverse value");
        testMap = Sorter.sortByDescValue(testMap);
//        for(int i = 0 ; i < 10 ; ++i) {
        System.out.println( "" + testMap.entrySet());
//        }
        System.out.println( "sorted by Desc key");
        testMap = Sorter.sortByDescKey(testMap);
        System.out.println( "" + testMap.entrySet());
        System.out.println( "sorted by Asc key");
        testMap = Sorter.sortByAscKey(testMap);
        System.out.println( "" + testMap.entrySet());
   }

}
class Sorter{
// Van https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
//    Dus gewoon aanroepen met b.v. testMap = Sorter.sortByiets( testMap );
    public static<K, V extends Comparable<? super V>> Map<K, V> sortByAscValue(Map<K, V> map){
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(e1,e2)->e1,LinkedHashMap::new));
    }
    public static<K, V extends Comparable<? super V>> Map<K, V> sortByDescValue(Map<K, V> map){
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(e1,e2)->e1,LinkedHashMap::new));
    }

    public static<K extends Comparable<? super K>, V > Map<K, V> sortByDescKey(Map<K, V> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<K, V>comparingByKey().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static<K extends Comparable<? super K>, V > Map<K, V> sortByAscKey(Map<K, V> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

}