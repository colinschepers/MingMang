package mingmang;

import java.util.ArrayList;

/**
 * @author colin
 */
public class OpeningState implements java.io.Serializable {

    public long hash;

    public OpeningState parent;
    public ArrayList<OpeningState> children;
    public ArrayList<Move> movesToChildren;
    public ArrayList<Integer> movePlayed;
    public ArrayList<Integer> moveWon;

    public OpeningState(long hash, OpeningState parent){
        this.hash = hash;
        this.parent = parent;
        this.children = new ArrayList<OpeningState>();
        this.movesToChildren = new ArrayList<Move>();
        this.movePlayed = new ArrayList<Integer>();
        this.moveWon = new ArrayList<Integer>(); 
    }
 
}
