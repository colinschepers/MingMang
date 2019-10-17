package mingmang;
/** @author Colin
 */
public interface AI extends Player
{
    public boolean getChange();
    public void stopThinking();
    public void doMove();
    public Move getBestMoveTime(State state, int time);
    public Move getBestMoveDepth(State state, int depth);
    public void thinkDuringOpponentsTime();
    public void dontThinkDuringOpponentsTime(); 
    public void resetAI();
}
