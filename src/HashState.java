package mingmang;

/**
 * @author colin
 */
public class HashState implements java.io.Serializable {

    // identification of position
    public long hashKey;

    // value of the position and type of value
    public int value;
    public int type;

    // best move
    public long from;
    public long to;

    // depth searched
    public int branchSize;
    public int depth;

    public HashState(long hashKey, int value, int type, long from, long to, int depth, int branchSize){
        this.hashKey = hashKey;
        this.value = value;
        this.type = type;
        this.from = from;
        this.to = to;
        this.depth = depth;
        this.branchSize = branchSize;
    }

}
