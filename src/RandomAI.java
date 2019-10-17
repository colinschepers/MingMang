package mingmang;

/* @author Colin
 */
public class RandomAI implements AI {

    private MingMang mingmang;
    private Game game;

    private int playerColor;
    private boolean thinking;

    private final double PASS_RATE = 0.01;

    public RandomAI(int playerColor, MingMang mingmang){
        this.playerColor = playerColor;
        this.mingmang = mingmang;
        this.game = mingmang.getGame();
    }
    public void doMove(){
        thinking = true;
        while(thinking){
            if(game.rand.nextDouble() < PASS_RATE) mingmang.getCurrentState().realPass();                // pass with a small probability
            int[] ownSquares = mingmang.getGame().getBitsSet(
                    mingmang.getCurrentState().getPieces()[playerColor]);               // get array of square occupied with friendly pieces
            int randomPiece = ownSquares[game.rand.nextInt(ownSquares.length)];              // pick a piece at random
            long moves = mingmang.getCurrentState().getPossibleMoves(randomPiece);      // get move bitboard of that piece
            int[] destinationSquares = mingmang.getGame().getBitsSet(moves);  // get array of squares of possoble destinations
            if(destinationSquares.length == 0) continue;                                // if no destinations then this piece cannot move -> pass
            int randomDestination = destinationSquares[game.rand.nextInt(destinationSquares.length)]; // pick a random square location
            mingmang.getCurrentState().realMove((1L << randomPiece), (1L << randomDestination));                   // move
            thinking = false;
        }
    }

    public void thinkDuringOpponentsTime(){
        thinking = true;
        while(mingmang.getCurrentState().getMovingPlayer() != playerColor && thinking){
            try {
                Thread.sleep(200);
            } catch (Exception e){}
        }
        thinking = false;
    }
    public void dontThinkDuringOpponentsTime(){

    }
    public void stopThinking(){
        thinking = false;
    }
    public int getPlayerColor(){
        return playerColor;
    }

    public void setTraining(boolean training) {

    }
    public void resetAI(){

    }
    public boolean getChange(){
        return false;
    }

    public void doMove(int time) {
        doMove();
    }

    public Move getBestMoveTime(State state, int time) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Move getBestMoveDepth(State state, int depth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
