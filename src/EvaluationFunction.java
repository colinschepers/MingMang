package mingmang;

/**
 * @author Colin
 */
public class EvaluationFunction {

    private CAMMP ai;
    private EvaluationFunctionParams params;
    
    public EvaluationFunction(MingMang mingmang, CAMMP ai){
        this.ai = ai;
//        Object object = mingmang.getTools().load("EFParams");
//        if(object != null && object instanceof EvaluationFunctionParams)
//            params = (EvaluationFunctionParams)object;
//        else
            params = new EvaluationFunctionParams();
    }

    public int evaluate(State state){
        return    params.pieceWeight * getPieceValue(state)
                + params.territoryWeight * getTerritoryValue(state)
                + params.areaWeight * getAreaValue(state)
                + params.movingPlayerWeight * getMovingPlayerValue(state);
    }

    public int getPieceValue(State state){
        return (ai.game.bitCount(state.getPieces()[state.getMovingPlayer()])
                      - ai.game.bitCount(state.getPieces()[~state.getMovingPlayer()&1]));
    }
    public int getTerritoryValue(State state){
        return state.getTerrVal(state.getMovingPlayer());
    }
    public int getAreaValue(State state){
        return (ai.game.getDiagonal8(state.getPieces()[state.getMovingPlayer()])
                      - ai.game.getDiagonal8(state.getPieces()[~state.getMovingPlayer()&1]));
    }
    public int getMovingPlayerValue(State state){
        return 1;
    }
    public EvaluationFunctionParams getParams() {
        return params;
    }
    public void setParams(EvaluationFunctionParams params) {
        this.params = params;
    }
    public void outputEvaluation(State state){
        int terr = state.getTerrVal(state.getMovingPlayer());

        int score =   params.pieceWeight * getPieceValue(state)
                    + params.territoryWeight * terr
                    + params.areaWeight * getAreaValue(state)
                    + params.movingPlayerWeight * getMovingPlayerValue(state);

        ai.mingmang.output((params.pieceWeight * getPieceValue(state)) + " + "
                    + (params.territoryWeight * terr)  + " + "
                    + (params.areaWeight * getAreaValue(state))  + " + "
                    + (params.movingPlayerWeight * getMovingPlayerValue(state)) + " = " + score);
    }
}
