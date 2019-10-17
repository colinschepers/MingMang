package mingmang; 

/** @author Colin
 */
public class State implements java.io.Serializable {

    private MingMang mingmang;
    private Game game;
    private int movingPlayer;
    private int boardSize;

    public int stateNumber;
    
    private long hashKey;
    private long[] pieces;

    private long[] hashHistory;
    public long[][] posHistory;
    
    public State(int movingPlayer, MingMang mingmang){
        this.movingPlayer = movingPlayer;
        this.mingmang = mingmang;
        game = mingmang.getGame();
        boardSize = game.BOARD_SIZE;
        pieces = new long[2];
    }

    public void initialSetup(){
        boardSize = game.BOARD_SIZE;
        movingPlayer = game.BLACK;
        pieces[game.BLACK] = (game.FILE[boardSize-1] | game.RANK[0]) ^ (game.FILE[boardSize-1] & game.RANK[boardSize-1]);
        pieces[game.WHITE] = (game.FILE[0] | game.RANK[boardSize-1]) ^ (game.FILE[0] & game.RANK[0]);
        hashKey = calcHashKey();
        initHistory();
        stateNumber = 0;
    }
    public boolean realMove(long from, long to){  
        if(move(from, to)) { 
            mingmang.addToMoveHistory(new Move(from, to));
            return true;
        }
        return false;
    }
    public boolean move(long from, long to){

        int fromI = game.getLS1Bint(from);
        int toI = game.getLS1Bint(to);
        long backupPiecesB = pieces[game.BLACK];
        long backupPiecesW = pieces[game.WHITE];
        long capture = 0, toOwnPieces = getPossibleMoves(toI, pieces[movingPlayer]);

        // if my piece not on from-square or to-square not reachable from from-square return false
        if((pieces[movingPlayer] & from) == 0 || ((getPossibleMoves(fromI) & to) == 0))
            return false;

        // move
        pieces[movingPlayer] ^= (from | to);

        // determine capture area
        long dir = toOwnPieces & game.NORTH[toI];
        if((dir & pieces[~movingPlayer&1]) == dir && toI/boardSize + game.bitCount(dir) < boardSize-1)
            capture |= dir;
        dir = toOwnPieces & game.SOUTH[toI];
        if((dir & pieces[~movingPlayer&1]) == dir && toI/boardSize - game.bitCount(dir) > 0)
            capture |= dir;
        dir = toOwnPieces & game.WEST[toI];
        if((dir & pieces[~movingPlayer&1]) == dir && toI%boardSize + game.bitCount(dir) < boardSize-1)
            capture |= dir;
        dir = toOwnPieces & game.EAST[toI];
        if((dir & pieces[~movingPlayer&1]) == dir && toI%boardSize - game.bitCount(dir) > 0)
            capture |= dir;

        // make captures
        pieces[movingPlayer] ^= capture;
        pieces[~movingPlayer&1] ^= capture;

        // check history
        for (int i = stateNumber - 1; i >= 0; i-=2) {
            if(pieces[game.BLACK] == posHistory[game.BLACK][i] && pieces[game.WHITE] == posHistory[game.WHITE][i]){
                pieces[game.BLACK] = backupPiecesB;
                pieces[game.WHITE] = backupPiecesW;
                return false;
            }
        }

        addToHistory(backupPiecesB, backupPiecesW, hashKey);

        // XOR Zobrist hash randoms; remove from, add to, change player
        hashKey ^= game.ZOBRIST_RANDOM[movingPlayer*64 + fromI] ^ 
                   game.ZOBRIST_RANDOM[movingPlayer*64 + toI] ^
                   game.ZOBRIST_RANDOM[2*64];

        // XOR Zobrist hash randoms; captures
        int square;
        while(capture != 0){
            square = game.getLS1Bint(capture);
            hashKey ^= game.ZOBRIST_RANDOM_CAPTURE[square];
            capture &= ~game.BIN_SQUARE[square];
        }

        // change player color
        movingPlayer = ~movingPlayer&1;
        
        return true;
    }
     
    public long calcHashKey(){
        long key = 0, loop;
        int square;
        for (int i = 0; i < 2; i++) { // 2 players
            loop = pieces[i];
            while(loop != 0){
                square = game.getLS1Bint(loop);
                loop &= ~(1L << square);
                key ^= game.ZOBRIST_RANDOM[i*64 + square];
            }
        }
        if(movingPlayer == game.WHITE)
            key ^= game.ZOBRIST_RANDOM[2*64];
        return key;
    }

    public void unMove(){
        stateNumber--; 
        pieces[game.BLACK] = posHistory[game.BLACK][stateNumber];
        pieces[game.WHITE] = posHistory[game.WHITE][stateNumber];
        hashKey = hashHistory[stateNumber];
        removeLastFromHistory();
        movingPlayer = ~movingPlayer&1;
    }
    public void realUnmove(){
        unMove();
        mingmang.removeLastMoveFromHistory();
    }
    public void pass(){
        addToHistory(pieces[game.BLACK], pieces[game.WHITE], hashKey);
        hashKey ^= game.ZOBRIST_RANDOM[2*64];
        movingPlayer = ~movingPlayer&1;
    }
    public void realPass(){ 
        mingmang.addToMoveHistory(game.pass(movingPlayer));
        pass();
    }
    public long getPossibleMoves(int square){
        int index = (int)(((game.MOVE_MASK[square] & getBoard()) * game.MOVE_MAGIC_NUMBER[square]) >>> game.MOVE_MAGIC_SHIFT[square]);
        return game.MOVE_RESULT[square][index] & ~getBoard();
    }
    public long getPossibleMoves(int square, long board){
        int index = (int)(((game.MOVE_MASK[square] & board) * game.MOVE_MAGIC_NUMBER[square]) >>> game.MOVE_MAGIC_SHIFT[square]);
        return game.MOVE_RESULT[square][index] & ~board;
    }
    public long getPossibleMovesInclusive(int square ){
        int index = (int)(((game.MOVE_MASK[square] & getBoard()) * game.MOVE_MAGIC_NUMBER[square]) >>> game.MOVE_MAGIC_SHIFT[square]);
        return game.MOVE_RESULT[square][index];
    }
    public long getPossibleMovesInclusive(int square, long board){
        int index = (int)(((game.MOVE_MASK[square] & board) * game.MOVE_MAGIC_NUMBER[square]) >>> game.MOVE_MAGIC_SHIFT[square]);
        return game.MOVE_RESULT[square][index];
    }
    public int[] getAllSquares(long bb){
        int nrOfSquares = game.bitCount(bb);
        int[] squares = new int[nrOfSquares];
        int count = 0;
        for (int i = 0; i < boardSize*boardSize; i++) {
            if((bb & 1) == 1) {
                squares[count] = i;
                count++;
            }
            bb = bb >>> 1;
        }
        return squares;
    }
    public void makeRandomMove(){
        Move move = getRandomMove();
        this.realMove(move.from, move.to);
    }
    public Move getRandomMove(){
        int[] ownSquares = game.getBitsSet(pieces[movingPlayer]);                                   // get array of squares occupied with friendly pieces
        int randomPiece = ownSquares[game.rand.nextInt(ownSquares.length)];                         // pick a piece at random of these squares
        long moves = mingmang.getCurrentState().getPossibleMoves(randomPiece);                      // get move bitboard of that piece
        int[] destinationSquares = game.getBitsSet(moves);                                          // get array of squares of possible destinations
        if(destinationSquares.length == 0) return getRandomMove();                                  // if no destinations then this piece cannot move
        int randomDestination = destinationSquares[game.rand.nextInt(destinationSquares.length)];   // pick a random square location
        return new Move((1L << randomPiece), (1L << randomDestination));                            // make move
    }
    public boolean hasAMove(int playerColor){
        long temp = pieces[playerColor];
        while(temp != 0) {
            if((game.NEIGHBORS[game.getLS1Bint(temp)] & ~getBoard()) != 0)
                return true;
            temp &= temp - 1;
        }
        return false;
    }
    public boolean myPiece(int playerColor, long square){
        return ((pieces[playerColor] & square) != 0);
    }
    public long[] calcTerritories(){
        // calculate the "official" territories' bitboards
        long[] reachable = new long[]{pieces[game.BLACK], pieces[game.WHITE]};
        long posMoves = 0, loop = 0, tempB = 0,tempW = 0, bb = 0;
        int square = 0;
        while(true) {
            loop = reachable[game.BLACK] | reachable[game.WHITE] | pieces[game.BLACK] | pieces[game.WHITE];
            tempB = reachable[game.BLACK];
            tempW = reachable[game.WHITE];
            while(loop != 0){
                square = game.getLS1Bint(loop);
                posMoves = getPossibleMoves(square);
                loop &= ~(game.BIN_SQUARE[square]);
                if(((pieces[game.BLACK] | reachable[game.BLACK]) & game.BIN_SQUARE[square]) != 0)
                    reachable[game.BLACK] |= game.BIN_SQUARE[square] | (posMoves);
                if(((pieces[game.WHITE] | reachable[game.WHITE]) & game.BIN_SQUARE[square]) != 0)
                    reachable[game.WHITE] |= game.BIN_SQUARE[square] | (posMoves);
            }
            if(tempB == reachable[game.BLACK] && tempW == reachable[game.WHITE])
                break;
        }
        return new long[]{ reachable[game.BLACK] & ~reachable[game.WHITE],
                           reachable[game.WHITE] & ~reachable[game.BLACK] };
    }
    public int getTerrVal(int playerColor){
        // return the number of "heuristic" (one-step) territories a player has more than the opponent
        long reachablePlayer = 0;
        long reachableOpponent = 0;
        long temp = pieces[playerColor];
        while(temp != 0) {
            reachablePlayer |= getPossibleMoves(game.getLS1Bint(temp));
            temp &= temp - 1;
        }
        temp = pieces[~playerColor&1];
        while(temp != 0) {
            reachableOpponent |= getPossibleMoves(game.getLS1Bint(temp));
            temp &= temp - 1;
        }
        return (game.bitCount(reachablePlayer) - game.bitCount(reachableOpponent));
    } 
    public int getTerritoryValue(int playerColor){
        // returns how much more "official" territories a player has
        long[] terr = calcTerritories();
        return game.bitCount(terr[playerColor]) - game.bitCount(terr[~playerColor&1]);
    }
    public int getMovingPlayer(){
        return movingPlayer;
    }
    public void setBlackToMove(){
        movingPlayer = game.BLACK;
    }
    public void setWhiteToMove(){
        movingPlayer = game.WHITE;
    }
    public int oppositeColor(int playerColor){
        if(playerColor == game.BLACK)
            return game.WHITE;
        return game.BLACK;
    }
    public long getBoard() {
        return pieces[game.BLACK] | pieces[game.WHITE];
    }
    public long[] getPieces() {
        return pieces;
    }
    public void setPieces(int color, long bb){
        pieces[color] = bb;
    }
    public int getStateNumber(){
        return stateNumber;
    }
    public long getHashKey() {
        return hashKey;
    }
    public void setHashKey(long hashKey) {
        this.hashKey = hashKey;
    }
    public void addToHistory(long piecesB, long piecesW, long hash){
        if(stateNumber >= posHistory[0].length)
            increaseHistorySize();
        posHistory[game.BLACK][stateNumber] = piecesB;
        posHistory[game.WHITE][stateNumber] = piecesW;
        hashHistory[stateNumber] = hash;
        stateNumber++;
    }
    public void removeLastFromHistory(){
        posHistory[game.BLACK][stateNumber] = 0;
        posHistory[game.WHITE][stateNumber] = 0;
        hashHistory[stateNumber] = 0;
    }
    public long getLastPos(int playerColor){
        if(stateNumber >= 1) return posHistory[playerColor][stateNumber-1];
        else return pieces[playerColor];
    }
    public long getPrevToLastPos(int playerColor){
        if(stateNumber >= 2) return posHistory[playerColor][stateNumber-2];
        else return pieces[playerColor];
    }
    public long getLastHash(){
        if(stateNumber > 0)
            return hashHistory[stateNumber-1];
        else return 0;
    }
    public void setHistory(int stateNumber, long[][] posHistory, long[] hashHistory){
        this.stateNumber = stateNumber;
        this.posHistory = posHistory;
        this.hashHistory = hashHistory;
    }
    public long[][] getPosHistory() {
        return posHistory;
    }
    public long[] getHashHistory() {
        return hashHistory;
    }
    public void clearHistory(){
        posHistory = new long[2][200];
        hashHistory = new long[200];
    }
    public void increaseHistorySize(){
        long[][] temp = new long[2][posHistory[0].length * 2];
        for (int i = 0; i < posHistory.length; i++) {
            for (int j = 0; j < posHistory[i].length; j++) {
                temp[i][j] = posHistory[i][j];
            }
        }
        posHistory = temp;
        long[] temp2 = new long[hashHistory.length*2];
        for (int i = 0; i < hashHistory.length; i++) {
            temp2[i] = hashHistory[i];
        }
        hashHistory = temp2;
    }
    public boolean passedLastTurn(){ 
        if(stateNumber >= 1 && getLastPos(game.BLACK) == pieces[game.BLACK] && getLastPos(game.WHITE) == pieces[game.WHITE])
            return true;
        return false;
    }
    public boolean passedPrevToLastTurn(){
        if(stateNumber >= 2 && getPrevToLastPos(game.BLACK) == pieces[game.BLACK] && getPrevToLastPos(game.WHITE) == pieces[game.WHITE])
            return true;
        return false;
    }
    public void initHistory(){
        posHistory = new long[2][500];
        hashHistory = new long[500];
    }
    public boolean isTerminal(){
        if(lastTwoMovesWerePasses() || !hasAMove(movingPlayer)) return true;
        return false;
    }
    public boolean lastTwoMovesWerePasses(){
        if(posHistory != null && stateNumber >= 2 && passedLastTurn() && passedPrevToLastTurn())
                return true;
        return false;
    }
    public void saveState(){
        Object[] info = new Object[]{movingPlayer, boardSize, stateNumber, hashKey, pieces, hashHistory, posHistory};
        new Tools().save(info, "LastPosition");
    }
    public void loadState(){
        Object object = new Tools().load("LastPosition");
        if(object != null && object instanceof Object[]){
            movingPlayer = ((Integer)((Object[])object)[0]);
            boardSize = ((Integer)((Object[])object)[1]);
            stateNumber = ((Integer)((Object[])object)[2]);
            hashKey = ((Long)((Object[])object)[3]);
            pieces = ((long[])((Object[])object)[4]);
            hashHistory = ((long[])((Object[])object)[5]);
            posHistory = ((long[][])((Object[])object)[6]);
        }
    }
    public State clone(){
        State out = new State(movingPlayer, mingmang);
        out.setHashKey(hashKey);
        out.setPieces(game.BLACK, pieces[game.BLACK]);
        out.setPieces(game.WHITE, pieces[game.WHITE]);
        out.setHistory(stateNumber, (long[][])posHistory.clone(), (long[])hashHistory.clone());
        return out;
    }
    public void printBoard(){
        long blackPieces = pieces[game.BLACK];
        long whitePieces = pieces[game.WHITE];
        System.out.print("------------------------");
        for (int i = 0; i < boardSize*boardSize; i++) {
            if(i%boardSize == 0) System.out.println("");
            if(Long.numberOfLeadingZeros(blackPieces) == 0)
                System.out.print(" B ");
            else if(Long.numberOfLeadingZeros(whitePieces) == 0)
                System.out.print(" W ");
            else System.out.print(" . ");
            blackPieces = blackPieces << 1;
            whitePieces = whitePieces << 1;
        }
        System.out.println("\n------------------------");
    }
}

// Currently unused methods; most used for evaluation of mobility values
//
//public int[] getTerritoriesAndMobilityValues(int playerColor){
//    long[] reachable = new long[]{pieces[playerColor], pieces[~playerColor&1]};
//    long posMoves = 0, loop = 0, tempPlayer = 0,tempOpponent = 0;
//    int square = 0, playerMob = 0, opponentMob = 0;
//    boolean firstLoop = true;
//    while(true) {
//        loop = reachable[playerColor] | reachable[~playerColor&1] | pieces[playerColor] | pieces[~playerColor&1];
//        tempPlayer = reachable[playerColor];
//        tempOpponent = reachable[~playerColor&1];
//        while(loop != 0){
//            square = game.getLS1Bint(loop);
//            posMoves = getPossibleMoves(square);
//            if(((pieces[playerColor] | reachable[playerColor]) & game.BIN_SQUARE[square]) != 0){
//                reachable[playerColor] |= posMoves;
//                if(firstLoop) playerMob += game.bitCount(posMoves);
//            }
//            if(((pieces[~playerColor&1] | reachable[~playerColor&1]) & game.BIN_SQUARE[square]) != 0){
//                reachable[~playerColor&1] |= posMoves;
//                if(firstLoop) opponentMob += game.bitCount(posMoves);
//            }
//            loop &= ~(game.BIN_SQUARE[square]);
//        }
//        firstLoop = false;
//        if(tempPlayer == reachable[playerColor] && tempOpponent == reachable[~playerColor&1])
//            break;
//    }
//    return new int[]{ game.bitCount(reachable[playerColor] & ~reachable[playerColor]) -
//                       game.bitCount(reachable[~playerColor&1] & ~reachable[~playerColor&1]),
//                       playerMob - opponentMob};
//}
//public int[] getTerritories1StepAndMobilityValues(int playerColor){
//    long reachablePlayer = 0;
//    long reachableOpponent = 0;
//    int playerMob = 0, opponentMob = 0;
//    long posMoves;
//    long temp = pieces[playerColor];
//    while(temp != 0) {
//        posMoves = getPossibleMoves(game.getLS1Bint(temp));
//        reachablePlayer |= posMoves;
//        playerMob += game.bitCount(posMoves);
//        temp &= temp - 1;
//    }
//    temp = pieces[~playerColor&1];
//    while(temp != 0) {
//        posMoves = getPossibleMoves(game.getLS1Bint(temp));
//        reachableOpponent |= posMoves;
//        opponentMob += game.bitCount(posMoves);
//        temp &= temp - 1;
//    }
//    return new int[]{(game.bitCount(reachablePlayer) - game.bitCount(reachableOpponent)),
//                      playerMob - opponentMob};
//}
//public int getMovesValue(int playerColor){
//    int value = 0;
//    long temp = pieces[playerColor];
//    while(temp != 0) {
//        value += game.bitCount(getPossibleMoves(game.getLS1Bint(temp)));
//        temp &= temp - 1;
//    }
//    return value;
//}
//public int calcNrMoves(int playerColor){
//    int moves = 0, square;
//    long temp = pieces[playerColor];
//    while(temp != 0) {
//        square = game.getLS1Bint(temp);
//        moves += game.bitCount(getPossibleMoves(square));
//        temp &= ~game.BIN_SQUARE[square];
//    }
//    return moves;
//}
//public int getMobVal(int playerColor){
//    int playerMob = 0, opponentMob = 0;
//    long temp = pieces[playerColor];
//    while(temp != 0) {
//        playerMob += game.bitCount(getPossibleMoves(game.getLS1Bint(temp)));
//        temp &= temp - 1;
//    }
//    temp = pieces[~playerColor&1];
//    while(temp != 0) {
//        opponentMob += game.bitCount(getPossibleMoves(game.getLS1Bint(temp)));
//        temp &= temp - 1;
//    }
//    return (playerMob - opponentMob);
//}