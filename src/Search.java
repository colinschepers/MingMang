package mingmang;
/**
 * @author Colin
 */
public interface Search {

    public Move getBestMove(State state, int searchTime);
    public void thinkDuringHumansTurn(State state, Move guess);
    public void dontThinkDuringOpponentsTurn();
    public void stopThinking();
    public EvaluationFunction getEvaluationFunction();
    public HashState[] getTranspositionTable1();
    public HashState[] getTranspositionTable2();
    public void setDepth(int depth);
    public int getNodesVisited();
    public int getBestScore();

}
