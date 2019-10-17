package mingmang;
/**
 * @author colin
 */
public class EvaluationFunctionParams implements java.io.Serializable {

    public int pieceWeight           =    50;
    public int territoryWeight       =    10;
    public int areaWeight            =     2;
    public int movingPlayerWeight    =     3;

    public void setParam(int paramNr, int value){
        switch (paramNr) {
            case 0: pieceWeight             = value; break;
            case 1: territoryWeight         = value; break;
            case 2: areaWeight              = value; break;
            case 3: movingPlayerWeight      = value; break;
            default: break;
        } 
    }
    public void setParams(int[] params){
        pieceWeight         = params[0];
        territoryWeight     = params[1];
        areaWeight          = params[2];
        movingPlayerWeight  = params[3];
    }
    public int[] getParams(){
        return new int[]{pieceWeight, territoryWeight, areaWeight, movingPlayerWeight};
    }
    public void printParams(){
        System.out.println("pieceWeight: " + pieceWeight);
        System.out.println("territoryWeight: " + territoryWeight);
        System.out.println("areaWeight: " + areaWeight);
        System.out.println("movingPlayerWeight: " + movingPlayerWeight);
    }
    public void printParamsShort(){
        System.out.println(pieceWeight + " " + territoryWeight + " " +  areaWeight + " " + movingPlayerWeight);
    }
}
