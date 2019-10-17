package mingmang; 

/**
 * @author Colin
 */
public class MCTS {

    private MingMang mingmang;
    private Game game;
    private CAMMP ai;
    
    private OpeningState root;
    private State state;
    private OpeningState oState;

    private final int SEARCH_TIME = 100;
    private final int MAX_MOVES_IN_BOOK = 20;

    public MCTS(){
        mingmang = new MingMang();
        game = mingmang.getGame();
        ai = new CAMMP(mingmang);
    }
    public void trainQBF2(){

        System.out.println("Loading...");
        Object object = new Tools().load("OpeningBook");
        if(object != null && object instanceof OpeningState){
            System.out.println("Loaded");
            root = (OpeningState)object;
        }
        else {
            State initialState = new State(game.BLACK, mingmang);
            initialState.initialSetup();
            root = new OpeningState(initialState.getHashKey(), null);
        }


        int trail = 0;
        outer: while (true) {

            trail++;
            System.out.println("\n\nTrail: " + trail);

            state = new State(game.BLACK, mingmang);
            state.initialSetup();
            oState = root;
            int result;

            trail: while(!state.isTerminal() && state.getStateNumber() < MAX_MOVES_IN_BOOK){

                double bestScore = 0.1;
                Move bestMove = null;
                int bestIndex = -1;

                if(!oState.movesToChildren.isEmpty()){

                    boolean newMove = false;

                    // every now and then give new moves another chance (to increase book breadth)
                    if(game.rand.nextDouble() < 0.05){
                        
                        int moves = 0;
                        long pieces = state.getPieces()[state.getMovingPlayer()];
                        while(pieces != 0) {
                            moves += game.bitCount(state.getPossibleMoves(game.getLS1Bint(pieces)));
                            pieces &= pieces - 1;
                        }
                        if(oState.movesToChildren.size() <= moves){
                            System.out.println("Random New Move"); 
                            newMove = true;
                        }
                    }

                    // every now and then give bad or mutated moves another chance (for ex. 0/1 would mean mutation but maybe that one game was bad luck)
                    if(!newMove && game.rand.nextDouble() < 0.05)
                    {
                        System.out.println("Random Choice Move");
                        bestIndex = game.rand.nextInt(oState.movesToChildren.size());
                        bestMove = oState.movesToChildren.get(bestIndex);
                    }

                    // else pick best move according to win ratio
                    else if(!newMove){
                        for (int m = 0; m < oState.movesToChildren.size(); m++) {
                            double score = (double)oState.moveWon.get(m) / (double)oState.movePlayed.get(m);
                            if(score > bestScore){
                                bestScore = score;
                                bestMove = oState.movesToChildren.get(m);
                                bestIndex = m;
                            }
                        }
                    }
                }
                
                // if no move in tree better than threshold use ai to pick best move
                if(bestMove == null){

                     // get all possible moves in state which are not in the tree
                    long pieces = state.getPieces()[state.getMovingPlayer()];
                    long[] posMoves = new long[game.BOARD_SIZE*game.BOARD_SIZE];
                    long tempPieces = pieces;
                    while(tempPieces != 0) { // get possible moves for all pieces
                        int from = game.getLS1Bint(tempPieces);
                        posMoves[from] = state.getPossibleMoves(from);
                        if(posMoves[from] == 0) pieces &= ~game.BIN_SQUARE[from];
                        tempPieces &= tempPieces - 1;
                    }
                    for (int m = 0; m < oState.movesToChildren.size(); m++) { // delete tree moves
                        Move move = oState.movesToChildren.get(m);
                        int from = game.getLS1Bint(move.from);
                        posMoves[from] &= ~move.to;
                        if(posMoves[from] == 0) pieces &= ~game.BIN_SQUARE[from];
                    }

                    // iterate through moves
                    int bestABScore = Integer.MAX_VALUE;
                    bestMove = new Move(0,0);
                    int depth = 2; // pessimistic
                    if (game.rand.nextBoolean())
                        depth = 3; // optimistic
                    while(pieces != 0){
                        int from = game.getLS1Bint(pieces);  
                        while(posMoves[from] != 0){
                            int to = game.getLS1Bint(posMoves[from]);

                            if(state.move(game.BIN_SQUARE[from], game.BIN_SQUARE[to])){

//                            System.out.println("Move: "+ mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(game.BIN_SQUARE[from])) + "-"
//                             + mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(game.BIN_SQUARE[to])));


                                ai.getBestMoveDepth(state, depth-1);
                                state.unMove();

                                if(ai.getSearch().getBestScore() < bestABScore){
                                    bestABScore = ai.getSearch().getBestScore();
                                    bestMove.from = game.BIN_SQUARE[from];
                                    bestMove.to = game.BIN_SQUARE[to];
                                }
//                                System.out.println("Score: " + ai.getSearch().getBestScore() + "\n"); 
                            } 

                            posMoves[from] &= (posMoves[from] - 1);
                        }
                        pieces &= (pieces - 1);
                    }

//                    System.out.println("AI Picked: "+ mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(bestMove.from)) + "-"
//                             + mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(bestMove.to)) + " : " + bestABScore); 
                    
                }
 
                if(bestMove.from == bestMove.to){
                    break trail;
                }

                if(bestIndex != -1) {
                    state.move(oState.movesToChildren.get(bestIndex).from, oState.movesToChildren.get(bestIndex).to);
                    oState = oState.children.get(bestIndex);
                }
                else {
                    state.move(bestMove.from, bestMove.to);
                    OpeningState child = new OpeningState(state.getHashKey(), oState);
                    oState.children.add(child);
                    oState.movesToChildren.add(bestMove);
                    oState.movePlayed.add(0);
                    oState.moveWon.add(0);
                    oState = child;
                }
            }
            
            int movingPlayer = state.getMovingPlayer();
            result = playout(state);
            backTrack(oState, movingPlayer, result);

            if(trail % 100 == 0) mingmang.getTools().save(root, "OpeningBook");

        }
    }
    private int playout(State state){
        while(!state.isTerminal() && state.getStateNumber() < 80) { 
            Move bestMove = ai.getBestMoveTime(state, SEARCH_TIME);

            if(bestMove.from == bestMove.to)
                state.pass();
            else if(!state.move(bestMove.from, bestMove.to))
                state.makeRandomMove();
        }
        return this.getResult(state);
    }
    private void backTrack(OpeningState s, int movingPlayer, int result){
        while(s.parent != null){
            int childIndex = s.parent.children.indexOf(s);
            s = s.parent;
            movingPlayer = ~movingPlayer&1;

            s.movePlayed.set(childIndex, s.movePlayed.get(childIndex) + 1);
            if(movingPlayer == result)
                s.moveWon.set(childIndex, s.moveWon.get(childIndex) + 1);  
        }
    }
    public int getResult(State state){
        long[] terr = state.calcTerritories();
        int territoriesBlack = Long.bitCount(terr[game.BLACK]);
        int territoriesWhite = Long.bitCount(terr[game.WHITE]);
        if(territoriesBlack > territoriesWhite)
            return game.BLACK;
        else if(territoriesBlack < territoriesWhite)
            return game.WHITE;
        return -1;
    }

    public int getBookSize(){
        return getBookSize(root);
    }
    public int getBookSize(OpeningState oState){
        int subTreeSize = 1;
        for (int i = 0; i < oState.children.size(); i++) {
            subTreeSize += getBookSize(oState.children.get(i));
        }
        return subTreeSize;
    }
    public void printMoves(){
        for (int i = 0; i < oState.movesToChildren.size(); i++) { 
            System.out.println(mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(oState.movesToChildren.get(i).from)) + "-"
                             + mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(oState.movesToChildren.get(i).to)) +
                             ": " + oState.moveWon.get(i) + " / " + oState.movePlayed.get(i) + " = " +
                             ((double)oState.moveWon.get(i) / (double)oState.movePlayed.get(i)));
        }
    }
    public void cleanUpDepth(){
         Object object = new Tools().load("OpeningBook");
        if(object != null && object instanceof OpeningState){
             root = (OpeningState)object;
        }
        cleanUpDepth(root, 0);
        mingmang.getTools().save(root, "OpeningBook");
    }
    public void cleanUpDepth(OpeningState s, int depth){
        for (int i = 0; i < s.children.size(); i++) {
            if(depth >= 20) {
                s.children.remove(i);
                s.movesToChildren.remove(i);
                s.movePlayed.remove(i);
                s.moveWon.remove(i);
                i--;
            }
            else {
                depth++;
                cleanUpDepth(s.children.get(i), depth);
                depth--;
            }
        }
    }
    public void cleanUpBreath(){
         Object object = new Tools().load("OpeningBook");
        if(object != null && object instanceof OpeningState){
             root = (OpeningState)object;
        }
        else {
             System.out.println("Loading Failed!");
             return;
        }
        cleanUpBreath(root);
        mingmang.getTools().save(root, "OpeningBook");
    }
    public void cleanUpBreath(OpeningState s){
        for (int i = 0; i < s.children.size(); i++) {
            if(s.movePlayed.get(i) < 10 || ((double)s.moveWon.get(i)/(double)s.movePlayed.get(i) <= 0.3)) {
                s.children.remove(i);
                s.movesToChildren.remove(i);
                s.movePlayed.remove(i);
                s.moveWon.remove(i);
                i--;
            }
            else {
                cleanUpBreath(s.children.get(i));
            }
        }

    }
}



//    private final double EXPLORATION_MULTIPLIER = 0.2;
//    public void trainQBF(){
//
//        Object object = new Tools().load("OpeningBook");
//        if(object != null && object instanceof OpeningState)
//            root = (OpeningState)object;
//        else {
//            State initialState = new State(game.BLACK, mingmang);
//            initialState.initialSetup();
//            root = new OpeningState(initialState.getHashKey(), null);
//        }
//
//        outer: while (getBookSize() < MAX_BOOK_SIZE) {
//
//            if(print) System.out.println("Book Size: " + getBookSize());
//
//            state = new State(game.BLACK, mingmang);
//            state.initialSetup();
//            oState = root;
//            int result;
//
//            trail: while(!state.isTerminal() && state.getStateNumber() < 70){
//
//                if(print) {
//                     mingmang.setCurrentState(state);
//                     mingmang.paint();
//                     state.printBoard();
//                     printMoves();
//                }
//
//                if(oState.children.isEmpty() || unExploredChild()){
//                    expand();
//                    result = playout(state.clone());
//                    backTrack(oState, state.getMovingPlayer(), result);
//                    mingmang.getTools().save(root, "OpeningBook");
//                    if(print) System.out.println("Result: " + result);
//                    continue outer;
//                }
//
//                double bestScore = -1;
//                int bestMoveIndex = -1;
//
//                for (int m = 0; m < oState.movesToChildren.size(); m++) {
//                    double score = getUCTScore(m);
//                    if(score > bestScore){
//                        bestScore = score;
//                        bestMoveIndex = m;
//                    }
//                }
//
//                state.move(oState.movesToChildren.get(bestMoveIndex).from, oState.movesToChildren.get(bestMoveIndex).to);
//                oState = oState.children.get(bestMoveIndex);
//            }
//        }
//    }
//    private void expand(){
//
//        Move move;
//        w: while(true){
//            move = state.getRandomMove();
//            for (int i = 0; i < oState.movesToChildren.size(); i++) {
//                if(oState.movesToChildren.get(i).from == move.from &&
//                   oState.movesToChildren.get(i).to == move.to){
//                    continue w;
//                }
//            }
//            break;
//        }
//
//        state.move(move.from, move.to);
//        OpeningState child = new OpeningState(state.getHashKey(), oState);
//        oState.children.add(child);
//        oState.movesToChildren.add(move);
//        oState.movePlayed.add(0);
//        oState.moveWon.add(0);
//        oState = child;
//
//        if(print) System.out.println("Expanded: "+ mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(move.from)) + "-"
//                 + mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(move.to)));
//    }
//    public boolean unExploredChild(){
//        long pieces = state.getPieces()[state.getMovingPlayer()];
//        while(pieces != 0) {
//            long fromBB = game.getLS1Bbb(pieces);
//            long posMoves = state.getPossibleMoves(game.getLS1Bint(fromBB));
//            w: while(posMoves != 0) {
//                long toBB = game.getLS1Bbb(posMoves);
//                posMoves &= posMoves - 1;
//
//                for (int i = 0; i < oState.movesToChildren.size(); i++) {
//                    if(oState.movesToChildren.get(i).from == fromBB &&
//                       oState.movesToChildren.get(i).to == toBB){
//                        continue w;
//                    }
//                }
//                return true;
//            }
//            pieces &= pieces - 1;
//        }
//        return false;
//    }
//    public double getUCTScore(int index){
//        double qScore = (double)oState.moveWon.get(index) / (double)oState.movePlayed.get(index);
//        int childVisits = oState.movePlayed.get(index);
//        int parentVisits = 0;
//        for (int i = 0; i < oState.movePlayed.size(); i++) {
//            parentVisits += oState.movePlayed.get(i);
//        }
//        return qScore + EXPLORATION_MULTIPLIER * Math.sqrt(Math.log((double)parentVisits) / (double)childVisits );
//    }
//    public void printMoves(){
//        for (int i = 0; i < oState.movesToChildren.size(); i++) {
//            int childVisits = oState.movePlayed.get(i);
//            int parentVisits = 0;
//            for (int j = 0; j < oState.movePlayed.size(); j++) {
//                parentVisits += oState.movePlayed.get(j);
//            }
//            System.out.println(mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(oState.movesToChildren.get(i).from)) + "-"
//                             + mingmang.getOutput2d().notationConvert(Long.numberOfTrailingZeros(oState.movesToChildren.get(i).to)) +
//                             ": (" + oState.moveWon.get(i) + " / " + oState.movePlayed.get(i) + " = " + ((double)oState.moveWon.get(i) / (double)oState.movePlayed.get(i))
//                             + ") + " + EXPLORATION_MULTIPLIER + " * (sqrt((ln " + parentVisits + ") / " + childVisits + ") = " +
//                              (EXPLORATION_MULTIPLIER * Math.sqrt(Math.log((double)parentVisits) / (double)childVisits ))
//                              + ") === " + getUCTScore(i));
//        }
//    }
//    public void printTree(){
//        State s = new State(game.BLACK, mingmang);
//        s.initialSetup();
//        printTree(root, s);
//    }
//    public void printTree(OpeningState os, State s){
//        s.printBoard();
//        for (int i = 0; i < os.children.size(); i++) {
//            if(os.movesToChildren.get(i).from == os.movesToChildren.get(i).to)
//                s.pass();
//            else
//                s.move(os.movesToChildren.get(i).from, os.movesToChildren.get(i).to);
//            printTree(os.children.get(i), s);
//            s.unMove();
//        }
//    }
