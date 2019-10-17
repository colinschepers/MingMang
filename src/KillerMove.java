package mingmang;

/**
 * @author colin
 */
public class KillerMove extends Move {

    int value;

    public KillerMove(int value, long from, long to){
        super(from, to);
        this.to = to;
    }

}
