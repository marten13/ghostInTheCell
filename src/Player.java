import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.floor;

class Player {
    public static List<Factory> factories = new ArrayList<>();
    public static int factoryCount;
    public static List<Troop> troops = new ArrayList<>();
    public static List<Bomb> bombs = new ArrayList<>();
    public static int loopCount;
    public static int myBombCount = 2;
    public static int enemyBombCount = 2;
//    public static List<String> outputStrings = new ArrayList<>();
    public static List<OutputString> outputStringsNew = new ArrayList<>();
    public static List<Integer> currentBombs = new ArrayList<>();

    public static void main(String args[]) {
//        MapUtilTest.testSortByValue();
        Player player = new Player();
        player.run();
    }

    private void run() {
        Scanner in = new Scanner(System.in);

        //Initialization of the factories.
        factoryCount = in.nextInt(); // the number of factories
        for (int x = 0; x < factoryCount; x++) {factories.add(new Factory(x));}
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
        for (Factory f : factories) f.setMaxDistance();

        // game loop
        while (true) {
            long starttime = System.currentTimeMillis();
//            outputStrings.clear();
            outputStringsNew.clear();
            currentBombs.clear();
            loopCount++;

            troops.forEach(troop -> troop.setTurnsTillArrival(troop.getTurnsTillArrival() - 1));
            troops.removeIf(troop -> troop.getTurnsTillArrival() != 0);

            //SCANNER INPUT
            int entityCount = in.nextInt(); // the number of entities (e.g. factories, troops & Bombs)
            for (int i = 0; i < entityCount; i++) {
                int entityId = in.nextInt();
                String entityType = in.next();
                // variables in args:       Factory             Troop               Bomb
                int arg1 = in.nextInt();//  owner               owner               owner
                int arg2 = in.nextInt();//  cyborgs             cyborgs             homeFactory
                int arg3 = in.nextInt();//  production          homeFactory         targetFactory
                int arg4 = in.nextInt();//  turnsTillResumeProd targetFactory       turnsTillArrival
                int arg5 = in.nextInt();//                      turnsTillArrival

                switch (entityType) {
                    case "FACTORY":
                        factories.get(entityId).setArgsInFactory(arg1, arg2, arg3, arg4);
                        break;
                    case "TROOP":
                        troops.add(new Troop(entityId, arg1, arg2, arg3, arg4, arg5));
                        break;
                    case "BOMB":
                        addBombOrFlightTime(entityId, arg1, arg2, arg3, arg4);
                        currentBombs.add(entityId);
                        break;
                    default:
//                        outputStrings.add("MSG nieuw entityType aangeleverd");
                        outputStringsNew.add(new OutputString("MSG", "nieuw entityType aangeleverd"));
                }
            }//End of scanner input

            //PROCESS THE SCANNER INPUT
            updateBomblist(currentBombs);
            //Which factories are mine?
            List<Factory> myFactories = new ArrayList<>();
            List<Factory> notMyFactories = new ArrayList<>();
            factories.forEach(factory -> {
                factory.setConquered(false);
                factory.clearTurnsToArriveAndFriendlyCyborgs();
                factory.resetBombArrivalTurns();
                if (factory.getOwner() == 1) myFactories.add(factory);
                else notMyFactories.add(factory);
            });
            troops.forEach(troop -> factories.get(troop.getTargetFactory()).addTroopArrivalsToFactory(troop));

            //ACTIONS ARE PLANNED FROM HERE ON

            // First Bomb administration. Then calculation of production -> defense -> defense help -> Increments -> Increment help -> attack.

            //Initialize tables with production and troops, accounting for bombs.
            if (loopCount < 2) {
                myFactories.forEach(myFact -> myFact.setEnemyBombOnTheWay(true)); //to prevend immediate upgrade
            } else {
                factories.forEach(myFact -> myFact.canBombArriveNow(bombs));
            }

            for (Factory myFact : myFactories) {
                myFact.setBombTargetAndDistanceThisTurn(0,0);
                myFact.setTurnsAndProducedCyb();
                myFact.setTurnsAndNetCyb();
                myFact.adjustTurnsAndSendableCybForLaterEnemies();
                myFact.adjustTurnsAndSendableCybForProdZero();
                if (myFact.isBombMightArrive()) myFact.setReadyForEvacuation();
            }

            //Send bombs, max 1 per turn
            chooseBombTargetNew();
//            String b = chooseBombTarget();
//            if (!Objects.equals(b, "")) outputStrings.add(b);

            //Help Friends
            sendHelpToFriends();

            //upgrade self
            for (Factory myFact : myFactories) {
                int sendableCyb = myFact.getCurrentSendableCyb();
                drukAf("Factory " + myFact.getEntityId() + " could use for upgrade " + sendableCyb);
                if (sendableCyb > 0) {
                    if (myFact.willIncreaseProd()) {
//                        outputStrings.add("INC " + myFact.getEntityId());
                        outputStringsNew.add(new OutputString("INC", myFact.getEntityId()));
                    }
                }
            }

            //Conquer
            for (Factory notMyFact : notMyFactories) {
                notMyFact.setTurnsAndProducedCyb();
                notMyFact.setTurnsAndNetCyb();
            }
            attack(true);
            attack(false);

            /*for (Factory myFact : myFactories) {
                int sendableCyb = myFact.getCurrentSendableCyb();
                drukAf("Factory " + myFact.getEntityId() + " could use for attacking " + sendableCyb + " max " + myFact.getMaxDistance());
                if (sendableCyb > 0) {
                    outputStrings.addAll(myFact.getConqueringMoves(notMyFactories));
                }
            }*/


//            sendMoves(outputStrings);
            sendMovesNew();

            long duration = System.currentTimeMillis() - starttime;
            drukAf("time: " + duration);
            if (!firstTurn && duration > maxTurnTime) {
                maxTurnTime = duration;
            }
            drukAf("maxTurnTime " + maxTurnTime);
            firstTurn = false;
        }
    }
/*    private boolean prevendTroopWithSameRouteAsBomb(OutputString newMove){
        //NOT usefull when attack is coded right!
        int newHome = newMove.getHomeFactory();
        int newTarget = newMove.getTargetFactory();
        boolean bombWithRouteExists;
        bombWithRouteExists = outputStringsNew.stream()
                .allMatch(outputString -> outputString.getOutputType().equals("BOMB")
                && outputString.getHomeFactory() == newHome
                && outputString.getTargetFactory() == newTarget);
        if (bombWithRouteExists){
            factories.get(newHome).setCurrentSendableCyb(factories.get(newHome).getCurrentSendableCyb() + newMove.getCyborgsAmount());
            //maybe adjust arriving cyb when I start working with them.
        }
        return bombWithRouteExists;
    }*/
    private void sendMovesNew(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("WAIT");
        for(OutputString str: outputStringsNew){
            switch (str.getOutputType().trim()){
                case "INC":
                    buffer.append(";");
                    buffer.append("INC ");
                    buffer.append(str.getHomeFactory());
                    break;
                case "BOMB":
                    buffer.append(";");
                    buffer.append("BOMB ");
                    buffer.append(str.getHomeFactory());
                    buffer.append(" ");
                    buffer.append(str.getTargetFactory());
                    break;
                case "MOVE":
                    buffer.append(";");
                    buffer.append("MOVE ");
                    buffer.append(str.getHomeFactory());
                    buffer.append(" ");
                    buffer.append(str.getTargetFactory());
                    buffer.append(" ");
                    buffer.append(str.getCyborgsAmount());
                    break;
                case "MSG":
                    buffer.append(";");
                    buffer.append("MSG ");
                    buffer.append(str.getMessage());
                default:
            }
        }
        System.out.println(buffer);
    }
    private void sendMoves(List mvs) {
        //ooit in class, voor ontdubbeling met bomb en voor kortere routes door eigen factory
        if (!mvs.isEmpty()) {
            String outputMoves = mvs.toString();
            outputMoves = outputMoves.replace(",", ";");
            outputMoves = outputMoves.replace("; ;", ";");
            outputMoves = outputMoves.replace("]", "");
            outputMoves = outputMoves.replace("[", "");
            System.out.println(outputMoves);
        } else System.out.println("WAIT");
    }

    private void attack(boolean onlyIfConquered){
        //todo determine return on investment before deciding which factories to attack AND help AND upgrade
        //which factories are friends with sendablecyb
        //Sort the enemies on lowest cyborgs
        //find friends to help attack
        List<Factory> friendsWithSendableCyb = factories.stream()
                .filter(friend -> friend.getOwner() == 1)
                .filter(friend -> friend.getCurrentSendableCyb() > 0)
                .collect(Collectors.toList());
        List<Factory> notMyFactories = factories.stream()
                .filter(factory -> factory.getOwner() != 1)
                .collect(Collectors.toList());
        for (Factory friend: friendsWithSendableCyb){
            List<Factory> enemiesNotBombed = notMyFactories.stream()
                    .filter(factory -> friend.getBombTargetThisTurn() != factory.getEntityId())
                    .filter(factory -> friend.getDistanceToOtherFactory(factory.getEntityId()) > factory.getBombArrivalTurns())
                    .filter(factory -> !factory.isConquered())
                    .sorted(Comparator.comparingInt(enemy -> enemy.getDistanceToOtherFactory(friend.getEntityId())))
                    .collect(Collectors.toList());
            for (Factory enemy: enemiesNotBombed){
                if(!enemy.isConquered()) {
                    List<OutputString> potentialOutputStringsNew = new ArrayList<>();
                    enemy.copyTurnsAndNetCybToTurnsAndCybAfterAttack();
                    List<Factory> friendsNotSendingBombWithSendableCyb = friendsWithSendableCyb.stream()
                            .filter(factory -> factory.getBombTargetThisTurn() != enemy.getEntityId())
                            .filter(factory -> enemy.getDistanceToOtherFactory(factory.getEntityId()) > enemy.getBombArrivalTurns())
                            .sorted(Comparator.comparingInt(f -> f.getDistanceToOtherFactory(enemy.getEntityId())))
                            .collect(Collectors.toList());

                    for (Factory helpingFriend : friendsNotSendingBombWithSendableCyb) {
                        if (!enemy.isConquered()) {
                            int shouldSend = 0;
                            int dist = enemy.getDistanceToOtherFactory(helpingFriend.getEntityId());
                            int neededCybToConquer = (enemy.getCybAfterAttack(dist) * -1) + 1;
                            if (helpingFriend.getCurrentSendableCyb() >= neededCybToConquer) {
                                shouldSend = neededCybToConquer;
                                enemy.setCybAfterAttackWithLastingEffect(dist, shouldSend);
                                helpingFriend.setCurrentSendableCyb(helpingFriend.getCurrentSendableCyb() - shouldSend);
                            }
                            if (helpingFriend.getCurrentSendableCyb() < neededCybToConquer) {
                                shouldSend = helpingFriend.getCurrentSendableCyb();
                                enemy.setCybAfterAttackWithLastingEffect(dist, shouldSend);
                                helpingFriend.setCurrentSendableCyb(helpingFriend.getCurrentSendableCyb() - shouldSend);
                            }
                            if (onlyIfConquered) {
                                drukAf("potentialOutputStringsNew size " + potentialOutputStringsNew.size());
                                potentialOutputStringsNew.add(new OutputString("MOVE", helpingFriend.getEntityId(), enemy.getEntityId(), shouldSend));
                                drukAf("potentialOutputStringsNew size " + potentialOutputStringsNew.size());
                            } else {
                                outputStringsNew.add(new OutputString("MOVE", helpingFriend.getEntityId(), enemy.getEntityId(), shouldSend));
                            }
                        }

                    }
                    if (onlyIfConquered) {
                        if (!enemy.isConquered()) {
                            //rollback
                            enemy.copyTurnsAndNetCybToTurnsAndCybAfterAttack();
                            for (OutputString rollbackString : potentialOutputStringsNew) {
                                factories.get(rollbackString.getHomeFactory())
                                        .setCurrentSendableCyb(factories.get(rollbackString.getHomeFactory()).getCurrentSendableCyb() + rollbackString.getCyborgsAmount());
                            }

                        } else {
                            drukAf("OutputStringsNew size " + outputStringsNew.size());
                            outputStringsNew.addAll(potentialOutputStringsNew);
                            drukAf("OutputStringsNew size " + outputStringsNew.size());
                        }
                    }
                }
            }
        }
    }

    private void attackold(boolean onlyIfConquered){
        //todo determine return on investment before deciding which factories to attack AND help AND upgrade
        //which factories are enemies
        //Sort the enemies on lowest (distance to friend-who-did-not-send-bomb-to-you with sendablecyb) > getBombArrivalTurns)
        //dont care about cyborgs in Fact. asume the enemy sees me coming.
        List<Factory> notMyFactories = factories.stream()
                .filter(factory -> factory.getOwner() != 1)
                .collect(Collectors.toList());
        drukAf("bombs size " + bombs.size());
        for (Factory enemy: notMyFactories){
            List<Factory> friendsNotSendingBombWithSendableCyb = factories.stream()
                    .filter(friend -> friend.getBombTargetThisTurn() != enemy.getEntityId())
                    .filter(friend -> friend.getOwner() == 1)
                    .filter(friend -> friend.getCurrentSendableCyb() > 0)
                    .filter(friend -> enemy.getDistanceToOtherFactory(friend.getEntityId()) > enemy.getBombArrivalTurns())
                    .collect(Collectors.toList());
            drukAf("enemy " + enemy.getEntityId() +
                    " bombturns " + enemy.getBombArrivalTurns() +
                    " friendsNotSendingBombWithSendableCyb size " + friendsNotSendingBombWithSendableCyb.size());
            //todo now I'm sorting on closest friend for every enemy instead of sorting on closest enemy for every friend = easiest.
            // todo determine ROI
            if (!friendsNotSendingBombWithSendableCyb.isEmpty()){
                friendsNotSendingBombWithSendableCyb.sort(Comparator.comparingInt(o -> enemy.getDistanceToOtherFactory(o.getEntityId())));
                List<OutputString> potentialOutputStringsNew = new ArrayList<>();
                enemy.copyTurnsAndNetCybToTurnsAndCybAfterAttack();
                for (Factory friend: friendsNotSendingBombWithSendableCyb){
                    if(!enemy.isConquered()){
                        int shouldSend = 0;
                        int dist = enemy.getDistanceToOtherFactory(friend.getEntityId());
                        int neededCybToConquer = (enemy.getCybAfterAttack(dist) * -1) + 1;
                        if (friend.getCurrentSendableCyb() >= neededCybToConquer){
                            shouldSend = neededCybToConquer;
                            enemy.setCybAfterAttackWithLastingEffect(dist, shouldSend);
                            friend.setCurrentSendableCyb(friend.getCurrentSendableCyb() - shouldSend);
                        }
                        if (friend.getCurrentSendableCyb() < neededCybToConquer){
                            shouldSend = friend.getCurrentSendableCyb();
                            enemy.setCybAfterAttackWithLastingEffect(dist, shouldSend);
                            friend.setCurrentSendableCyb(friend.getCurrentSendableCyb() - shouldSend);
                        }
                        if(onlyIfConquered){
                            drukAf("potentialOutputStringsNew size " + potentialOutputStringsNew.size());
                            potentialOutputStringsNew.add(new OutputString("MOVE", friend.getEntityId(), enemy.getEntityId(), shouldSend));
                            drukAf("potentialOutputStringsNew size " + potentialOutputStringsNew.size());
                        }else{
                            outputStringsNew.add(new OutputString("MOVE", friend.getEntityId(), enemy.getEntityId(), shouldSend));
                        }
                    }

                }
                if (onlyIfConquered) {
                    if (!enemy.isConquered()) {
                        //rollback
                        enemy.copyTurnsAndNetCybToTurnsAndCybAfterAttack();
                        for (OutputString rollbackString : potentialOutputStringsNew) {
                            factories.get(rollbackString.getHomeFactory())
                                    .setCurrentSendableCyb(factories.get(rollbackString.getHomeFactory()).getCurrentSendableCyb() + rollbackString.getCyborgsAmount());
                        }

                    } else {
                        drukAf("OutputStringsNew size " + outputStringsNew.size());
                        outputStringsNew.addAll(potentialOutputStringsNew);
                        drukAf("OutputStringsNew size " + outputStringsNew.size());
                    }
                }
            }
        }
    }

    private void sendHelpToFriends() {
        //filter demanders on !isEnemyBombOnTheWay help needed
        //sort demanders on prod at the moment of helpturn
        //filter providers on distance < helpturn
        //sort providers on amount at helpturn - distance
        List<Factory> helpDemandersSortedOnProdNotBombed = factories.stream()
                .filter(factory -> factory.getOwner() == 1)
                .filter(factory -> !factory.isEnemyBombOnTheWay())
                .filter(factory -> factory.getHelpAmount() > 0)
                .collect(Collectors.toList());
        helpDemandersSortedOnProdNotBombed.sort((o1, o2) -> o2.getProducedCyb(o2.getHelpTurns()) - o1.getProducedCyb(o1.getHelpTurns()));

        List<Factory> helpProviders = factories.stream()
                .filter(factory -> factory.getOwner() == 1)
                .filter(factory -> factory.getHelpAmount() == 0)
                .filter(factory -> factory.getCurrentSendableCyb() > 0)
                .collect(Collectors.toList());
        if (!helpProviders.isEmpty()) {
            for (Factory demander : helpDemandersSortedOnProdNotBombed) {
                //providers close enough to help.
                List<Factory> closeHelpproviders = helpProviders.stream()
                        //todo work with bombflighttime
                        .filter(factory -> factory.getDistanceToOtherFactory(demander.getEntityId()) <= demander.getHelpTurns())
                        .collect(Collectors.toList());
                closeHelpproviders.sort(Comparator.comparingInt(o -> o.getDistanceToOtherFactory(demander.getEntityId())));
//                List<String> potentialOutputStrings = new ArrayList<>();
                List<OutputString> potentialOutputStringsNew = new ArrayList<>();
                for (Factory closeHelpProvider : closeHelpproviders) {
                    int shouldSend = 0;
                    if (closeHelpProvider.getCurrentSendableCyb() < demander.getHelpAmount()
                            && closeHelpProvider.getCurrentSendableCyb() > 0) {
                        shouldSend = closeHelpProvider.getCurrentSendableCyb();
                        demander.setHelpAmount(demander.getHelpAmount() - shouldSend);
                        closeHelpProvider.setCurrentSendableCyb(0);
                    }
                    if (closeHelpProvider.getCurrentSendableCyb() >= demander.getHelpAmount()
                            && demander.getHelpAmount() > 0) {
                        shouldSend = demander.getHelpAmount();
                        demander.setHelpAmount(0);
                        closeHelpProvider.setCurrentSendableCyb(closeHelpProvider.getCurrentSendableCyb() - shouldSend);
                    }
//                    if (shouldSend > 0) potentialOutputStrings.add("MOVE " + closeHelpProvider.getEntityId() + " " + demander.getEntityId() + " " + shouldSend);
                    if (shouldSend > 0) {
                        potentialOutputStringsNew.add(new OutputString("MOVE", closeHelpProvider.getEntityId(), demander.getEntityId(), shouldSend));
                        drukAf("potentialOutputStringsNew size " + potentialOutputStringsNew.size());
                    }
                }
                //Only send the help moves if the Factory still survivable!
//                if (demander.getHelpAmount() == 0) outputStrings.addAll(potentialOutputStrings);
                drukAf("outputStringsNew before " + outputStringsNew.size());
                if (demander.getHelpAmount() == 0) {outputStringsNew.addAll(potentialOutputStringsNew);
                    drukAf("outputStringsNew after " + outputStringsNew.size());}
                else {
                    /*evacuate! So turn it into a provider...
                    roll back the move & cyborg administration*/
                    demander.setReadyForEvacuation();
                    for(OutputString rollbackString: potentialOutputStringsNew){
                        factories.get(rollbackString.getHomeFactory())
                                .setCurrentSendableCyb(factories.get(rollbackString.getHomeFactory()).getCurrentSendableCyb() + rollbackString.getCyborgsAmount());
                    }
                }
            }
        }
    }

    private void addBombOrFlightTime(int entityId, int owner, int homeFactory, int targetFactory, int turnsTillArrival) {
        boolean knownBomb = bombs.stream()
                .anyMatch((Bomb b) -> entityId == b.getEntityId());
        if (!knownBomb) {
            bombs.add(new Bomb(entityId, owner, homeFactory, targetFactory, turnsTillArrival));
            if (owner == 1) myBombCount--;
            else {
                enemyBombCount--;
                drukAf("Bomb!");
            }
        }
    }

    private void updateBomblist(List<Integer> currentBombs) {
        bombs.forEach(Bomb::upFlightTime);
        bombs.removeIf(bomb -> !currentBombs.contains(bomb.getEntityId()));
    }

    /*String chooseBombTarget() {
        if (myBombCount > 0) {
            List<Factory> enemies = factories.stream()
                    .filter(factory -> factory.getOwner() == -1)
                    .collect(Collectors.toList());
            List<Factory> enemiesNotBombedYet = enemies.stream()
                    .filter(factory -> !factory.isEnemyBombOnTheWay())
                    .filter(factory -> factory.getTurnsTillResumeProd() == 0)
                    .collect(Collectors.toList());
            // sort asc on prod because enemies produce negative friendly cyborgs. Most productive enemies produce -3 per turn.
            enemiesNotBombedYet.sort(Comparator.comparingInt(o -> o.getProducedCyb(0)));
            List<Factory> enemiesWithTopProdSortOnAmountCyb = enemiesNotBombedYet.stream()
                    .filter(factory -> enemiesNotBombedYet.get(0).getProducedCyb(0) == factory.getProducedCyb(0))
                    .collect(Collectors.toList());
            enemiesWithTopProdSortOnAmountCyb.sort(Comparator.comparing(Factory::getCyborgs).reversed());
            List<Factory> myFactories = factories.stream().filter(f -> 1 == f.getOwner()).collect(Collectors.toList());
            if (!enemiesWithTopProdSortOnAmountCyb.isEmpty() && !myFactories.isEmpty()) {
                int cybInEnemyFactories = enemiesNotBombedYet.stream().map(Factory::getCyborgs).mapToInt(Integer::intValue).sum();
                int cybInFriendFactories = myFactories.stream().map(Factory::getCyborgs).mapToInt(Integer::intValue).sum();
                Factory target = enemiesWithTopProdSortOnAmountCyb.get(0);
                Factory home = myFactories.stream()
                        .sorted(Comparator.comparing(factory -> target.getDistanceToOtherFactory(factory.getEntityId())))
                        .collect(Collectors.toList())
                        .get(0);
                if (myBombCount == 2) return "BOMB " + home.getEntityId() + " " + target.getEntityId();
                if (myBombCount == 1
                        && enemiesNotBombedYet.size() > floor(myFactories.size() * 1.1)
                        && cybInEnemyFactories > floor(cybInFriendFactories * 1.1))
                    return "BOMB " + home.getEntityId() + " " + target.getEntityId();
            }
        }
        return "";
    }*/
    private void chooseBombTargetNew() {
        if (myBombCount > 0) {
            List<Factory> enemies = factories.stream()
                    .filter(factory -> factory.getOwner() == -1)
                    .collect(Collectors.toList());
            List<Factory> enemiesNotBombedYet = enemies.stream()
                    .filter(factory -> !factory.isEnemyBombOnTheWay())
                    .filter(factory -> factory.getTurnsTillResumeProd() == 0)
                    .collect(Collectors.toList());
            // sort asc on prod because enemies produce negative friendly cyborgs. Most productive enemies produce -3 per turn.
            enemiesNotBombedYet.sort(Comparator.comparingInt(o -> o.getProducedCyb(0)));
            List<Factory> enemiesWithTopProdSortOnAmountCyb = enemiesNotBombedYet.stream()
                    .filter(factory -> enemiesNotBombedYet.get(0).getProducedCyb(0) == factory.getProducedCyb(0))
                    .collect(Collectors.toList());
            enemiesWithTopProdSortOnAmountCyb.sort(Comparator.comparing(Factory::getCyborgs).reversed());
            List<Factory> myFactories = factories.stream().filter(f -> 1 == f.getOwner()).collect(Collectors.toList());
            if (!enemiesWithTopProdSortOnAmountCyb.isEmpty() && !myFactories.isEmpty()) {
                Factory target = enemiesWithTopProdSortOnAmountCyb.get(0);
                Factory home = myFactories.stream()
                        .sorted(Comparator.comparing(factory -> target.getDistanceToOtherFactory(factory.getEntityId())))
                        .collect(Collectors.toList())
                        .get(0);
                int dist = home.getDistanceToOtherFactory(target.getEntityId());
//                drukAf("chhose BOMB" + " " + home.getEntityId() + " " + target.getEntityId() + " " + dist);
                if (myBombCount == 2) {
                    target.setEnemyBombOnTheWay(true);
                    target.setBombArrivalTurns(dist);
                    home.setBombTargetAndDistanceThisTurn(target.getEntityId(), dist);
                    outputStringsNew.add(new OutputString("BOMB", home.getEntityId(), target.getEntityId()));
                }
                int cybInEnemyFactories = enemiesNotBombedYet.stream().map(Factory::getCyborgs).mapToInt(Integer::intValue).sum();
                int cybInFriendFactories = myFactories.stream().map(Factory::getCyborgs).mapToInt(Integer::intValue).sum();
                if (myBombCount == 1
                        && enemiesNotBombedYet.size() > floor(myFactories.size() * 1.1)
                        && cybInEnemyFactories > floor(cybInFriendFactories * 1.1)){
                    target.setEnemyBombOnTheWay(true);
                    target.setBombArrivalTurns(dist);
                    home.setBombTargetAndDistanceThisTurn(target.getEntityId(), dist);
                    outputStringsNew.add(new OutputString("BOMB", home.getEntityId(), target.getEntityId()));
                }
            }
        }
//        return new OutputString();
    }

    private void drukAf(String input) {System.err.println(input);}
}
class OutputString {
    private String outputType;
    private int homeFactory;
    private int targetFactory;
    private int cyborgsAmount;
    private String message;

    OutputString() {
    }

    //for INC
    OutputString(String outputType, int homeFactory){
        this.outputType = outputType;
        this.homeFactory = homeFactory;
    }
    //for MOVE
    OutputString(String outputType, int homeFactory, int targetFactory, int cyborgsAmount){
        this.outputType = outputType;
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
        this.cyborgsAmount = cyborgsAmount;
    }
    //for BOMB
    OutputString(String outputType, int homeFactory, int targetFactory){
        this.outputType = outputType;
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
    }
    // for Message
    OutputString(String outputType, String message){
        this.outputType = outputType;
        this.message = message;
    }

    String getOutputType() {
        return outputType;
    }

    int getHomeFactory() {
        return homeFactory;
    }

    int getTargetFactory() {
        return targetFactory;
    }

    int getCyborgsAmount() {
        return cyborgsAmount;
    }

    String getMessage() {
        return message;
    }
}

class Factory extends Entity {
    private int production;
    private int turnsTillResumeProd;
    private Map<Integer, Integer> factoriesDistances = new HashMap<>();
    private int maxDistance;

    //misschien een inner class voor turns
    private Map<Integer, Integer> turnsToArriveAndFriendlyCyborgs = new LinkedHashMap<>();
    private Map<Integer, Integer> turnsAndProducedCyb = new LinkedHashMap<>();
    private Map<Integer, Integer> turnsAndNetCyb = new LinkedHashMap<>();
    private Map<Integer, Integer> turnsAndSendableCyb = new LinkedHashMap<>();
    private Map<Integer, Integer> turnsAndCybAfterAttack = new LinkedHashMap<>();
    private int helpAmount;
    private int helpTurns;
    private boolean enemyBombOnTheWay;
    private int currentSendableCyb;
    private boolean bombMightArrive;
    private int bombArrivalTurns; //in target Factory
    private int bombTargetThisTurn ; //in homeFactory
    private int bombDistanceThisTurn ; //in homeFactory
    private boolean isConquered;


    Factory(int entityId) {
        super(entityId);
    }

    Factory() {
    }

    void canBombArriveNow(List<Bomb> bombs) {
        setBombMightArrive(false);
        setEnemyBombOnTheWay(false);
        for (Bomb bmb : bombs) {
            if (bmb.getOwner() == -1) {
                if (getDistanceToOtherFactory(bmb.getHomeFactory()) > bmb.getFlightTime()+ 1) {
                    setEnemyBombOnTheWay(true);
                    setBombArrivalTurns(getDistanceToOtherFactory(bmb.getHomeFactory())-bmb.getFlightTime());
                }
                if (getDistanceToOtherFactory(bmb.getHomeFactory()) == bmb.getFlightTime() + 1) {
                    drukAf("bombid " + bmb.getEntityId() + " owner " + bmb.getOwner() + " home " + bmb.getHomeFactory() + " target " + bmb.getTargetFactory() + " turns " + bmb.getTurnsTillArrival());
                    setEnemyBombOnTheWay(true);
                    setBombMightArrive(true);
                    setBombArrivalTurns(0);
                    drukAf("bomb CAN arrive at factory " + this.getEntityId());
                    setHelpAmount(0);
                    setHelpTurns(0);
                }
            } else if (this.getEntityId() == bmb.getTargetFactory()) {
                drukAf("bombid " + bmb.getEntityId() + " owner " + bmb.getOwner() + " home " + bmb.getHomeFactory() + " target " + bmb.getTargetFactory() + " turns " + bmb.getTurnsTillArrival());
                setEnemyBombOnTheWay(true);
                setBombArrivalTurns(bmb.getTurnsTillArrival());
                if (getBombArrivalTurns() == 0) {
                    setBombMightArrive(true);
                }
            }
        }
    }
    void setBombArrivalTurns(int turn){bombArrivalTurns = turn;}
    int getBombArrivalTurns(){return this.bombArrivalTurns;}

    //included possible bomb effects. Enemy has negative production
    void setTurnsAndProducedCyb() {
        //max turns to calculate is max Distance to other factories. Base = 0 = this turn
        for (int turn = 0; turn < maxDistance + 1; turn++) {
            int p = 0;
            if (turnsTillResumeProd - turn <= 0) {
                //todo account for future upgrades
                p = getProduction() * getOwner();
            }
            if (getOwner() == 1) {
                if (this.isBombMightArrive() && turn > 0 && turn < 6) p = 0;
            } else {
                if (turn > getBombArrivalTurns() && turn <= getBombArrivalTurns() + 6) p = 0;
            }
            this.turnsAndProducedCyb.put(turn, p);
        }
    }
    int getProducedCyb(int turn) {return turnsAndProducedCyb.getOrDefault(turn, 0);}

    //amount of cyb after (troops arrive && production)
    void setTurnsAndNetCyb() {
        turnsAndNetCyb.clear();
        int friendlyCyborgsInFactory;
        if (getOwner() == 1) friendlyCyborgsInFactory = getCyborgs();
        else friendlyCyborgsInFactory = getCyborgs() * -1;
        for (int turn = 0; turn < maxDistance + 1; turn++) {
            friendlyCyborgsInFactory += getTurnsToArriveAndFriendlyCyborgs(turn);
            if (turn > 0) friendlyCyborgsInFactory += getProducedCyb(turn - 1);
            turnsAndNetCyb.put(turn, friendlyCyborgsInFactory);
        }
    }
    private int getNetCyb(int turn) {return turnsAndNetCyb.getOrDefault(turn, 0);}

    void adjustTurnsAndSendableCybForLaterEnemies() {
    /*save cyb for attacks in transit.
     Set Help amount and turns.
     Set initial currentSendableCyb*/
    //Walk backwards through set to account for attacks to determine how much I should save this turn.
        turnsAndSendableCyb.putAll(turnsAndNetCyb);
        int surplus;
        setHelpAmount(0);
        setHelpTurns(0);
        for (int turn = maxDistance; turn > -1; turn--) {
            surplus = turnsAndNetCyb.getOrDefault(turn, 0);
            if (surplus < 0) {
                helpAmount = (surplus * -1);
                helpTurns = (turn);
            }
            if (helpAmount > 0) {
                turnsAndSendableCyb.put(turn, 0); // make sure I don't send anything
            }
        }
        currentSendableCyb = getSendableCyb(0);
    }
    private int getSendableCyb(int turn) {return turnsAndSendableCyb.getOrDefault(turn, 0);}

    void adjustTurnsAndSendableCybForProdZero() {
        if (getProduction() == 0) {
            //find first turn with max available cyb.
            int turnWithMax = Collections.max(turnsAndSendableCyb.entrySet(), Map.Entry.comparingByValue()).getKey();
            int max = turnsAndSendableCyb.getOrDefault(turnWithMax, 0);
            //will my factory be conquered and is help necessary?
            if (max < 10 && helpAmount == 0) {
                setHelpTurns(turnWithMax);
                setHelpAmount(10 - max);
                setCurrentSendableCyb(0);
            }
        }
    }

    //decide about increase of production
    boolean willIncreaseProd() {
        if (canIncreaseProd() && getCurrentSendableCyb() > 9) {
            setCurrentSendableCyb(getCurrentSendableCyb() - 10);
            return true;
        }
        return false;
    }
    private boolean canIncreaseProd() {
        return this.getProduction() < 3
                && !isEnemyBombOnTheWay()
                && helpAmount == 0;
    }

    int getBombTargetThisTurn() {return bombTargetThisTurn;}
    public int getBombDistanceThisTurn() {return bombDistanceThisTurn;}
    void setBombTargetAndDistanceThisTurn(int bombTargetThisTurn, int bombDistanceThisTurn) {
        this.bombTargetThisTurn = bombTargetThisTurn;
        this.bombDistanceThisTurn = bombDistanceThisTurn;
    }

    void setReadyForEvacuation() {
        setHelpAmount(0);
        setHelpTurns(0);
        setCurrentSendableCyb(Math.max(0, getNetCyb(0)));
        drukAf("Factory evacuate " + getEntityId());
    }

    void copyTurnsAndNetCybToTurnsAndCybAfterAttack(){
        turnsAndCybAfterAttack.putAll(turnsAndNetCyb);
    }
    void setConquered(boolean b) { this.isConquered = b;}
    boolean isConquered(){
        if (isConquered){return true;}
        else {
            int maxFriends = -100000;
            if (!turnsAndCybAfterAttack.isEmpty()) {
                maxFriends = turnsAndCybAfterAttack.values().stream().collect(Collectors.summarizingInt(Integer::intValue)).getMax();
                drukAf("fact " + getEntityId() + "maxFriends " + maxFriends);
            }
            if (maxFriends > 0) {isConquered = true; return true;}
        }
        return false;
    }
    int getCybAfterAttack(int turn){
        return turnsAndCybAfterAttack.getOrDefault(turn,0);
    }

    void setCybAfterAttackWithLastingEffect(int turn, int cyb){
        for (int i = turn; i < maxDistance + 1; i++){
            int oldValue = turnsAndCybAfterAttack.getOrDefault(i, 0);
            turnsAndCybAfterAttack.put(i, oldValue + cyb);
        }
    }

    boolean isEnemyBombOnTheWay() {return enemyBombOnTheWay;}

    void setEnemyBombOnTheWay(boolean enemyBombOnTheWay) {this.enemyBombOnTheWay = enemyBombOnTheWay;}

    int getCurrentSendableCyb() {return this.currentSendableCyb;}

    void setCurrentSendableCyb(int currentSurplus) {this.currentSendableCyb = currentSurplus;}

    int getProduction() {return production;}

    int getTurnsTillResumeProd() {return turnsTillResumeProd;}

    void addDistanceToOtherFactory(int factory2, int distance) {this.factoriesDistances.put(factory2, distance);}

    int getDistanceToOtherFactory(int factory2) {return factoriesDistances.getOrDefault(factory2, 0);}

    void addTroopArrivalsToFactory(Troop troop) {
        int updateWaarde = troop.getOwner() * troop.getCyborgs();
        int oudeWaarde = turnsToArriveAndFriendlyCyborgs.getOrDefault(troop.getTurnsTillArrival(), 0);
        turnsToArriveAndFriendlyCyborgs.put(troop.getTurnsTillArrival(), oudeWaarde + updateWaarde);
    }

    int getHelpAmount() {return helpAmount;}

    void setHelpAmount(int helpAmount) {this.helpAmount = helpAmount;}

    int getHelpTurns() {return helpTurns;}

    void setHelpTurns(int helpTurns) {this.helpTurns = helpTurns;}

    void setSurplusCyb(int turn, int surplus) {this.turnsAndSendableCyb.put(turn, surplus);}

    boolean isBombMightArrive() {return bombMightArrive;}

    private void setBombMightArrive(boolean bombMightArrive) {this.bombMightArrive = bombMightArrive;}

    private int getTurnsToArriveAndFriendlyCyborgs(int turnsTillArrival) {return turnsToArriveAndFriendlyCyborgs.getOrDefault(turnsTillArrival, 0);}

    void clearTurnsToArriveAndFriendlyCyborgs() {turnsToArriveAndFriendlyCyborgs.clear();}

    void setArgsInFactory(int arg1, int arg2, int arg3, int arg4) {
        this.setOwner(arg1);
        this.setCyborgs(arg2);
        production = (arg3);
        turnsTillResumeProd = (arg4);
    }

    private void drukAf(String input) {System.err.println(input);}

    void setMaxDistance() {maxDistance = Sorter.sortByDescValue(factoriesDistances).entrySet().iterator().next().getValue();}

    void resetBombArrivalTurns() {
        bombArrivalTurns = -1;
    }
}

@SuppressWarnings("WeakerAccess")
class Entity {
    private int entityId;
    private int owner;
    private int cyborgs;

    public Entity() {
    }

    //for Troop
    public Entity(int entityId, int owner, int cyborgs) {
        this.entityId = entityId;
        this.owner = owner;
        this.cyborgs = cyborgs;
    }

    //for Factory
    public Entity(int entityId) {
        this.entityId = entityId;
    }

    //for Bomb
    public Entity(int entityId, int owner) {
        this.entityId = entityId;
        this.owner = owner;
    }

    public int getEntityId() {return entityId;}

    public int getOwner() {return owner;}

    public void setOwner(int owner) {this.owner = owner;}

    public int getCyborgs() {return cyborgs;}

    public void setCyborgs(int cyborgs) {this.cyborgs = cyborgs;}

}

class Bomb extends Troop {
//    Idee: ontvlucht de bomb: Je kent de homefact. hou de vluchtduur bij. If isBombMightArrive Than stuur alles weg
//          naar andere fact.
// TODO Maar voor andereEigenFact geldt: dist tot bombHomefact <> vluchtduur+dist this.fact tot andereEigenFact.
//          Anders kunnen ze tegelijk aankomen.

    //      Aanval: voorwaarden: enemyFact cyb > 10 || prdo == 3. Tijdje niets naartoe sturen.
//              Zorg dat mijn troops 1 beurt later kunnen veroveren
    private int flightTime;

    Bomb(int entityId, int owner, int homeFactory, int targetFactory, int turnsTillArrival) {
        super(entityId, owner, homeFactory, targetFactory, turnsTillArrival);
    }

    void upFlightTime() {
        flightTime++;
        System.err.println("bomb nr " + getEntityId() + " flightTime: " + flightTime);
    }

    int getFlightTime() {return flightTime;}
}

class Troop extends Entity {
    private int homeFactory;
    private int targetFactory;
    private int turnsTillArrival;

    //for Bomb
    Troop(int entityId, int owner, int homeFactory, int targetFactory, int turnsTillArrival) {
        super(entityId, owner);
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
        this.turnsTillArrival = turnsTillArrival;
    }

    // for Troop itself
    Troop(int entityId, int owner, int homeFactory, int targetFactory, int cyborgs, int turnsTillArrival) {
        super(entityId, owner, cyborgs);
        this.homeFactory = homeFactory;
        this.targetFactory = targetFactory;
        this.turnsTillArrival = turnsTillArrival;
    }

    int getHomeFactory() {return homeFactory;}

    int getTargetFactory() {return targetFactory;}

    int getTurnsTillArrival() {return turnsTillArrival;}

    void setTurnsTillArrival(int turnsTillArrival) {this.turnsTillArrival = turnsTillArrival;}
}

class Sorter {
    // Van https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
//    Dus gewoon aanroepen met b.v. testMap = Sorter.sortByiets( testMap );
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByAscValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByDescValue(Map<K, V> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static <K extends Comparable<? super K>, V> Map<K, V> sortByDescKey(Map<K, V> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<K, V>comparingByKey().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static <K extends Comparable<? super K>, V> Map<K, V> sortByAscKey(Map<K, V> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

}