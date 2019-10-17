package mingmang;
/** @author Colin
 */
public class Human implements Player {

    private int playerColor;
    private boolean thinking;

    public Human(int playerColor){
        this.playerColor = playerColor;
    }

    public int getPlayerColor(){
        return playerColor;
    }

    public void stopThinking(){}
}
