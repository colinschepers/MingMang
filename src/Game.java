package mingmang;

import java.util.HashMap;
import java.util.Random;
/**
 * @author Administrator
 */
public class Game  {

    public Random rand;

    // De Bruijn used for retrieving first set bit number from bitboard
    private final long DE_BRUIJN;
    private final int[] DE_BRUYN_BIT_TABLE;

    // bit boards for each square (0-63)
    public final long[] BIN_SQUARE;

    // bit boards for ranks and files (0-8)
    public final long[] RANK;
    public final long[] FILE;
    public final long[] NEIGHBORS;

    // bit boards for all squares north, east, south and west of a square
    public final long[] NORTH;
    public final long[] EAST;
    public final long[] SOUTH;
    public final long[] WEST;

    // bit boards for important areas on the board
    public final long DIAGONAL_8;
    public final long[] VARIATIONS_DIAGONAL_8;
    public final long MAGIC_DIAGONAL_8;
    public final int SHIFT_DIAGONAL_8;
    public final int[] RESULT_DIAGONAL_8;

    // bit boards for important areas on the board
    public final long CENTER_4;
    public final long[] VARIATIONS_CENTER_4;
    public final long MAGIC_CENTER_4;
    public final int SHIFT_CENTER_4;
    public final int[] RESULT_CENTER_4;

    // bit boards for important areas on the board
    public final long CENTER_16;
    public final long[] VARIATIONS_CENTER_16;
    public final long MAGIC_CENTER_16;
    public final int SHIFT_CENTER_16;
    public final int[] RESULT_CENTER_16;

    // bit boards for important areas on the board
    public final long CORNERS_4;
    public final long[] VARIATIONS_CORNERS_4;
    public final long MAGIC_CORNERS_4;
    public final int SHIFT_CORNERS_4;
    public final int[] RESULT_CORNERS_4;

    // bit boards for important areas on the board
    public final long EDGES_28;

    // Used for generating possible moves
    public final long[] MOVE_MASK;
    public final long[][] MOVE_OCCUPANCY_VARIATION;
    public final long[] MOVE_MAGIC_NUMBER;
    public final int[] MOVE_MAGIC_SHIFT;
    public final long[][] MOVE_RESULT;
    
    // Table with randoms using for zobrist hashing
    public final int ZOBRIST_SEED;
    public final long[] ZOBRIST_RANDOM;
//    public final long[][] ZOBRIST_VARIATIONS;
    public final long[] ZOBRIST_RANDOM_CAPTURE;

    // identifiers for EMPTY, BLACK or WHITE squares/players
    public final int EMPTY = -1;
    public final int BLACK = 0;
    public final int WHITE = 1;
    public final int BOARD_SIZE = 8;

    public Game(){
        this.rand = new Random();

        DE_BRUIJN = 0x07edd5e59a4e28c2L;
        DE_BRUYN_BIT_TABLE = initDeBruynBitTable();
 
        BIN_SQUARE = initBinSquares();

        RANK = initRanks();
        FILE = initFiles();
        NEIGHBORS = initNeighbors();

        NORTH = initNorth();
        EAST = initEast();
        SOUTH = initSouth();
        WEST = initWest();

        DIAGONAL_8 = initDiagonal8();
        VARIATIONS_DIAGONAL_8 = initAreaVariations(DIAGONAL_8);
        MAGIC_DIAGONAL_8 = 40603315725288456L;
        SHIFT_DIAGONAL_8 = 56;
        RESULT_DIAGONAL_8 = initAreaResult(VARIATIONS_DIAGONAL_8, MAGIC_DIAGONAL_8, SHIFT_DIAGONAL_8);

        CENTER_4 = initCenter4();
        VARIATIONS_CENTER_4 = initAreaVariations(CENTER_4);
        MAGIC_CENTER_4 = 144220779913940996L;
        SHIFT_CENTER_4 = 60;
        RESULT_CENTER_4 = initAreaResult(VARIATIONS_CENTER_4, MAGIC_CENTER_4, SHIFT_CENTER_4);

        CENTER_16 = initCenter16();
        VARIATIONS_CENTER_16 = initAreaVariations(CENTER_16);
        MAGIC_CENTER_16 = 72075186492473344L;
        SHIFT_CENTER_16 = 48;
        RESULT_CENTER_16 = initAreaResult(VARIATIONS_CENTER_16, MAGIC_CENTER_16, SHIFT_CENTER_16);

        CORNERS_4 = initCorners4();
        VARIATIONS_CORNERS_4 = initAreaVariations(CORNERS_4);
        MAGIC_CORNERS_4 = 4620695416839603745L;
        SHIFT_CORNERS_4 = 60;
        RESULT_CORNERS_4 = initAreaResult(VARIATIONS_CORNERS_4, MAGIC_CORNERS_4, SHIFT_CORNERS_4);

        EDGES_28 = initEdges28();

        MOVE_MASK = initMoveMasks();
        MOVE_OCCUPANCY_VARIATION = initMoveOccupancyVariation();
        MOVE_MAGIC_NUMBER = initMoveMagicNumbers();
        MOVE_MAGIC_SHIFT = initMoveMagicShifts();
        MOVE_RESULT = initMoveResult();

        ZOBRIST_SEED = 408662; // my student number
        ZOBRIST_RANDOM = initZobristRandom();
//        ZOBRIST_VARIATIONS = initZobristVariations();
        ZOBRIST_RANDOM_CAPTURE = initZobristRandomCapture();

    }
    public Move pass(int playerColor){
        if(playerColor == BLACK)
            return new Move(-1,-1);
        else return new Move(-2,-2);
    }

    public int[] getBitsSet(long bb){
        int[] squares = new int[Long.bitCount(bb)];
        int count = 0;
        for (int i = 0; i < BOARD_SIZE*BOARD_SIZE; i++) {
            if((bb & 1) == 1){
                 squares[count] = i;
                 count++;
            }
            bb = bb >>> 1;
        }
        return squares;
    }

    private long[] initBinSquares(){
        long[] squares = new long[BOARD_SIZE*BOARD_SIZE];
        squares[0] = 1;
        for (int i = 1; i < squares.length; i++){
            if(i == 63) squares[i] = Long.MIN_VALUE;
            else squares[i] = squares[i - 1] * 2;
        }
        return squares;
    }
    private long[] initRanks(){
        long[] ranks = new long[BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            long rank = 0;
            for (int f = 0; f < BOARD_SIZE; f++)
                rank = rank | BIN_SQUARE[r*BOARD_SIZE + f];
            ranks[r] = rank;
        }
        return ranks;
    }
    private long[] initFiles(){
        long[] files = new long[BOARD_SIZE];
        for (int f = 0; f < BOARD_SIZE; f++) {
            long file = 0;
            for (int r = 0; r < BOARD_SIZE; r++)
                file = file | BIN_SQUARE[r*BOARD_SIZE + (BOARD_SIZE-1-f)];
            files[f] = file;
        }
        return files;
    }
    private long[] initNeighbors(){
        long[] neighbors = new long[BOARD_SIZE*BOARD_SIZE];
        for (int i = 0; i < neighbors.length; i++) {
            long nBor = 0;
            if(i % BOARD_SIZE != BOARD_SIZE-1)
                nBor |= BIN_SQUARE[i] << 1L;
            if(i % BOARD_SIZE != 0)
                nBor |= BIN_SQUARE[i] >>> 1L;
            if(i / BOARD_SIZE != BOARD_SIZE-1)
                nBor |= BIN_SQUARE[i] << BOARD_SIZE;
            if(i / BOARD_SIZE != 0)
                nBor |= BIN_SQUARE[i] >>> BOARD_SIZE;
            neighbors[i] = nBor;
        }
        return neighbors;
    }
    private long[] initNorth(){
        long[] north = new long[BOARD_SIZE*BOARD_SIZE];
        for (int i = 0; i < north.length; i++) {
            north[i] = ~(BIN_SQUARE[i] - 1) & ~(BIN_SQUARE[i]) & FILE[(BOARD_SIZE - 1) - (i % BOARD_SIZE)];
        }
        return north;
    }
    private long[] initEast(){
        long[] east = new long[BOARD_SIZE*BOARD_SIZE];
        for (int i = 0; i < east.length; i++) {
            east[i] = (BIN_SQUARE[i] - 1) & ~(BIN_SQUARE[i]) & RANK[i / BOARD_SIZE];
        }
        return east;
    }
    private long[] initSouth(){
        long[] south = new long[BOARD_SIZE*BOARD_SIZE];
        for (int i = 0; i < south.length; i++) {
            south[i] = (BIN_SQUARE[i] - 1) & ~(BIN_SQUARE[i]) & FILE[(BOARD_SIZE - 1) - (i % BOARD_SIZE)];
        }
        return south;
    }
    private long[] initWest(){
        long[] west = new long[BOARD_SIZE*BOARD_SIZE];
        for (int i = 0; i < west.length; i++) {
            west[i] = ~(BIN_SQUARE[i] - 1) & ~(BIN_SQUARE[i]) & RANK[i / BOARD_SIZE];
        }
        return west;
    }

    private long initDiagonal8(){
        return (FILE[0] & RANK[0]) | (FILE[1] & RANK[1]) | (FILE[2] & RANK[2]) | (FILE[3] & RANK[3]) |
               (FILE[4] & RANK[4]) | (FILE[5] & RANK[5]) | (FILE[6] & RANK[6]) | (FILE[7] & RANK[7]);
    }
    private long initCenter4(){
        return (FILE[3] | FILE[4]) & (RANK[3] | RANK[4]);
    }
    private long initCenter16(){
        return (FILE[2] | FILE[3] | FILE[4] | FILE[5]) & (RANK[2] | RANK[3] | RANK[4] | RANK[5]) &
                ~CENTER_4;
    }
    private long initCorners4(){
        return (FILE[0] | FILE[7]) & (RANK[0] | RANK[7]);
    }
    private long initEdges28(){
        return (FILE[0] | FILE[7] | RANK[0] | RANK[7]);
    }

    private long[] initAreaVariations(long area){
        long[] variations = null;
        int v;
        int variationCount;
        int[] maskArray, setBitsIndex;
        maskArray = getBitsSet(area);
        variationCount = (int)(1L << Long.bitCount(area));
        variations = new long[variationCount];
        for (v = 0; v < variationCount; v++) {
            variations[v] = 0;
            setBitsIndex = getBitsSet(v);
            for (int j = 0; j < setBitsIndex.length; j++){
                variations[v] |= (1L << maskArray[setBitsIndex[j]]);
            }
        }
        return variations;
    }

    private int[] initAreaResult(long[] variations, long magic, int shift){
        int[] result = new int[(1 << (64 - shift))];
        int index;
        for (int v = 0; v < variations.length; v++) {
            index = (int)((variations[v] * magic) >>> shift);
            result[index] = Long.bitCount(variations[v]);
        }
        return result;
    }

    private long[] initMoveMasks(){
        return new long[]{
            0x101010101017eL, 0x202020202027cL, 0x404040404047aL, 0x8080808080876L, 0x1010101010106eL, 0x2020202020205eL, 0x4040404040403eL, 0x8080808080807eL,
            0x1010101017e00L, 0x2020202027c00L, 0x4040404047a00L, 0x8080808087600L, 0x10101010106e00L, 0x20202020205e00L, 0x40404040403e00L, 0x80808080807e00L,
            0x10101017e0100L, 0x20202027c0200L, 0x40404047a0400L, 0x8080808760800L, 0x101010106e1000L, 0x202020205e2000L, 0x404040403e4000L, 0x808080807e8000L,
            0x101017e010100L, 0x202027c020200L, 0x404047a040400L, 0x8080876080800L, 0x1010106e101000L, 0x2020205e202000L, 0x4040403e404000L, 0x8080807e808000L,
            0x1017e01010100L, 0x2027c02020200L, 0x4047a04040400L, 0x8087608080800L, 0x10106e10101000L, 0x20205e20202000L, 0x40403e40404000L, 0x80807e80808000L,
            0x17e0101010100L, 0x27c0202020200L, 0x47a0404040400L, 0x8760808080800L, 0x106e1010101000L, 0x205e2020202000L, 0x403e4040404000L, 0x807e8080808000L,
            0x7e010101010100L, 0x7c020202020200L, 0x7a040404040400L, 0x76080808080800L, 0x6e101010101000L, 0x5e202020202000L, 0x3e404040404000L, 0x7e808080808000L,
            0x7e01010101010100L, 0x7c02020202020200L, 0x7a04040404040400L, 0x7608080808080800L, 0x6e10101010101000L, 0x5e20202020202000L, 0x3e40404040404000L, 0x7e80808080808000L
        };
    }
    private long[] initMoveMagicNumbers(){
        return new long[]{
            0xa180022080400230L, 0x40100040022000L,   0x80088020001002L,   0x80080280841000L,   0x4200042010460008L, 0x4800a0003040080L,  0x400110082041008L, 0x8000a041000880L,
            0x10138001a080c010L, 0x804008200480L,     0x10011012000c0L,    0x22004128102200L,   0x200081201200cL,    0x202a001048460004L, 0x81000100420004L,  0x4000800380004500L,
            0x208002904001L,     0x90004040026008L,   0x208808010002001L,  0x2002020020704940L, 0x8048010008110005L, 0x6820808004002200L, 0xa80040008023011L, 0xb1460000811044L,
            0x4204400080008ea0L, 0xb002400180200184L, 0x2020200080100380L, 0x10080080100080L,   0x2204080080800400L, 0xa40080360080L,     0x2040604002810b1L, 0x8c218600004104L,
            0x8180004000402000L, 0x488c402000401001L, 0x4018a00080801004L, 0x1230002105001008L, 0x8904800800800400L, 0x42000c42003810L,   0x8408110400b012L,  0x18086182000401L,
            0x2240088020c28000L, 0x1001201040c004L,   0xa02008010420020L,  0x10003009010060L,   0x4008008008014L,    0x80020004008080L,   0x282020001008080L, 0x50000181204a0004L,
            0x102042111804200L,  0x40002010004001c0L, 0x19220045508200L,   0x20030010060a900L,  0x8018028040080L,    0x88240002008080L,   0x10301802830400L,  0x332a4081140200L,
            0x8080010a601241L,   0x1008010400021L,    0x4082001007241L,    0x211009001200509L,  0x8015001002441801L, 0x801000804000603L,  0xc0900220024a401L, 0x1000200608243L
        };
    }
    private int[] initMoveMagicShifts(){
        return new int[]{
            52,53,53,53,53,53,53,52,
            53,54,54,54,54,54,54,53,
            53,54,54,54,54,54,54,53,
            53,54,54,54,54,54,54,53,
            53,54,54,54,54,54,54,53,
            53,54,54,54,54,54,54,53,
            53,54,54,54,54,54,54,53,
            52,53,53,53,53,53,53,52
        };
    }
    public long[][] initMoveOccupancyVariation() {
        int v, j, s;
        int variationCount;
        int[] maskArray, setBitsIndex;
        long[][] occupancyVariation = new long[BOARD_SIZE*BOARD_SIZE][];
        for (s = 0; s < BOARD_SIZE*BOARD_SIZE; s++) {
            maskArray = getBitsSet(MOVE_MASK[s]);
            variationCount = (int)(1L << Long.bitCount(MOVE_MASK[s]));
            occupancyVariation[s] = new long[variationCount];
            for (v = 0; v < variationCount; v++) {
                occupancyVariation[s][v] = 0;
                setBitsIndex = getBitsSet(v);
                for (j = 0; j < setBitsIndex.length; j++){
                    occupancyVariation[s][v] |= (1L << maskArray[setBitsIndex[j]]);
                }
            }
        }
        return occupancyVariation;
    }
    public long[][] initMoveResult(){
        long[][] magicMove = new long[BOARD_SIZE*BOARD_SIZE][(int)(1L << 12)];
        long validMoves;
        int variations;
        int s, v, i, magicIndex;
        for (s = 0; s < BOARD_SIZE*BOARD_SIZE; s++){
            variations = (int)(1L << Long.bitCount(MOVE_MASK[s]));
            for (v = 0; v < variations; v++) {
                validMoves = 0;
                magicIndex = (int)((MOVE_OCCUPANCY_VARIATION[s][v] * MOVE_MAGIC_NUMBER[s]) >>> MOVE_MAGIC_SHIFT[s]);
                for (i = s+BOARD_SIZE; i < BOARD_SIZE*BOARD_SIZE; i += BOARD_SIZE) {
                    validMoves |= (1L << i);
                    if ((MOVE_OCCUPANCY_VARIATION[s][v] & (1L << i)) != 0)
                        break;
                }
                for (i = s-BOARD_SIZE; i >= 0; i -= BOARD_SIZE) {
                    validMoves |= (1L << i);
                    if ((MOVE_OCCUPANCY_VARIATION[s][v] & (1L << i)) != 0)
                        break;
                }
                for (i = s+1; i%BOARD_SIZE != 0; i++) {
                    validMoves |= (1L << i);
                    if ((MOVE_OCCUPANCY_VARIATION[s][v] & (1L << i)) != 0)
                        break;
                }
                for (i = s-1; i%BOARD_SIZE != BOARD_SIZE-1 && i >= 0; i--) {
                    validMoves |= (1L << i);
                    if ((MOVE_OCCUPANCY_VARIATION[s][v] & (1L << i)) != 0)
                        break;
                }
                magicMove[s][magicIndex] = validMoves;
            }
        }
        return magicMove;
    }
    public int[] initDeBruynBitTable(){
        return new int[]{
            63,  0, 58,  1, 59, 47, 53,  2,
            60, 39, 48, 27, 54, 33, 42,  3,
            61, 51, 37, 40, 49, 18, 28, 20,
            55, 30, 34, 11, 43, 14, 22,  4,
            62, 57, 46, 52, 38, 26, 32, 41,
            50, 36, 17, 19, 29, 10, 13, 21,
            56, 45, 25, 31, 35, 16,  9, 12,
            44, 24, 15,  8, 23,  7,  6,  5
        };
    }

    public int getLS1Bint(long bb){
        return DE_BRUYN_BIT_TABLE[(int)(((bb & (0-bb)) * DE_BRUIJN) >>> 58)];
    }
    public long getLS1Bbb(long bb){
        return bb & (0 - bb);
    }
    public int getDiagonal8(long pieces){
        return RESULT_DIAGONAL_8[(int)(((pieces & DIAGONAL_8) * MAGIC_DIAGONAL_8) >>> SHIFT_DIAGONAL_8)];
    }
    public int getCenter4(long pieces){
        return RESULT_CENTER_4[(int)(((pieces & CENTER_4) * MAGIC_CENTER_4) >>> SHIFT_CENTER_4)];
    }
    public int getCenter16(long pieces){
        return RESULT_CENTER_16[(int)(((pieces & CENTER_16) * MAGIC_CENTER_16) >>> SHIFT_CENTER_16)];
    }
    public int getCorners4(long pieces){
        return RESULT_CORNERS_4[(int)(((pieces & CORNERS_4) * MAGIC_CORNERS_4) >>> SHIFT_CORNERS_4)];
    }
    public int getEdges28(long pieces){
        return this.bitCount(pieces & EDGES_28);
    }
    public long[] initZobristRandom(){
        long[] zobristRandom = new long[BOARD_SIZE*BOARD_SIZE * 2 + 1];
        Random random = new Random();
        random.setSeed(ZOBRIST_SEED);
        for (int i = 0; i < zobristRandom.length; i++)
            zobristRandom[i] = random.nextLong();
        return zobristRandom;
    }
    public long[][] initZobristVariations(){
        long[][] var = new long[BOARD_SIZE*BOARD_SIZE][];
        for (int s = 0; s < BOARD_SIZE*BOARD_SIZE; s++) {
            int i = 0;
            long sL = this.BIN_SQUARE[s];
            int sx = s%BOARD_SIZE;
            int sy = s/BOARD_SIZE;
            long[] varS = new long[max(1,sx+1) * max(1,sy+1) * max(1,BOARD_SIZE-sx) * max(1,BOARD_SIZE-sy)];
            long variation = this.BIN_SQUARE[s];
            for (int north = 0; north < BOARD_SIZE-sy; north++) {
                if(north != 0) variation |= (sL << north*BOARD_SIZE);
                for (int south = 0; south <= sy; south++) {
                    if(south != 0) variation |= (sL >>> south*BOARD_SIZE);
                    for (int west = 0; west < BOARD_SIZE-sx; west++) {
                        if(west != 0) variation |= (sL << west);
                        for (int east = 0; east <= sx; east++) {
                            if(east != 0) variation |= (sL >>> east);
                            varS[i] = variation & ~sL;
                            i++;
                        }
                        variation &= ~this.EAST[s];
                    }
                    variation &= ~this.WEST[s];
                }
                variation &= ~this.SOUTH[s];
            }
            var[s] = varS;
        }
        return var;
    }
    private long[] initZobristRandomCapture(){
        long[] zobCap = new long[BOARD_SIZE*BOARD_SIZE];
        for (int s = 0; s < BOARD_SIZE*BOARD_SIZE; s++) {
            zobCap[s] = ZOBRIST_RANDOM[s] ^ ZOBRIST_RANDOM[BOARD_SIZE*BOARD_SIZE + s];
        }
        return zobCap;
    }
    public long getZobristCapture(long captureArea){
        long hashKey = 0;
        while(captureArea != 0){
            int square = getLS1Bint(captureArea);
            hashKey ^= ZOBRIST_RANDOM[0*64 + square] ^ ZOBRIST_RANDOM[1*64 + square];
            captureArea &= ~BIN_SQUARE[square];
        }
        return hashKey;
    }
    public int bitCount(long bb){
        bb -= (bb >>> 1) & 0x5555555555555555L;
        bb =  (bb & 0x3333333333333333L) + ((bb >>> 2) & 0x3333333333333333L);
        bb =  (bb + (bb >>> 4)) & 0xf0f0f0f0f0f0f0fL;
        bb =  (bb * 0x101010101010101L) >>> 56;
        return ((int)bb);
    }public int max(int a, int b){
        if(a >= b) return a;
        return b;
    }
    public int min(int a, int b){
        if(a <= b) return a;
        return b;
    }
    private void findMagicAndShiftValue(long area, long[] variations){
        int i, j;
        Random r = new Random();
        long magicNumber = 0;
        int index;
        int bitCount = Long.bitCount(area);
        int variationCount = variations.length;
        boolean fail;
        long usedBy[] = new long[(int)(1L << bitCount)];
        int attempts = 0;
        do  {
            magicNumber = r.nextLong() & r.nextLong() & r.nextLong(); // generate a random number with not many bits set
            for (j = 0; j < variationCount; j++)
                usedBy[j] = -1;
            attempts ++;
            System.out.println("Attempt: " + attempts);
            for (i = 0, fail = false; i < variationCount && !fail; i++)
            {
                index = (int)((variations[i] * magicNumber) >>> (64-bitCount));
                fail = usedBy[index] != -1 && usedBy[index] != Long.numberOfTrailingZeros(variations[i]);
                usedBy[index] = Long.numberOfTrailingZeros(variations[i]);
            }
        }
        while (fail);
        System.out.println("Magic: " + magicNumber);
        System.out.println("Shift: " + (64-bitCount));
    }
}
