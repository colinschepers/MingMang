package mingmang;
 
import java.util.ArrayList;
import java.io.*;

/** @author Colin
 */
public class MingMang  {
 
    private Output2D out2d;
    private Tools tools;

    private Game game;
    private State currentState;
    private Player blackPlayer;
    private Player whitePlayer;
    private boolean gameOver;
    private ArrayList<Move> moveHistory;

    private boolean pause;
    private boolean paused;

    private final int AI_DELAY = 0;
    private final int ONE_SEC = 1000;
    private final int TEN_SEC = 10000;
    private final int ONE_MIN = 60000;
    private final int FIVE_MIN = 300000;
    private final int FIFTEEN_MIN = 900000;
    
    private int black = 0;
    private int white = 0;
    private int draw = 0;

    public MingMang(){

        tools = new Tools();
        game = new Game();
        moveHistory = new ArrayList<Move>();

        // manually set players
        setBlackPlayer(new Human(game.BLACK));
//        setBlackPlayer(new RandomAI(game.BLACK, this));
//        setBlackPlayer(new CAMMP(this, game.BLACK, FIFTEEN_MIN, false));
//
//        setWhitePlayer(new Human(game.WHITE));
//        setWhitePlayer(new RandomAI(game.WHITE, this));
        setWhitePlayer(new CAMMP(this, game.WHITE, FIFTEEN_MIN, false));

        newGame();

        // manually load state using bitboard values
//        currentState.setPieces(0,       848857471803785L);
//        currentState.setPieces(1,  -6123619713716027392L);
//        currentState.setWhiteToMove();

        out2d = new Output2D(this);
    }

    public void gameLoop(){

        while(true){

            while(!gameOver){
                
                paint();

                if(getMovingPlayer() instanceof Human && getNonMovingPlayer() instanceof AI){
                    ((AI)getNonMovingPlayer()).thinkDuringOpponentsTime();
                }
                else if(getMovingPlayer() instanceof AI && getNonMovingPlayer() instanceof Human) {
                    ((AI)getMovingPlayer()).doMove();
                }
                else if(getMovingPlayer() instanceof Human && getNonMovingPlayer() instanceof Human) {
                    int mov = currentState.getMovingPlayer();
                    while(currentState.getMovingPlayer() == mov && !gameOver) {
                        try { Thread.sleep(100); } catch(Exception e) {e.printStackTrace();}
                    }
                }
                else if(getMovingPlayer() instanceof AI && getNonMovingPlayer() instanceof AI){
                    ((AI)getNonMovingPlayer()).dontThinkDuringOpponentsTime();
                    ((AI)getMovingPlayer()).doMove();
                }

                while(pause) {
                    paused = true;
                    try { Thread.sleep(100); } catch(Exception e) { }
                }
                paused = false;

                printLastMove();
                this.currentState.saveState();

                if(currentState.isTerminal()){
                    gameOver();
                }

                if(gameOver) {
                    endResult();
                    paint();
                }
            }
        }
    }
    public void output(String text){
        this.out2d.appendOutputText(text + "\n");
    }
    public void paint(){
        out2d.getPaintBoard().paintImmediately(0, 0, out2d.getFrame().getWidth(), out2d.getFrame().getHeight());
    }
    public final void newGame(){
        moveHistory.clear();
        currentState = new State(game.BLACK, this);
        currentState.initialSetup();
        if(this.getBlackPlayer() instanceof CAMMP)
            ((CAMMP)getBlackPlayer()).resetAI();
        if(this.getWhitePlayer() instanceof CAMMP)
            ((CAMMP)getWhitePlayer()).resetAI();
        gameOver = false;
        if(out2d != null && out2d.getOutputEnabled())
            out2d.removeOutputText();
    }
    private void delayAI(long time){
        // can be used when AI plays AI so that have time to observe the moves
        if(getMovingPlayer() instanceof AI) {
            long start = System.currentTimeMillis();
            while(System.currentTimeMillis() - start < time){
                try {
                    Thread.yield();
                } catch (Exception e) {}
            }
        }
    }
    public void savePosition(){
       tools.save(currentState, "LastPosition");
    }
    public Output2D getOutput2d(){
        return out2d;
    }
    public Tools getTools(){
        return tools;
    }
    public boolean isGameOver(){
        return gameOver;
    }
    public void gameOver(){
        gameOver = true;
    }
    public void gameNotOver(){
        gameOver = false;
    }
    public Game getGame(){
        return game;
    }
    public State getCurrentState(){
        return currentState;
    } 
    public void setCurrentState(State newCurrentState){
        currentState = newCurrentState;
    }
    public Player getMovingPlayer(){
        if(currentState.getMovingPlayer() == game.BLACK)
            return blackPlayer;
        return whitePlayer;
    }
    public Player getNonMovingPlayer(){
        if(currentState.getMovingPlayer() == game.BLACK)
            return whitePlayer;
        return blackPlayer;
    }
    public Player getBlackPlayer(){
        return blackPlayer;
    }
    public final void setBlackPlayer(Player player){
        blackPlayer = player;
    }
    public Player getWhitePlayer(){
        return whitePlayer;
    }
    public final void setWhitePlayer(Player player){
        whitePlayer = player;
    }
    public void printBB(long bb){
        for (int i = 0; i < game.BOARD_SIZE*game.BOARD_SIZE; i++) {
            if(i%game.BOARD_SIZE == 0) output("");
            if(Long.numberOfLeadingZeros(bb) == 0)
                System.out.print("1");
            else System.out.print("0");
            bb = bb << 1;
        }
        output("");
    }
    public void addToMoveHistory(Move move){
        moveHistory.add(move);
    }
    public void removeLastMoveFromHistory(){
        moveHistory.remove(moveHistory.size()-1);
    }
    public Move getLastMove(){
        if(moveHistory != null && !moveHistory.isEmpty())
            return moveHistory.get(moveHistory.size()-1);
        else return null;
    }
    public ArrayList<Move> getMoveHistory(){
        return moveHistory;
    }
    public void setMoveHistory(ArrayList<Move> moveHistory){
        this.moveHistory = moveHistory;
    }
    public void clearMoveHistory(){
        moveHistory.clear();
    }
    public boolean inHistory(Move move){
        for (int i = 0; i < moveHistory.size(); i++) {
            if((move.from == moveHistory.get(i).from) && (move.to == moveHistory.get(i).to))
                return true;
        }
        return false;
    }
    public boolean isPause() {
        return pause;
    }
    public void setPause(boolean pause) {
        this.pause = pause;
    }
    public boolean isPaused() {
        return paused;
    }
    public void printLastMove(){
        if(getLastMove() == null || game.BOARD_SIZE != 8) return;
        String move = "----------------------------------------------------------------------------- "+ (((getMoveHistory().size()+1)/2)) + ". ";
        if(getCurrentState().getMovingPlayer() == game.BLACK)
            move += " ...  ";
        if(getLastMove().from == -1 || getLastMove().from == -2)
             move += " O-O";
        else move += out2d.notationConvert(Long.numberOfTrailingZeros(getLastMove().from)) + "-"
                       + out2d.notationConvert(Long.numberOfTrailingZeros(getLastMove().to)) + " ";
        output(move);
        System.out.println("------------------ State: " + currentState.getPieces()[0] + " " + currentState.getPieces()[1]);
    }
    public void endResult(){
        output("\nGame Over: ");
        long[] terr = currentState.calcTerritories();
        int territoriesBlack = Long.bitCount(terr[game.BLACK]);
        int territoriesWhite = Long.bitCount(terr[game.WHITE]);
        if(territoriesBlack > territoriesWhite){
            output("Black wins: ");
            black++;
        }
        else if(territoriesBlack < territoriesWhite){
            output("White wins: ");
            white++;
        }
        else {
            output("Draw: ");
            draw++;
        }
        output("Territories: "+ territoriesBlack + " - " + territoriesWhite);
    }
}


//    Used for approximating the evaluation function weights
//
//    private int winTD = 0;
//    private int winNonTD = 0;
//    private int drawTD = 0;
//    private int blackWin = 0;
//    private int whiteWin = 0;
//    private int draw = 0;
//    int td;
//
//    public int endResult(){
//        int res = -1;
//        currentState.calcTerritories();
//        long[] terr = currentState.calcTerritories();
//        int territoriesBlack = Long.bitCount(terr[game.BLACK]);
//        int territoriesWhite = Long.bitCount(terr[game.WHITE]);
//        if(territoriesBlack > territoriesWhite){
//            res = game.BLACK;
//            if(td == game.BLACK) winTD++;
//            else winNonTD++;
//            blackWin++;
//        }
//        else if(territoriesBlack < territoriesWhite){
//            res = game.WHITE;
//            if(td == game.WHITE) winTD++;
//            else winNonTD++;
//            whiteWin++;
//        }
//        else {
//            drawTD++;
//            draw++;
//        }
//        return res;
//    }
//
//    private void TDLearning(){
//
//        output("TD - Non-TD: " + winTD + " - " + winNonTD);
//
//        blackPlayer.stopThinking();
//        whitePlayer.stopThinking();
//
//        ((CAMMP)blackPlayer).resetAI();
//        ((CAMMP)whitePlayer).resetAI();
//        Player temp = blackPlayer;
//        setBlackPlayer(whitePlayer);
//        setWhitePlayer(temp);
//        ((CAMMP)blackPlayer).setPlayerColor(game.BLACK);
//        ((CAMMP)whitePlayer).setPlayerColor(game.WHITE);
//        td = currentState.oppositeColor(td);
//
//        if(winTD >= 10) {
//            if(td == game.BLACK) {
//                int[] params1 = ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().getParams();
//                int[] params2 = ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().getParams();
//                int[] newParams = new int[params1.length];
//                for (int i = 0; i < newParams.length; i++) {
//                    if(params1[i] == 0) newParams[i] = 0;
//                    else newParams[i] = (int)(params2[i] + 0.5 * (params1[i] - params2[i]));
//                }
//
//                ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().setParams(newParams);
//                ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().setParams(newParams);
//
//                getTools().save(((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams(),"EFParams");
//
//                System.out.print("Params Saved: ");
//             ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().printParamsShort();
//
//            }
//            else if(td == game.WHITE) {
//                int[] params1 = ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().getParams();
//                int[] params2 = ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().getParams();
//                int[] newParams = new int[params1.length];
//                for (int i = 0; i < newParams.length; i++){
//                    if(params1[i] == 0) newParams[i] = 0;
//                    else newParams[i] = (int)(params2[i] + 0.5 * (params1[i] - params2[i]));
//                }
//
//                ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().setParams(newParams);
//                ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().setParams(newParams);
//
//                getTools().save(((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams(),"EFParams");
//
//                System.out.print("Params Saved: ");
//                ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().printParamsShort();
//
//            }
//
//
//            winTD = 0;
//            winNonTD = 0;
//            drawTD = 0;
//            td = 1;
//            changeParams(td);
//        }
//        else if(winNonTD > 5 || drawTD > 5) {
//            winTD = 0;
//            winNonTD = 0;
//            drawTD = 0;
//            if(td == game.BLACK)
//                ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().setParams(
//                        ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().getParams());
//            else if(td == game.WHITE)
//                ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().setParams(
//                        ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().getParams());
//            td = 1;
//            changeParams(td);
//        }
//
//        newGame();
//    }
//
//    private void changeParams(int td){
//        if(td == 0){
//            int[] params = ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().getParams();
//            for (int i = 0; i < params.length; i++) {
//
//                double change;
//                if(i == 0) change = game.rand.nextInt(50000);
//                else change = game.rand.nextInt(4);
//
//                if(game.rand.nextBoolean()) change *= -1;
//                params[i] += change;
//
//
//            }
//            ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().setParams(params);
//        }
//        else if(td == 1)  {
//            int[] params = ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().getParams();
//            for (int i = 0; i < params.length; i++) {
//
//                double change;
//                if(i == 0) change = game.rand.nextInt(50000);
//                else change = game.rand.nextInt(4);
//
//                if(game.rand.nextBoolean()) change *= -1;
//                params[i] += change;
//
//            }
//            ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().setParams(params);
//        }
//        System.out.print("\n\nNew Params: ");
//        if(td == 0) ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().printParamsShort();
//        else ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().printParamsShort();
//
//        System.out.print("Play Against: ");
//        if(td == 0)  ((CAMMP)getWhitePlayer()).getSearch().getEvaluationFunction().getParams().printParamsShort();
//        else ((CAMMP)getBlackPlayer()).getSearch().getEvaluationFunction().getParams().printParamsShort();
//
//    }
//
//
//
//
//         // this goes into gameloop()
//
//            TDLearning();
//
//            newGame();
//
//            output(black + " - " + white + ": " + blackWin + " - " + whiteWin + " (" + draw + ")");
//
//            if(this.whiteWin >= 10 || this.blackWin >= 10 || draw >= 10){
//
//                output("******************************************");
//                whiteWin = 0;
//                blackWin = 0;
//                draw = 0;
//                black++;
//                if(black == 5){
//                    black = 0;
//                    white++;
//                }
//                ((CAMPP)blackPlayer).param = black;
//                ((CAMPP)blackPlayer).resetAI();
//                ((CAMPP)whitePlayer).param = white;
//                ((CAMPP)whitePlayer).resetAI();
//            }
