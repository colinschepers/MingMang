package mingmang;
/**
 * @author colin
 */
public class Move implements java.io.Serializable {

    long from;
    long to;

    public Move(long from, long to){
        this.from = from;
        this.to = to;
    }

    public Move clone(){
        return new Move(from,to);
    }
}
