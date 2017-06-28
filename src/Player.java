import java.util.*;
import java.util.stream.Collectors;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static List<Factory> factories = new ArrayList<>();
    public static int factoryCount;
    public static List<Troop> troops = new ArrayList<>();
    public static List<Bomb> bombs = new ArrayList<>();
    public static int loopCount;
    public static int myBombCount = 2;
    public static int enemyBombCount = 2;
    //Collect the MOVE's
    public static List<String> moves = new ArrayList<>();

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
        List<Integer> currentBombs = new ArrayList<>();
        for (Factory f: factories) {
            f.setMaxDistance();
        }

        // game loop
        while (true) {
            long starttime = System.currentTimeMillis();
            moves.clear();
            currentBombs.clear();
            loopCount++;

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
                        Troop inputTroop = new Troop(entityId, "TROOP", arg1, arg2, arg3, arg4, arg5);
                        troops.add(inputTroop);
                        factories.get(inputTroop.getTargetFactory()).addTroopArrivalsToFactory(inputTroop);
                        break;
                    case "BOMB":
                        addBombOrFlightTime( entityId,  arg1,  arg2,  arg3,  arg4);
                        currentBombs.add(entityId);
                }
            }//End of scanner input

            updateBomblist(currentBombs);
            //Which factories are mine?
            List<Factory> myFactories = listMyFact();
            //Which factories are NOT mine?
            List<Factory> notMyFactories = listEnemyFact();

            // komt eerst de bom? Dan kan je production bepalen, dan defense, help en attack.
            //organise own defense
            for (Factory myFact: myFactories) {
                if (loopCount < 3){
                    myFact.setEnemyBombOnTheWay(true); //to prevend immediate upgrade
                }else{
                    myFact.setEnemyBombOnTheWay(false);
                    myFact.setBombMightArrive(myFact.canBombArriveNow(bombs));
                }
                myFact.setTurnsAndProducedCyb(bombs);
                myFact.setTurnsAndSurplusCyb();
                myFact.adjustTurnsAndSurplusCybForLaterEnemies();
            }
            //Help Friends
            for (Factory myFact: myFactories) {
                myFact.adjustTurnsAndSurplusCybForProdZero();
                int sendableCyb = myFact.getCurrentSurplusCybAvailableForSending();
                drukAf("Factory " + myFact.getEntityId() + " can help with " + sendableCyb);
                if (sendableCyb > 0) {
                    //Help if I can send help in time
                    if (myBombCount == 2){
                        int target = myFact.chooseBombTarget(notMyFactories, bombs);
                        if (target > -1){
                            moves.add("BOMB " + myFact.getEntityId() + " " + target);
                        }
                    }
                    if (!myFact.isBombSender()){
                        moves.addAll(myFact.getFactoriesICanHelp(myFactories));
                    }
                }else{
                    drukAf("my bombs " + myBombCount);
                    if (myBombCount > 0){
                        int target = myFact.chooseBombTarget(notMyFactories, bombs);
                        if (target > -1){
                            moves.add("BOMB " + myFact.getEntityId() + " " + target);
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
                            moves.add("INC " + myFact.getEntityId());
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
                        moves.addAll(myFact.getFactoriesICanHelpToUpgrade(myFactories));
                    }
                }
            }
            //Conquer
            for (Factory notMyFact: notMyFactories) {
                notMyFact.setBombMightArrive(notMyFact.canBombArriveNow(bombs));
                notMyFact.setTurnsAndProducedCyb(bombs);
                notMyFact.setTurnsAndSurplusCyb();
            }
            for (Factory myFact: myFactories) {
                if (!myFact.isBombSender()) {
                    int sendableCyb = myFact.getCurrentSurplusCybAvailableForSending();
                    drukAf("Factory " + myFact.getEntityId() + " could use for attacking " + sendableCyb + " max " + myFact.getMaxDistance());
                    if (sendableCyb > 0) {
                        moves.addAll(myFact.getConqueringMoves(notMyFactories));
                    }
                }
            }

            sendMoves(moves);
            
            troops.clear();
            
            long duration = System.currentTimeMillis() - starttime;
            drukAf("time: " + duration);
            if (!firstTurn && duration > maxTurnTime) {maxTurnTime = duration;}
            drukAf("maxTurnTime " + maxTurnTime);
            firstTurn = false;
        }
    }


    private void sendMoves(List mvs){
        if (mvs.size()>0) {
            String outputMoves = mvs.toString();
            outputMoves = outputMoves.replace(",", ";");
            outputMoves = outputMoves.replace("]", "");
            outputMoves = outputMoves.replace("[", "");
            System.out.println(outputMoves);
        }else{
            System.out.println("WAIT");
        }
    }
    
    private List listMyFact(){
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
    private List listEnemyFact(){
        //Which factories are not mine? (incl neutral)
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

    private void addBombOrFlightTime(int entityId, int owner, int homeFactory, int targetFactory, int turnsTillArrival){
        boolean knownBomb = false;
        for (Bomb bomb: bombs){
            drukAf("Bombs id" + bomb.getEntityId() + " new id " + entityId);
            if(bomb.getEntityId() == entityId){
                knownBomb = true;
            }
            bomb.upFlightTime();
        }
        if(!knownBomb){
            drukAf("new bomb " + entityId);
            bombs.add( new Bomb(entityId, "BOMB", owner, homeFactory, targetFactory, turnsTillArrival));
            if (owner == 1) {
                myBombCount = myBombCount - 1;
            }else{
                enemyBombCount = enemyBombCount - 1;
                drukAf("Bomb!" );
            }
        }
    }
    private  void updateBomblist(List<Integer> currentBombs){
        if (bombs.size() > 0) {
            Iterator <Bomb> b = bombs.iterator();
            while (b.hasNext()){
                Bomb tmpbmb = b.next();
                boolean found = false;
                for (int bmb : currentBombs) {
                    if (tmpbmb.getEntityId() == bmb) {
                        found = true;
                    }
                }
                if (!found) {
                    b.remove();
                }
            }
        }
    }

    public  void drukAf(String input){
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

//TODO:
//    Idee: ontvlucht de bomb: Je kent de homefact. hou de vluchtduur bij. If vluchtduur = dist  Than stuur alles weg
//          naar andere fact. Maar voor andereEigenFact geldt: dist tot bombHomefact <> vluchtduur+dist this.fact tot andereEigenFact.
//          Anders kunnen ze tegelijk aankomen.

//      Aanval: voorwaarden: enemyFact cyb > 30. Tijdje niets naartoe sturen.
//              Zorg dat mijn troops 1 beurt later kunnen veroveren
    private int flightTime;
    Bomb(){}
    Bomb(int entityId, String entityType, int owner, int homeFactory, int targetFactory, int turnsTillArrival){
        super(entityId, entityType, owner, homeFactory, targetFactory, turnsTillArrival);
        flightTime++;
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
    Troop(int entityId, String entityType, int owner, int homeFactory, int targetFactory, int cyborgs, int turnsTillArrival){
        super(entityId, entityType, owner, cyborgs);
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

}

class Factory extends Entity{
    private int production;
    private int turnsTillResumeProd;
    //Distance from this factory to all others:
    private Map<Integer, Integer> factoriesDistances = new HashMap<>();
    private int maxDistance;

    //misschien een inner class voor turns
    private Map<Integer, Integer> turnsToArriveAndFriendlyCyborgs = new LinkedHashMap<>();
    private List<Integer> turnsAndProducedCyb = new ArrayList<>();
    private List<Integer> turnsAndSurplusCyb = new ArrayList<>();
    private int helpAmount;
    private int helpTurns;
    private boolean enemyBombOnTheWay;
    private int cybNotForDefense;
    private int currentSurplusCybAvailableForSending;
    private boolean bombMightArrive;
    private boolean BombSender;


    Factory(int entityId){
        super(entityId);
    }
    public boolean canBombArriveNow(List<Bomb> bombs) {
        this.setBombMightArrive(false);
        this.setEnemyBombOnTheWay(false);
        for (Bomb bmb : bombs) {
            if (bmb.getOwner() == -1 && this.getOwner() == 1) {
//                drukAf("this fact " + this.getEntityId() + " afstand tot bomb maker " + this.getDistanceToOtherFactory(bmb.getHomeFactory())
//                        + " flightTime " + bmb.getFlightTime());
                if (this.getDistanceToOtherFactory(bmb.getHomeFactory()) > bmb.getFlightTime()) {
                    this.setEnemyBombOnTheWay(true);
                    this.setBombMightArrive(false);
                }
                if (this.getDistanceToOtherFactory(bmb.getHomeFactory()) == bmb.getFlightTime()) {
                    this.setEnemyBombOnTheWay(true);
                    this.setBombMightArrive(true);
                    drukAf("bomb CAN arrive at factory " + this.getEntityId());
                    this.setHelpAmount(0);
                    this.setCybNotForDefense(this.getCyborgs());
                    this.setCurrentSurplusCybAvailableForSending(this.getCyborgs());
                    return true;
                }
                if (this.getDistanceToOtherFactory(bmb.getHomeFactory()) < bmb.getFlightTime()){
                    this.setBombMightArrive(false);
                    this.setEnemyBombOnTheWay(false);
                    return false;
                }
            }else {
                if (bmb.getTurnsTillArrival() == 0 && this.getEntityId() == bmb.getTargetFactory()) {
                    this.setBombMightArrive(true);
                    this.setHelpAmount(0);
                    return true;
                }
            }
        }
        return false;
    }

    //included possible bomb effects. Enemy has negative production
    public void setTurnsAndProducedCyb(List<Bomb> bombs){
        this.turnsAndProducedCyb.clear();
        //max turns to calculate is max Distance to other factories. Base = 0 = this turn
        for (int turn = 0; turn < this.maxDistance + 1; turn++){
            int p;
            if (this.turnsTillResumeProd - turn <= 0){
                p = this.getProduction() * this.getOwner();
            } else {
                p = 0;
            }
            if (this.getOwner() == 1) {
                if(this.isBombMightArrive() && turn > 0 && turn < 5){
                    this.turnsAndProducedCyb.add(turn, 0);
                }else{
                    this.turnsAndProducedCyb.add(turn, p);
                }
            }else {
                if (bombs.size() == 0) {
                    this.turnsAndProducedCyb.add(turn, p);
                }else {
                    for (Bomb bmb : bombs) {
                        if (bmb.getOwner() == 1 &&
                                this.getEntityId() == bmb.getTargetFactory() &&
                                turn >= bmb.getTurnsTillArrival() &&
                                turn <= bmb.getTurnsTillArrival() + 5) {
                            this.turnsAndProducedCyb.add(turn, 0);
                        } else {
                            this.turnsAndProducedCyb.add(turn, p);
                        }
                    }
                }
            }
        }
    }

    public int getProducedCyb(int turn){
        return turnsAndProducedCyb.get(turn);
    }
    //amount of cyb after (troops arrive && production)
    public void setTurnsAndSurplusCyb() {
        this.turnsAndSurplusCyb.clear();
        int friendlyCyborgsInFactory = 0;
        if (this.getOwner() == 1){
            friendlyCyborgsInFactory = this.getCyborgs() ;
        }
        this.turnsToArriveAndFriendlyCyborgs = Sorter.sortByAscKey(this.turnsToArriveAndFriendlyCyborgs);
        for (int turn = 0; turn < this.maxDistance + 1; turn++) {
            friendlyCyborgsInFactory = friendlyCyborgsInFactory + this.getTurnsToArriveAndFriendlyCyborgs(turn);
            friendlyCyborgsInFactory = friendlyCyborgsInFactory + this.getProducedCyb(turn);
            this.turnsAndSurplusCyb.add(turn, friendlyCyborgsInFactory);
        }
        this.currentSurplusCybAvailableForSending = this.turnsAndSurplusCyb.get(0) - this.turnsAndProducedCyb.get(0); //made after sending.
    }

    public int getSurplusCyb(int turn) {
        return this.turnsAndSurplusCyb.get(turn);
    }

    //save cyb for attacks in transit.
    // Set Help amount and turns.
    // Set initial currentSurplusCybAvailableForSending
    public void adjustTurnsAndSurplusCybForLaterEnemies() {
        //Walk backwards through set to account for attacks
        //      to determine how much I should save this turn.
        int surplus = 0;
        this.setHelpAmount(0);
        this.setHelpTurns(0);
        for (int turn = this.maxDistance; turn > -1 ; turn--) {
            surplus = surplus + this.turnsAndSurplusCyb.get(turn);
            if (surplus >= 0) {
                this.setHelpTurns(0);
                surplus = 0; //een positief surplus is zinloos NA een aanval.
            } else {
                if (this.getHelpTurns() == 0){
                    this.setHelpTurns(turn); //first attack where assistance is needed
                }
                this.turnsAndSurplusCyb.set(turn, 0); //zodat je ze bewaart
            }
        }
        this.setHelpAmount(surplus * -1);
        this.currentSurplusCybAvailableForSending = this.turnsAndSurplusCyb.get(0) - this.turnsAndProducedCyb.get(0); //made after sending.
    }
    public void adjustTurnsAndSurplusCybForProdZero(){
        if (this.getProduction() == 0) {
            int need = 10 - this.getCyborgs();
            this.setHelpTurns(1);
            this.setHelpAmount(need);
            for (int turn = 0; turn < this.maxDistance + 1; turn++) {
                    this.turnsAndSurplusCyb.set(turn, need * -1); //enough to upgrade
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
        if (this.canIncreaseProd() && this.getCurrentSurplusCybAvailableForSending() > 9 ){
            this.setCurrentSurplusCybAvailableForSending(this.getCurrentSurplusCybAvailableForSending() - 10);
            return true;
        }
        return false;
    }

    public boolean canIncreaseProd(){
        if (this.getProduction() < 3 && this.getHelpAmount() == 0 && this.getHelpTurns() == 0 && !this.isEnemyBombOnTheWay()){
            return true;
        }
        return false;
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
        this.turnsToArriveAndFriendlyCyborgs.put(troop.getTurnsTillArrival(), oudeWaarde + updateWaarde);
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
        this.turnsAndSurplusCyb.set(turn, surplus);
    }
    public int getCybNotForDefense() {
        return cybNotForDefense;
    }
    public void setCybNotForDefense(int cybNotForDefense) {
        this.cybNotForDefense = cybNotForDefense;
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
        this.setEntityType("FACTORY");
        this.setOwner(arg1);
        this.setCyborgs(arg2);
        this.production = (arg3);
        this.turnsTillResumeProd =(arg4);
        //and clear turnsToArriveAndFriendlyCyborgs
        this.clearTurnsToArriveAndFriendlyCyborgs();
    }

    private void drukAf(String input){System.err.println(input);}
    public void setMaxDistance(){
        maxDistance = Sorter.sortByDescValue(factoriesDistances).entrySet().iterator().next().getValue();
//        if (this.getEntityId() == 1) {
//            drukAf("" + this.getMaxDistance());
//            this.factoriesDistances = Sorter.sortByDescValue(this.factoriesDistances);
//            Iterator it = factoriesDistances.entrySet().iterator();
//            while (it.hasNext()) {
//                Map.Entry pair = (Map.Entry) it.next();
//                drukAf("" + pair.getKey() + " = " + pair.getValue());
//            }
//        }
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public int chooseBombTarget(List<Factory> enemyFactories, List<Bomb> bombs) {
        //TODO sort on prod.
        for(Factory ef: enemyFactories){
            drukAf("prod " + ef.getProduction() + " cyb avail " + ef.getCurrentSurplusCybAvailableForSending() + " own " + ef.getOwner());
            if (ef.getProduction() > 1 && ef.getCurrentSurplusCybAvailableForSending() < -1 && ef.getOwner() == -1){
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
    
    public String getEntityType() {
        return entityType;
    }
}

class MapUtilTest
{
    public static void testSortByValue()
    {
        Random random = new Random(System.currentTimeMillis());
        int size = 10;
        Map<Integer, Integer> testMap = new HashMap<Integer, Integer>(size);
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