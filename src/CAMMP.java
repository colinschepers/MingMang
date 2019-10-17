package mingmang;

import java.util.ArrayList;

/* @author Colin
 */
public class CAMMP implements AI {

    public MingMang mingmang;
    public Game game;
    private Search search;
    private OpeningState openingTable;

    private int playerColor;
    private long totalTime;
    private long timeLeft[];
    private boolean openingBook = false;
    private boolean change; // used for testing purposes

    public CAMMP(MingMang mingmang){
        this.mingmang = mingmang;
        game = mingmang.getGame();
        search = new AlphaBeta(this);
        if(openingBook){
            Object object = new Tools().load("OpeningBook");
            if(object != null && object instanceof OpeningState)
                openingTable = (OpeningState)object;
        }
        else openingTable = null;
    }
    public CAMMP(MingMang mingmang, int playerColor, int totalTime, boolean change){
        this.change = change;
        this.mingmang = mingmang;
        game = mingmang.getGame();
        search = new AlphaBeta(this);
        if(openingBook){
            Object object = new Tools().load("OpeningBook");
            if(object != null && object instanceof OpeningState)
                openingTable = (OpeningState)object;
        }
        this.playerColor = playerColor;
        this.totalTime = totalTime;
        this.timeLeft = new long[]{totalTime, totalTime}; 
    }

    public void resetAI(){
        timeLeft = new long[]{totalTime, totalTime};
        ((AlphaBeta)search).reset();
    }

    public Move getBestMoveDepth(State state, int depth){
        return ((AlphaBeta)search).getBestMoveDepth(state, depth);
    }
    public Move getBestMoveTime(State state, int time){
        return ((AlphaBeta)search).getBestMove(state, time);
    }
    
    public void doMove(){

        long start = System.currentTimeMillis();
        final int SAFETY_THRESHOLD = 30000;
        final double THINK_PERCENTAGE = 0.05;  // 1 over 20
        int sTime = (int)((timeLeft[playerColor] - SAFETY_THRESHOLD) * THINK_PERCENTAGE);
        if(sTime < 100) sTime = 100;
        mingmang.output("\nTime left: " + timeLeft[playerColor]);

        Move bestMove = getOpeningMove();
        if(bestMove == null) bestMove = search.getBestMove(mingmang.getCurrentState().clone(), sTime);

        if(!mingmang.isPause()) {

            if(bestMove.from == bestMove.to){
                mingmang.getCurrentState().realPass();
            }
            else if(!mingmang.getCurrentState().realMove(bestMove.from, bestMove.to)){
                mingmang.getCurrentState().makeRandomMove();
            }

        }
        timeLeft[playerColor] -=  (int)(System.currentTimeMillis() - start);
    }

    public Move getOpeningMove(){

        if(!openingBook) return null;

        ArrayList<Move> moveHistory = mingmang.getMoveHistory();
        OpeningState state = openingTable;

        if(mingmang.getCurrentState().getStateNumber() != moveHistory.size())
            return null;

        moveH: for (int i = 0; i < moveHistory.size(); i++) {
            for (int j = 0; j < state.movesToChildren.size(); j++) {
                if(state.movesToChildren.get(j).from == moveHistory.get(i).from &&
                   state.movesToChildren.get(j).to == moveHistory.get(i).to){
                   state = state.children.get(j);
                   continue moveH;
                }
            }
            return null;
        }

        double bestValue = 0.4;
        Move bestMove = null;
        for (int i = 0; i < state.movesToChildren.size(); i++) {
            if((double)state.moveWon.get(i) / (double)state.movePlayed.get(i) > bestValue){
                bestValue = (double)state.moveWon.get(i) / (double)state.movePlayed.get(i);
                bestMove = state.movesToChildren.get(i);
            }
        }

        if(bestMove != null)
            mingmang.output("Opening move: " +
                mingmang.getOutput2d().notationConvert(game.getLS1Bint(bestMove.from)) + "-" +
                mingmang.getOutput2d().notationConvert(game.getLS1Bint(bestMove.to)) + ": " + bestValue);

        return bestMove;
    }
    public void thinkDuringOpponentsTime(){
        mingmang.output("Opponent's time left: " + timeLeft[~playerColor&1]);
        long start = System.currentTimeMillis();
        Move guess = getOpeningMove();
        search.thinkDuringHumansTurn(mingmang.getCurrentState().clone(), guess);
        timeLeft[~playerColor&1] -= (int)(System.currentTimeMillis() - start); 
    }
    public void dontThinkDuringOpponentsTime(){
        search.dontThinkDuringOpponentsTurn();
    }
    public void stopThinking(){
        search.stopThinking();
    }
    public boolean getChange(){
        return change;
    }
    public int getPlayerColor(){
        return playerColor;
    }
    public int getOpponentColor(){
        if(playerColor == game.BLACK)
            return game.WHITE;
        return game.BLACK;
    }
    public void setSearch(Search search) {
        this.search = search;
    }
    public Search getSearch() {
        return search;
    }
    public void setPlayerColor(int playerColor){
       this.playerColor = playerColor;
    }
}