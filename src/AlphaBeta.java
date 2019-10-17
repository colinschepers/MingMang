package mingmang;
import java.io.File;

/**
 * @author Colin
 */
public class AlphaBeta implements Search {

    private CAMMP ai;
    private EvaluationFunction evalF;

    private int depth;
    private int nodesVisited;
    private int quiNodesVisited;

    private  HashState[] transpositionTable1;
    private  HashState[] transpositionTable2;
    private          int tTableSize = (1 << 16);
    private       Move[] killerMove1;
    private KillerMove[] killerMove2;
    private      int[][] historyTable;

    private State state;
    private  Move bestMove;
    private   int bestScore;
    private Move guessMove;

    private boolean stopThinking;
    private long start, searchTime, oppTime;

    private final int EXTENDED_DEPTH = 6, FULL_DEPTH_MOVES = 5;
    private final int DEPTH_REDUCTION = 2, MULTICUT_M = 10, MULTICUT_C = 3;
    private final int EXACT = 0, LOWERBOUND = 1, UPPERBOUND = 2;

    public AlphaBeta(CAMMP ai)  {
        this.ai = ai; 
        this.evalF = new EvaluationFunction(ai.mingmang, ai);
        transpositionTable1 = new HashState[tTableSize];
        transpositionTable2 = new HashState[tTableSize];
        historyTable = new int[ai.game.BOARD_SIZE*ai.game.BOARD_SIZE][ai.game.BOARD_SIZE*ai.game.BOARD_SIZE];
        initKillers();
    }

    public void reset(){
        transpositionTable1 = new HashState[tTableSize];
        transpositionTable2 = new HashState[tTableSize];
        historyTable = new int[ai.game.BOARD_SIZE*ai.game.BOARD_SIZE][ai.game.BOARD_SIZE*ai.game.BOARD_SIZE];
        initKillers();
    }

    public Move getBestMove(State state, int searchTime){

        this.state = state;

        if(guessMove != null && ai.mingmang.getLastMove() != null &&
                ai.mingmang.getLastMove().from == guessMove.from &&
                ai.mingmang.getLastMove().to == guessMove.to){

            ai.mingmang.output("Guessed right!");

           if(oppTime >= searchTime){
               ai.mingmang.output("Immediate move: " +
                           ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.from)) + " - " +
                           ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.to)));
               return bestMove;
           }
           searchTime -= oppTime;
        }
         else {
            transpositionTable1 = new HashState[tTableSize];
            transpositionTable2 = new HashState[tTableSize];
            depth = 0;
         }

         
        ai.mingmang.output("Thinking for at most " + searchTime + " ms\nMy Current score: ");
        evalF.outputEvaluation(state);

        int scoreBefore = evalF.evaluate(state);
        int value = scoreBefore;
        nodesVisited = 0;
        quiNodesVisited = 0;
        Move bestValidMove = bestMove;
        this.searchTime = searchTime; 
        start = System.currentTimeMillis();
        long start2, nodesExp, quiNodesExp;
        int alpha = Integer.MIN_VALUE + 1, beta = Integer.MAX_VALUE - 1, delta = 50;
        stopThinking = false;

        while(!stopThinking) {

            start2 = System.currentTimeMillis();
            nodesExp = nodesVisited;
            quiNodesExp = quiNodesVisited;
            depth += 2;
            if(depth >= 50){ 
                bestMove = new Move(0,0);
                break;
            }

            bestScore = value;
            value = alphaBeta(alpha, beta, depth, false);

            if(!stopThinking) bestValidMove = bestMove.clone();
            if((System.currentTimeMillis() - start) + ((System.currentTimeMillis() - start2)*2) > searchTime)
                stopThinking = true;

            ai.mingmang.output("Depth " + depth + "(+" + EXTENDED_DEPTH + "): " + (System.currentTimeMillis() - start2) + "ms | "
                    + (nodesVisited - nodesExp) + " + " + (quiNodesVisited - quiNodesExp) + " nodes visited | score = " + value + " | move: " +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.from)) + "-" +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.to)));
        }

        ai.mingmang.output("Total search: " + (System.currentTimeMillis() - start) + "ms | "
                    + nodesVisited + " + " + quiNodesVisited + " nodes visited | score: " + bestScore + " | move: " +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestValidMove.from)) + "-" +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestValidMove.to)));

        moveUpKillers();

        long[] terr = state.calcTerritories();
        if(ai.game.bitCount(terr[state.getMovingPlayer()]) >
                ((ai.game.BOARD_SIZE*ai.game.BOARD_SIZE) / 2)
                    && bestScore <= scoreBefore){
            return ai.game.pass(state.getMovingPlayer());
        }

        ai.mingmang.output("Territories: " + ai.game.bitCount(terr[ai.game.BLACK]) + " - " + ai.game.bitCount(terr[ai.game.WHITE]));

        return bestValidMove;
    }

    public int alphaBeta(int alpha, int beta, int depth, boolean nullMoved){ 

        nodesVisited++;
        
        // retreive TT values and check for a-b update or cut off
        int oldAlpha = alpha;
        HashState hash = null;
        int hashIndex = (int)(state.getHashKey() & (tTableSize-1));
        if (isValid(transpositionTable1[hashIndex], state.getHashKey())) {
            hash = transpositionTable1[hashIndex];
            if(hash.depth >= depth){
                if(hash.type == EXACT)
                    return hash.value;
                else if(hash.type == LOWERBOUND)
                    alpha = ai.game.max(alpha, hash.value);
                else if(hash.type == UPPERBOUND)
                    beta = ai.game.min(beta, hash.value);
		if(alpha >= beta)
                    return hash.value;
            }
        }
        else if(isValid(transpositionTable2[hashIndex], state.getHashKey())) {
            hash = transpositionTable2[hashIndex];
            if(hash.depth >= depth){
                if(hash.type == EXACT)
                    return hash.value;
                else if(hash.type == LOWERBOUND)
                    alpha = ai.game.max(alpha, hash.value);
                else if(hash.type == UPPERBOUND)
                    beta = ai.game.min(beta, hash.value);
		if(alpha >= beta)
                    return hash.value;
            }
        }

        // exit node if search time over or opponent moves
        if((start != -1 && ((System.currentTimeMillis() - start) > searchTime)) ||
                (start == -1 && ai.mingmang.getCurrentState().getMovingPlayer() == ai.getPlayerColor())) {
            stopThinking = true;
            return evalF.evaluate(state);
        }
        // exit node and move to q-search if depth is 0 or terminal node
        else if(depth == 0 || state.isTerminal()) {
            return quiescenceSearch(alpha, beta, EXTENDED_DEPTH);
        }

        Move best = new Move(0,0);
        int nodesVisitedInitial = nodesVisited;
        int score;

        prune: while(true) {

            int value;
            score = Integer.MIN_VALUE + 1;
            int movesSearched = 0;

            // null-move
            if(!nullMoved && depth > DEPTH_REDUCTION && depth < this.depth){
                state.pass();
                value = -alphaBeta(-beta, -beta+1, depth-1-DEPTH_REDUCTION, true);
                state.unMove();
                if(value >= beta) return value;
            }
            else nullMoved = false;
            
            // get info on pieces and their possible moves
            long pieces = state.getPieces()[state.getMovingPlayer()];
            long[] posMoves = new long[ai.game.BOARD_SIZE*ai.game.BOARD_SIZE];
            long tempPieces = pieces;
            while(tempPieces != 0) {
                int from = ai.game.getLS1Bint(tempPieces);
                posMoves[from] = state.getPossibleMoves(from);
                if(posMoves[from] == 0) pieces &= ~ai.game.BIN_SQUARE[from];
                tempPieces &= tempPieces - 1;
            }

            int to, from, i = 0;
            Move[] bestMoves = new Move[MULTICUT_M];

            // get transposition table's move
            if (hash != null) {
                from = ai.game.getLS1Bint(hash.from);
                if(!(hash.from == 0 && hash.to == 0) && (posMoves[from] & hash.to) != 0) {

                    bestMoves[i] = new Move(hash.from, hash.to);
                    i++;

                    posMoves[from] &= ~hash.to;
                    if(posMoves[from] == 0) pieces &= ~hash.from;
                }
            }

            // check killer move 1
            if(!(killerMove1[(this.depth - depth)].from == 0 && killerMove1[(this.depth - depth)].to == 0)){
                from = ai.game.getLS1Bint(killerMove1[(this.depth - depth)].from);
                if((posMoves[from] & killerMove1[(this.depth - depth)].to) != 0) {

                    bestMoves[i] = new Move(killerMove1[(this.depth - depth)].from, killerMove1[(this.depth - depth)].to);
                    i++;

                    posMoves[from] &= ~killerMove1[(this.depth - depth)].to;
                    if(posMoves[from] == 0) pieces &= ~killerMove1[(this.depth - depth)].from;
                }
            }

            // check killer move 2
            if(!(killerMove2[(this.depth - depth)].from == 0 && killerMove2[(this.depth - depth)].to == 0)){
                from = ai.game.getLS1Bint(killerMove2[(this.depth - depth)].from);
                if((posMoves[from] & killerMove2[(this.depth - depth)].to) != 0) {

                    bestMoves[i] = new Move(killerMove2[(this.depth - depth)].from, killerMove2[(this.depth - depth)].to);
                    i++;

                    posMoves[from] &= ~killerMove2[(this.depth - depth)].to;
                    if(posMoves[from] == 0) pieces &= ~killerMove2[(this.depth - depth)].from;
                }
            }

            Move mostPromisingMove;
            int mostPromisingValue;
            long tempMoves;

            // multi-cut
            i = 0;
            int c = 0;
            if(depth > DEPTH_REDUCTION && depth < this.depth) {
                while(pieces != 0 &&i < MULTICUT_M) {
                    if(bestMoves[i] != null){
                        mostPromisingMove = bestMoves[i];
                    }
                    else {
                        mostPromisingMove = null;
                        mostPromisingValue = Integer.MIN_VALUE;
                        tempPieces = pieces;
                        while(tempPieces != 0){
                            from = ai.game.getLS1Bint(tempPieces);
                            tempMoves = posMoves[from];
                            while(tempMoves != 0){
                                to = ai.game.getLS1Bint(tempMoves);
                                if(historyTable[from][to] > mostPromisingValue) {
                                    mostPromisingMove = new Move(ai.game.BIN_SQUARE[from], ai.game.BIN_SQUARE[to]);
                                    mostPromisingValue = historyTable[from][to];
                                }
                                tempMoves &= (tempMoves - 1);
                            }
                            tempPieces &= (tempPieces - 1);
                        }

                        if(mostPromisingMove == null) break;
                        bestMoves[i] = mostPromisingMove;
                    }

                    if(state.move(mostPromisingMove.from,  mostPromisingMove.to)){
                        value = -alphaBeta( -beta, -alpha, depth-1-DEPTH_REDUCTION, true);
                        state.unMove();
                        if(value >= beta){
                           c++;
                           if(c >= MULTICUT_C) return beta;
                        }
                    }

                    i++;

                    from = ai.game.getLS1Bint(mostPromisingMove.from);
                    posMoves[from] &= ~mostPromisingMove.to;
                    if(posMoves[from] == 0) pieces &= ~mostPromisingMove.from;
                }
            }

            // go through moves
            i = 0;
            while(pieces != 0) {      
                // find best move according to history heuristic
                if(i < bestMoves.length && bestMoves[i] != null){
                    mostPromisingMove = bestMoves[i];
                    i++;
                }
                else {
                    mostPromisingMove = new Move(0,0);
                    mostPromisingValue = Integer.MIN_VALUE;
                    tempPieces = pieces;
                    while(tempPieces != 0){
                        from = ai.game.getLS1Bint(tempPieces);
                        tempMoves = posMoves[from];
                        while(tempMoves != 0){
                            to = ai.game.getLS1Bint(tempMoves);
                            if(historyTable[from][to] > mostPromisingValue) {
                                mostPromisingMove = new Move(ai.game.BIN_SQUARE[from], ai.game.BIN_SQUARE[to]);
                                mostPromisingValue = historyTable[from][to];
                            }
                            tempMoves &= (tempMoves - 1);
                        }
                        tempPieces &= (tempPieces - 1);
                    }
                }

                if(mostPromisingMove.from == 0 && mostPromisingMove.to == 0) break;

                // move (if possible) and evaluate further with alpha beta
                if(state.move(mostPromisingMove.from,  mostPromisingMove.to)){

                    // late move reduction
                    if(movesSearched >= this.FULL_DEPTH_MOVES && depth > DEPTH_REDUCTION && depth < this.depth)
                        value = -alphaBeta(-beta, -alpha, depth-1-DEPTH_REDUCTION, nullMoved);
                    else
                        value = -alphaBeta(-beta, -alpha, depth-1, nullMoved);

                    state.unMove();
                    movesSearched++;

                    if(value > score) {
                        score = value;
                        best.from = mostPromisingMove.from;
                        best.to = mostPromisingMove.to;
                    }
                    if(score > alpha) alpha = score;
                    if(score >= beta){
                        killerMove1[(this.depth - depth)] = new Move(mostPromisingMove.from, mostPromisingMove.to);
                        break prune;
                    }
                }
                // remove this move
                from = ai.game.getLS1Bint(mostPromisingMove.from);
                posMoves[from] &= ~mostPromisingMove.to;
                if(posMoves[from] == 0) pieces &= ~mostPromisingMove.from;
            }
            break prune;
        }

        // if exiting root -> set best move found
        if(depth == this.depth && !stopThinking){ 
            bestMove = best;
        }

        // store in TT
        int flag = EXACT;
        if(score <= oldAlpha)  flag = LOWERBOUND;
        else if(score >= beta) flag = UPPERBOUND;
        storeTTEntry(state.getHashKey(), score, flag, best.from, best.to, depth, (nodesVisited - nodesVisitedInitial));

        // store killer move 2 (best valued move)
        if(score > killerMove2[(this.depth - depth)].value)
            killerMove2[(this.depth - depth)] = new KillerMove(score, best.from, best.to);

        // update best move in history table
        historyTable[ai.game.getLS1Bint(best.from)][ai.game.getLS1Bint(best.to)] += depth * depth;

        return score;
    }

    public int quiescenceSearch(int alpha, int beta, int depth){
        
        int score = evalF.evaluate(state);
        if( score >= beta) return score;
        if( score > alpha) alpha = score;

        if((start != -1 && ((System.currentTimeMillis() - start) > searchTime)) ||
                (start == -1 && ai.mingmang.getCurrentState().getMovingPlayer() == ai.getPlayerColor())) {
            stopThinking = true;
            return score;
        }
        else if(isQuiet(state, depth) || depth == 0 || state.isTerminal()) {
            return score;
        }

        quiNodesVisited++;

        // get info on own pieces and possible moves
        long pieces = state.getPieces()[state.getMovingPlayer()];
        long[] posMoves = new long[ai.game.BOARD_SIZE*ai.game.BOARD_SIZE];
        long tempPieces = state.getPieces()[state.getMovingPlayer()];
        while(tempPieces != 0) {
            int from = ai.game.getLS1Bint(tempPieces);
            posMoves[from] = state.getPossibleMoves(from);
            if(posMoves[from] == 0) pieces &= ~ai.game.BIN_SQUARE[from];
            tempPieces &= tempPieces - 1;
        }

        // go through the moves
        while(pieces != 0) {
            // find best move according to history heuristic
            Move mostPromisingMove = new Move(0,0);
            int mostPromisingValue = Integer.MIN_VALUE;
            tempPieces = pieces;
            while(tempPieces != 0){
                int from = ai.game.getLS1Bint(tempPieces);
                long tempMoves = posMoves[from];
                while(tempMoves != 0){
                    int to = ai.game.getLS1Bint(tempMoves);
                    if(historyTable[ai.game.getLS1Bint(tempPieces)][to] > mostPromisingValue) {
                        mostPromisingMove.from = ai.game.BIN_SQUARE[from];
                        mostPromisingMove.to = ai.game.BIN_SQUARE[to];
                        mostPromisingValue = historyTable[from][to];
                    }
                    tempMoves &= tempMoves - 1;
                }
                tempPieces &= tempPieces - 1;
            }
            if(mostPromisingMove.from == 0 && mostPromisingMove.to == 0) break;

            // move (if possible) and evaluate
            if(state.move(mostPromisingMove.from,  mostPromisingMove.to)){

                int value = -quiescenceSearch(-beta, -alpha, depth-1);
                state.unMove();

                if(value > score){
                    score = value;
                    if(value >= beta) return score;
                    if(value > alpha) score = alpha;
                }
            }
            // remove this move
            int from = ai.game.getLS1Bint(mostPromisingMove.from);
            posMoves[from] &= ~mostPromisingMove.to;
            if(posMoves[from] == 0) pieces &= ~mostPromisingMove.from;
        }
        return score;
    }
    public boolean isQuiet(State state, int depth){
        if(depth % 2 != 0)
            return false;
        if(ai.game.bitCount(state.getLastPos(0)) != ai.game.bitCount(state.getPieces()[0]))
            return false;
        return true;
    }
    public boolean isValid(HashState hashState, long hash){
        if(hashState == null || hashState.hashKey != hash)
            return false;
        return true;
    }
    private void initKillers(){
        killerMove1 = new Move[100];
        killerMove2 = new KillerMove[100];
        for (int i = 0; i < killerMove1.length; i++) {
            killerMove1[i] = new Move(0, 0);
            killerMove2[i] = new KillerMove(Integer.MIN_VALUE, 0, 0);
        }
    }
    private void moveUpKillers(){
        Move[] killers1Temp = new Move[killerMove1.length];
        KillerMove[] killers2Temp = new KillerMove[killerMove2.length];
        for (int i = 0; i < killerMove1.length - 1; i++) {
            killers1Temp[i] = killerMove1[i+1];
            killers2Temp[i] = killerMove2[i+1];
        }
        killers1Temp[killerMove1.length - 1] = new Move(0,0);
        killers2Temp[killerMove2.length - 1] = new KillerMove(Integer.MIN_VALUE,0,0);
        killerMove1 = killers1Temp;
        killerMove2 = killers2Temp;
    }

    private void storeTTEntry(long hashKey, int value, int type, long from, long to, int depth, int branchSize){
        int index = (int)(hashKey & (tTableSize-1));
        if(transpositionTable1[index] == null)
            transpositionTable1[index] = new HashState(hashKey, value, type, from, to, depth, branchSize);
        else if(!isValid(transpositionTable1[index], hashKey) &&
                 branchSize > transpositionTable1[index].branchSize) {
            transpositionTable2[index] = transpositionTable1[index];
            transpositionTable1[index] = new HashState(hashKey, value, type, from, to, depth, branchSize);
        }
        else transpositionTable2[index] = new HashState(hashKey, value, type, from, to, depth, branchSize);
    }
    public int getDepth(){
        return depth;
    }
    public void setDepth(int depth){
        this.depth = depth;
    }
    public int getNodesVisited() {
        return nodesVisited;
    }
    public EvaluationFunction getEvaluationFunction() {
        return evalF;
    }
    public void setEvaluationFunction(EvaluationFunction evalF) {
        this.evalF = evalF;
    }
    public HashState[] getTranspositionTable1() {
        return transpositionTable1;
    }
    public HashState[] getTranspositionTable2() {
        return transpositionTable2;
    }
    public void stopThinking() {
        this.stopThinking = true;
    }
    public int getBestScore(){
        return bestScore;
    }

    public Move getBestMoveDepth(State state, int maxDepth){
        this.state = state;
        transpositionTable1 = new HashState[tTableSize];
        transpositionTable2 = new HashState[tTableSize];
        depth = 0;
        bestMove = new Move(0,0);
        start = System.currentTimeMillis();
        searchTime = Integer.MAX_VALUE;
        stopThinking = false;
        int alpha = Integer.MIN_VALUE + 1, beta = Integer.MAX_VALUE - 1, delta = 50;
        int scoreBefore = evalF.evaluate(state);

        while(depth < maxDepth) {

            long start2 = System.currentTimeMillis();
            depth+=2;
            bestScore = alphaBeta(alpha, beta, depth, false);

            ai.mingmang.output("Depth " + depth + "(+" + EXTENDED_DEPTH + "): " + (System.currentTimeMillis() - start2)
                    + "ms | score = " + bestScore + " | move: " +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.from)) + "-" +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.to)));

        }

        ai.mingmang.output("Total search: " + (System.currentTimeMillis() - start) + "ms | "
                    + nodesVisited + " + " + quiNodesVisited + " nodes visited | score: " + bestScore + " | move: " +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.from)) + "-" +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.to)));

        if(ai.game.bitCount(state.calcTerritories()[state.getMovingPlayer()]) > ((ai.game.BOARD_SIZE*ai.game.BOARD_SIZE) / 2)
                    && bestScore <= scoreBefore){
            return ai.game.pass(state.getMovingPlayer());
        }
        return bestMove;
    }

    public void thinkDuringHumansTurn(State state, Move guessMove){
        this.state = state;
        long start2 = System.currentTimeMillis();
        int scoreBefore = evalF.evaluate(state);

        ai.mingmang.output("Thinking during humans turn");
        ai.mingmang.output("Opponents score: " + scoreBefore);

        if(guessMove == null){

            guessMove = new Move(0,0);
            HashState hash = null;

            int hashIndex = (int)(state.getHashKey() & (tTableSize-1));
            if (isValid(transpositionTable1[hashIndex], state.getHashKey()))
                hash = transpositionTable1[hashIndex];
            else if(isValid(transpositionTable2[hashIndex], state.getHashKey()))
                hash = transpositionTable2[hashIndex];

            if(hash != null && hash.depth >= 4){
                guessMove.from = hash.from;
                guessMove.to = hash.to;
            }
            else {
                ai.mingmang.output("\nQuick search for guess move:");
                guessMove = getBestMove(state, 500);
            }
        }

        state.move(guessMove.from, guessMove.to);

        ai.mingmang.output("\nGuess: " +
                           ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(guessMove.from)) + " - " +
                           ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(guessMove.to)));

        transpositionTable1 = new HashState[tTableSize];
        transpositionTable2 = new HashState[tTableSize];
        nodesVisited = 0;
        start = -1;
        depth = 0;
        int value = bestScore;
        stopThinking = false;

        while(ai.mingmang.getCurrentState().getMovingPlayer() != ai.getPlayerColor()) {

            depth+= 2;

            bestScore = value;
            if(depth >= 30) continue;
            value = alphaBeta(Integer.MIN_VALUE+1, Integer.MAX_VALUE-1, depth, false);

            ai.mingmang.output("Depth " + depth + "(+" + EXTENDED_DEPTH + "): score = " + value + " | move: " +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.from)) + "-" +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.to)));

        }

        ai.mingmang.output("Total search: score: " + bestScore + " | move: " +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.from)) + "-" +
                    ai.mingmang.getOutput2d().notationConvert(ai.game.getLS1Bint(bestMove.to)));
 
        depth -= 2;

        moveUpKillers();
        oppTime = (System.currentTimeMillis() - start2);
        this.guessMove = guessMove;
    }
    public void dontThinkDuringOpponentsTurn(){
        moveUpKillers();
    }
}
