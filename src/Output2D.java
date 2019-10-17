package mingmang;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.JFrame;
import java.net.URL;
import java.awt.geom.Point2D;
import java.util.ArrayList;
/** @author Colin
 */
public class Output2D {
    private JFrame frame;
    private JFrame output;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private MingMang mingmang;
    private PaintBoard paintBoard; 
    private Listeners listeners;
    private int squareWidth, squareHeight, screenWidth, screenHeight;
    private boolean outputEnabled;

    public Output2D(MingMang mm) {
        mingmang = mm;
        paintBoard = new PaintBoard();
        screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()-20;
        screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()-20;
        squareHeight = (screenHeight - 50 - paintBoard.frameHeight*2 - paintBoard.BOARD_OFFSET*2) / boardSize();
        squareWidth = squareHeight;
        frame = new JFrame();
        frame.setSize(boardSize() * squareWidth, boardSize() * squareHeight);
        frame.setLocation((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()-frame.getWidth(), 0);
        frame.setTitle("Ming Mang");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        listeners = new Listeners();
        frame.setJMenuBar(makeJMenu());
        frame.add(paintBoard);
        frame.setVisible(true);
    }

    public JFrame getFrame(){
        return frame;
    }
    public final int boardSize(){
        return mingmang.getGame().BOARD_SIZE;
    }
    public PaintBoard getPaintBoard(){
        return paintBoard;
    }
    private JMenuBar makeJMenu() {
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        // Create "Game" menu
        JMenu gameMenu = new JMenu("Game");
        menuBar.add(gameMenu);
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(listeners);
        gameMenu.add(newGame);
        JMenuItem saveGame = new JMenuItem("Quick save");
        saveGame.addActionListener(listeners);
        gameMenu.add(saveGame);
        JMenuItem loadGame = new JMenuItem("Quick load");
        loadGame.addActionListener(listeners);
        gameMenu.add(loadGame);
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(listeners);
        gameMenu.add(exit);
        // Create "Options" menu
        JMenu optionsMenu = new JMenu("Options");
        menuBar.add(optionsMenu);
        JMenu playerMenu = new JMenu("Change Player");
        optionsMenu.add(playerMenu);
        JMenu blackMenu = new JMenu("Black");
        playerMenu.add(blackMenu);
        JMenu whiteMenu = new JMenu("White");
        playerMenu.add(whiteMenu);
        JMenuItem humanB = new JMenuItem("Human");
        humanB.addActionListener(listeners);
        blackMenu.add(humanB);
        JMenuItem randomB = new JMenuItem("Random AI");
        randomB.addActionListener(listeners);
        blackMenu.add(randomB);
        JMenu CAMPPB = new JMenu("CAMPP");
        blackMenu.add(CAMPPB);
        JMenuItem CampB10sec = new JMenuItem("10s per game");
        CampB10sec.addActionListener(listeners);
        CAMPPB.add(CampB10sec);
        JMenuItem CampB1min = new JMenuItem("1min per game");
        CampB1min.addActionListener(listeners);
        CAMPPB.add(CampB1min);
        JMenuItem CampB5min = new JMenuItem("5min per game");
        CampB5min.addActionListener(listeners);
        CAMPPB.add(CampB5min);
        JMenuItem CampB15min = new JMenuItem("15min per game");
        CampB15min.addActionListener(listeners);
        CAMPPB.add(CampB15min);
        JMenuItem humanW = new JMenuItem("Human ");
        humanW.addActionListener(listeners);
        whiteMenu.add(humanW);
        JMenuItem randomW = new JMenuItem("Random AI ");
        randomW.addActionListener(listeners);
        whiteMenu.add(randomW);
        JMenu CAMPPW = new JMenu("CAMPP");
        whiteMenu.add(CAMPPW);
        JMenuItem CampW10sec = new JMenuItem("10s per game ");
        CampW10sec.addActionListener(listeners);
        CAMPPW.add(CampW10sec);
        JMenuItem CampW1min = new JMenuItem("1min per game ");
        CampW1min.addActionListener(listeners);
        CAMPPW.add(CampW1min);
        JMenuItem CampW5min = new JMenuItem("5min per game ");
        CampW5min.addActionListener(listeners);
        CAMPPW.add(CampW5min);
        JMenuItem CampW15min = new JMenuItem("15min per game ");
        CampW15min.addActionListener(listeners);
        CAMPPW.add(CampW15min);
//        JMenu sizeMenu = new JMenu("Change Board Size");
//        optionsMenu.add(sizeMenu);
//        JMenuItem x4 = new JMenuItem("4 x 4");
//        x4.addActionListener(listeners);
//        sizeMenu.add(x4);
//        JMenuItem x8 = new JMenuItem("8 x 8");
//        x8.addActionListener(listeners);
//        sizeMenu.add(x8);
        JMenu pieceMenu = new JMenu("Change Visuals");
        optionsMenu.add(pieceMenu);
        JMenuItem ps1 = new JMenuItem("Wood");
        ps1.addActionListener(listeners);
        pieceMenu.add(ps1);
        JMenuItem ps2 = new JMenuItem("Marble");
        ps2.addActionListener(listeners);
        pieceMenu.add(ps2);
        JMenuItem ps3 = new JMenuItem("Simple");
        ps3.addActionListener(listeners);
        pieceMenu.add(ps3);
        JMenuItem outp = new JMenuItem("Show output");
        outp.addActionListener(listeners);
        optionsMenu.add(outp);
        JMenu actionMenu = new JMenu("Action");
        menuBar.add(actionMenu);
        JMenuItem undo = new JMenuItem("Undo");
        undo.addActionListener(listeners);
        actionMenu.add(undo);
        JMenuItem pause = new JMenuItem("Pause");
        pause.addActionListener(listeners);
        actionMenu.add(pause);
        return menuBar;
    }

    private class Listeners implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (((JMenuItem) e.getSource()).getText().equals("New Game")) {
                mingmang.gameOver();
                mingmang.newGame();
                paintBoard.repaint();
            } else if (((JMenuItem) e.getSource()).getText().equals("Quick save")) {
                mingmang.getCurrentState().saveState();
            } else if (((JMenuItem) e.getSource()).getText().equals("Quick load")) {
                mingmang.getCurrentState().loadState();
                paintBoard.repaint();
             } else if (((JMenuItem) e.getSource()).getText().equals("Exit")) {
                System.exit(0);
             } else if (((JMenuItem) e.getSource()).getText().equals("Human")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setBlackPlayer(new Human(mingmang.getGame().BLACK));
             } else if (((JMenuItem) e.getSource()).getText().equals("Random AI")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setBlackPlayer(new RandomAI(mingmang.getGame().BLACK, mingmang));
             } else if (((JMenuItem) e.getSource()).getText().equals("10s per game")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setBlackPlayer(new CAMMP(mingmang, mingmang.getGame().BLACK, 10000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("1min per game")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setBlackPlayer(new CAMMP(mingmang, mingmang.getGame().BLACK, 60000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("5min per game")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setBlackPlayer(new CAMMP(mingmang, mingmang.getGame().BLACK, 300000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("15min per game")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setBlackPlayer(new CAMMP(mingmang, mingmang.getGame().BLACK, 900000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("Human ")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setWhitePlayer(new Human(mingmang.getGame().WHITE));
             } else if (((JMenuItem) e.getSource()).getText().equals("Random AI ")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setWhitePlayer(new RandomAI(mingmang.getGame().WHITE, mingmang));
             } else if (((JMenuItem) e.getSource()).getText().equals("10s per game ")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setWhitePlayer(new CAMMP(mingmang, mingmang.getGame().WHITE, 10000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("1min per game ")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setWhitePlayer(new CAMMP(mingmang, mingmang.getGame().WHITE, 60000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("5min per game ")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setWhitePlayer(new CAMMP(mingmang, mingmang.getGame().WHITE, 300000, false));
             } else if (((JMenuItem) e.getSource()).getText().equals("15min per game ")) {
                mingmang.gameOver();
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.setWhitePlayer(new CAMMP(mingmang, mingmang.getGame().WHITE, 900000, false));
            // change piece set
            } else if (((JMenuItem) e.getSource()).getText().equals("Wood")) {
               paintBoard.setPieceSet(0);
               paintBoard.setBoardSet(0);
               paintBoard.setFrameSet(0);
               paintBoard.setButtonSet(0);
               paintBoard.repaint();
            } else if (((JMenuItem) e.getSource()).getText().equals("Marble")) {
               paintBoard.setPieceSet(1);
               paintBoard.setBoardSet(1);
               paintBoard.setFrameSet(1);
               paintBoard.setButtonSet(1);
               paintBoard.repaint();
            } else if (((JMenuItem) e.getSource()).getText().equals("Simple")) {
               paintBoard.setPieceSet(2);
               paintBoard.setBoardSet(2);
               paintBoard.setFrameSet(2);
               paintBoard.setButtonSet(2);
               paintBoard.repaint(); 
             } else if (((JMenuItem) e.getSource()).getText().equals("Show output")) {
                textArea = new JTextArea();
                textArea.setEditable(false);
                outputEnabled = true;
                scrollPane = new JScrollPane(textArea);
                output = new JFrame();
                output.setSize((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()-frame.getWidth(), boardSize() * squareHeight);
                output.setTitle("Output Frame");
                output.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                output.add(scrollPane);
                output.setVisible(true);
                frame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        outputEnabled = false;
                }});
            } else if (((JMenuItem) e.getSource()).getText().equals("Undo")) {
                if(mingmang.getMoveHistory().size() > 0) {
                    mingmang.setPause(true);
                    mingmang.getMovingPlayer().stopThinking();
                    mingmang.getCurrentState().realUnmove();
                    while(!mingmang.isPaused())
                        try { Thread.sleep(100); } catch(Exception ee) { }
                    mingmang.setPause(false);
                    paintBoard.repaint();
                }
            } else if (((JMenuItem) e.getSource()).getText().equals("Pause")) {
                mingmang.setPause(!mingmang.isPaused());
                mingmang.getMovingPlayer().stopThinking();
            }
        }
    }
    public void appendOutputText(String string){
        if(outputEnabled)
            textArea.append(string);
    }
    public void removeOutputText(){
        textArea.setText("");
    }
    public boolean getOutputEnabled(){
        return outputEnabled;
    }
    public String notationConvert(int square){
       if(square < 0) return "OO";
       String s;
       switch (7 - square%8) {
            case 0: s = "a"; break;
            case 1: s = "b"; break;
            case 2: s = "c"; break;
            case 3: s = "d"; break;
            case 4: s = "e"; break;
            case 5: s = "f"; break;
            case 6: s = "g"; break;
            case 7: s = "h"; break;
            default: s = " "; break;
        }
        return s + Integer.toString(square/8 + 1);
    }

    public class PaintBoard extends JComponent implements MouseListener, MouseWheelListener {
        private Toolkit toolkit;
        private Image[] frames;
        private Image[] buttons;
        private Image[][] boards;
        private Image[][] pieces;
        private Point2D.Float selected;
        private Point2D.Float[] lastMove;
        private int boardSet = 0, pieceSet = 0, frameSet = 0, buttonSet = 0;
        private int frameWidth = 0, frameHeight = 0;
        private final double FRAME_PERC_X = 0.05, FRAME_PERC_Y = 0.05;
        private final int NR_OF_BOARDS = 3, NR_OF_PIECE_SETS = 3, NR_OF_FRAMES = 3, NR_OF_BUTTONS = 3;
        private final int NR_OF_PIECECOLORS = 2, NR_OF_FIELDCOLORS = 2;
        private final int BOARD_OFFSET = 0, SQUARE_OFFSET = 5, BUTTON_OFFSET = 5, SELECTED_PIECE_OFFSET = -1, SELECTED_BUTTON_OFFSET = 2;

        public PaintBoard(){
            frames = new Image[NR_OF_FRAMES];
            buttons = new Image[NR_OF_BUTTONS];
            selected = new Point2D.Float(Integer.MIN_VALUE, Integer.MIN_VALUE);
            lastMove = new Point2D.Float[2];
            lastMove[0] = new Point2D.Float(-1,-1);
            lastMove[1] = new Point2D.Float(-1,-1);
            boards = new Image[NR_OF_BOARDS][NR_OF_FIELDCOLORS];
            pieces = new Image[NR_OF_PIECE_SETS][NR_OF_PIECECOLORS];
            toolkit = Toolkit.getDefaultToolkit();
            loadImages();
            addMouseListener(this);
            addMouseWheelListener(this);
        }

        private Image loadImage(String name) {
            URL imgURL = getClass().getResource(name);
            if (imgURL == null) 
                return toolkit.getImage(name);
            return toolkit.getImage(imgURL);
        }

        private void loadImages() {
            frames[0] = loadImage("../pics/Frame0.png");
            frames[1] = loadImage("../pics/Frame1.png");
            frames[2] = loadImage("../pics/Frame2.png");
            buttons[0] = loadImage("../pics/Button0.png");
            buttons[1] = loadImage("../pics/Button1.png");
            buttons[2] = loadImage("../pics/Button2.png");
            boards[0][mingmang.getGame().BLACK] = loadImage("../pics/Board0_black.png");
            boards[0][mingmang.getGame().WHITE] = loadImage("../pics/Board0_white.png");
            boards[1][mingmang.getGame().BLACK] = loadImage("../pics/Board1_black.png");
            boards[1][mingmang.getGame().WHITE] = loadImage("../pics/Board1_white.png");
            boards[2][mingmang.getGame().BLACK] = loadImage("../pics/Board2_black.png");
            boards[2][mingmang.getGame().WHITE] = loadImage("../pics/Board2_white.png");
            pieces[0][mingmang.getGame().BLACK] = loadImage("../pics/Piece0_black.png");
            pieces[0][mingmang.getGame().WHITE] = loadImage("../pics/Piece0_white.png");
            pieces[1][mingmang.getGame().BLACK] = loadImage("../pics/Piece1_black.png");
            pieces[1][mingmang.getGame().WHITE] = loadImage("../pics/Piece1_white.png");
            pieces[2][mingmang.getGame().BLACK] = loadImage("../pics/Piece2_black.png");
            pieces[2][mingmang.getGame().WHITE] = loadImage("../pics/Piece2_white.png");
        }

        public void setPieceSet(int pieceSet){
            this.pieceSet = pieceSet;
        }
        public void setBoardSet(int boardSet){
            this.boardSet = boardSet;
        }
        public void setFrameSet(int frameSet){
            this.frameSet = frameSet;
        }
        public void setButtonSet(int buttonSet){
            this.buttonSet = buttonSet;
        }

        public void mousePressed(MouseEvent e) {
            if(mingmang.isGameOver()){
                mingmang.getBlackPlayer().stopThinking();
                mingmang.getWhitePlayer().stopThinking();
                mingmang.newGame();
            }
            else if(mingmang.getMovingPlayer() instanceof Human){
                if(isOnPassButton(e.getX(),e.getY())){
                    selected.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE); // passed
                    mingmang.getMovingPlayer().stopThinking();
                    mingmang.getCurrentState().realPass();
                }
                else {
                    int square = convertToSquare(e.getX(), e.getY());
                    int x = square % boardSize();
                    int y = square / boardSize();
                    int oldX = (int)selected.getX();
                    int oldY = (int)selected.getY();
                    int oldSquare = oldX + oldY*boardSize();
                    if(x < 0 || y < 0)
                        selected.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE); // Not clicked on board: deselect
                    else if(oldX == Integer.MIN_VALUE && oldY == Integer.MIN_VALUE && mingmang.getCurrentState().myPiece(mingmang.getCurrentState().getMovingPlayer(), mingmang.getGame().BIN_SQUARE[square]))
                        selected.setLocation(x,y); // select own piece if nothing selected yet
                    else  {
                        if(square >= 0 && oldSquare >= 0) {
                            selected.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE); // make a move
                            if(mingmang.getCurrentState().realMove(mingmang.getGame().BIN_SQUARE[oldSquare],mingmang.getGame().BIN_SQUARE[square]))
                                mingmang.getNonMovingPlayer().stopThinking();
                        }
                        else { 
                            selected.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
                        } // Not a valid move: deselect
                    }
                }
            }
            repaint();
        }
        public void mouseClicked(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mouseWheelMoved(MouseWheelEvent e) {}

        private boolean isOnPassButton(int x, int y){
            return x >= getWidth() - BOARD_OFFSET - frameWidth &&
                   y >= getHeight() - BOARD_OFFSET - frameWidth &&
                   x <= getWidth() - BOARD_OFFSET - BUTTON_OFFSET/2 &&
                   y <= getHeight() - BOARD_OFFSET - BUTTON_OFFSET/2;
        }
        private int convertToSquare(int x, int y){
            x -= (BOARD_OFFSET + frameWidth);
            y -= (BOARD_OFFSET + frameHeight);
            int boardWidth = getWidth() - ((BOARD_OFFSET + frameWidth) * 2);
            int boardHeight = getHeight() - ((BOARD_OFFSET + frameHeight) * 2);
            if(x < 0 || y < 0 || x > boardWidth || y > boardHeight)
                return Integer.MIN_VALUE; // not on a field
            int fieldX = (int)Math.floor(x / squareWidth);
            int fieldY = (int)Math.floor(y / squareHeight);
            int square = (boardSize()-1 - fieldX) + (boardSize()-1 - fieldY) * boardSize();
            // make up for rounding errors
            if(square < 0) square = 0;
            else if(square >= boardSize()*boardSize()) square = boardSize() - 1;
            return square;
        }
        private Point2D.Float convertToCoords(int square){
            int x = square % boardSize();
            int y = square / boardSize();
            x = getWidth() - (BOARD_OFFSET + frameWidth) - (x * squareWidth) - squareWidth;
            y = getHeight() -  (BOARD_OFFSET + frameHeight) - (y * squareHeight) - squareHeight;
            return new Point2D.Float(x,y);
        }
        public void paint(Graphics g){
            Graphics2D g2 = (Graphics2D) g;

            // draw frame
            g2.drawImage(frames[frameSet], BOARD_OFFSET, BOARD_OFFSET,
                this.getWidth() - BOARD_OFFSET*2 , this.getHeight() - BOARD_OFFSET*2, this);

            frameWidth = (int)((this.getWidth()  - BOARD_OFFSET*2) * FRAME_PERC_X);
            frameHeight = (int)((this.getHeight() - BOARD_OFFSET*2) * FRAME_PERC_Y);
            squareWidth = (this.getWidth() - 2*BOARD_OFFSET - 2*frameWidth) / boardSize();
            squareHeight = (this.getHeight() - 2*BOARD_OFFSET - 2*frameHeight) / boardSize();

            boolean whiteField = true;
            g2.setColor(Color.GREEN);

            BasicStroke bs = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2.setStroke(bs);

            for (int y = 0; y < boardSize(); y++) {
                for (int x = 0; x < boardSize(); x++) {
                    int square = x + y * boardSize();
                    Point2D.Float coord = convertToCoords(square);

                    if(whiteField){ // draw white field
                        g2.drawImage(boards[boardSet][mingmang.getGame().WHITE], (int)coord.x,
                                (int)coord.y, squareWidth, squareHeight, this);
                       whiteField = !whiteField;
                    }
                    else { // draw black field
                        g2.drawImage(boards[boardSet][mingmang.getGame().BLACK], (int)coord.x,
                                (int)coord.y, squareWidth, squareHeight, this);
                        whiteField = !whiteField;
                    }
                    if((int)selected.getX() == x && (int)selected.getY() == y){ // draw selection indicator
                        g2.fillOval((int)coord.x - SELECTED_PIECE_OFFSET, (int)coord.y - SELECTED_PIECE_OFFSET,
                                squareWidth + SELECTED_PIECE_OFFSET * 2, squareHeight + SELECTED_PIECE_OFFSET * 2);
                    }
                    if((mingmang.getGame().BIN_SQUARE[square] & mingmang.getCurrentState().getPieces()[mingmang.getGame().BLACK]) != 0){ // draw black piece
                        g2.drawImage(pieces[pieceSet][mingmang.getGame().BLACK], (int)coord.x + SQUARE_OFFSET,
                                (int)coord.y + SQUARE_OFFSET, squareWidth - SQUARE_OFFSET*2, squareHeight - SQUARE_OFFSET*2, this);
                    }
                    else if((mingmang.getGame().BIN_SQUARE[square] & mingmang.getCurrentState().getPieces()[mingmang.getGame().WHITE]) != 0){ // draw white piece
                        g2.drawImage(pieces[pieceSet][mingmang.getGame().WHITE], (int)coord.x + SQUARE_OFFSET,
                                (int)coord.y + SQUARE_OFFSET, squareWidth - SQUARE_OFFSET*2, squareHeight - SQUARE_OFFSET*2, this);
                    }
                }
                whiteField = !whiteField;
            }

            // set last move indicator line
            if( mingmang.getLastMove() != null) {
                final long SQUARE_FROM = mingmang.getLastMove().from;
                final long SQUARE_TO = mingmang.getLastMove().to;
                final int FROM_X = Long.numberOfTrailingZeros(SQUARE_FROM) % boardSize();
                final int FROM_Y = Long.numberOfTrailingZeros(SQUARE_FROM) / boardSize();
                final int TO_X   = Long.numberOfTrailingZeros(SQUARE_TO) % boardSize();
                final int TO_Y   = Long.numberOfTrailingZeros(SQUARE_TO) / boardSize();

                if(FROM_X < boardSize() || FROM_Y < boardSize() ||
                        TO_X < boardSize() || TO_Y < boardSize() ||
                    FROM_X >= 0 || FROM_Y >= 0 || TO_X >= 0 || TO_Y >= 0) {

                    if(SQUARE_FROM == -1 || SQUARE_FROM == -2){
                        // pass occured last turn
                        if(SQUARE_FROM == -1)
                            g2.setColor(new Color(0, 0, 0, 0.5F));
                        else g2.setColor(new Color(1, 1, 1, 0.5F));

                        if(buttonSet == 0)
                            g2.fillOval(getWidth() - BOARD_OFFSET - frameWidth - BUTTON_OFFSET - SELECTED_BUTTON_OFFSET*2,
                                getHeight() - BOARD_OFFSET - frameHeight - BUTTON_OFFSET - SELECTED_BUTTON_OFFSET*2,
                                frameWidth + BUTTON_OFFSET/2 + SELECTED_BUTTON_OFFSET*4,
                                frameHeight + BUTTON_OFFSET/2 + SELECTED_BUTTON_OFFSET*4);
                        else
                            g2.fillRect(getWidth() - BOARD_OFFSET - frameWidth - BUTTON_OFFSET - SELECTED_BUTTON_OFFSET*2,
                                getHeight() - BOARD_OFFSET - frameHeight - BUTTON_OFFSET - SELECTED_BUTTON_OFFSET*2,
                                frameWidth + BUTTON_OFFSET/2 + SELECTED_BUTTON_OFFSET*4,
                                frameHeight + BUTTON_OFFSET/2 + SELECTED_BUTTON_OFFSET*4);
                    }
                    else {
                        // draw last move indicator
                        if((SQUARE_FROM & mingmang.getCurrentState().getPieces()[mingmang.getGame().BLACK]) != 0)
                            g2.setColor(new Color(0, 0, 0, 0.3F));
                        else g2.setColor(new Color(1, 1, 1, 0.3F));

                        if(FROM_X >= 0){
                            g2.drawLine((int)convertToCoords(FROM_X + FROM_Y*boardSize()).getX()+squareWidth/2,
                                        (int)convertToCoords(FROM_X + FROM_Y*boardSize()).getY()+squareHeight/2,
                                        (int)convertToCoords(TO_X + TO_Y*boardSize()).getX()+squareWidth/2,
                                        (int)convertToCoords(TO_X + TO_Y*boardSize()).getY()+squareHeight/2);
                        }
                    }
                }
            }
            // draw pass button
            g2.drawImage(buttons[buttonSet],
                    getWidth() - BOARD_OFFSET - frameWidth - BUTTON_OFFSET,
                    getHeight() - BOARD_OFFSET - frameHeight - BUTTON_OFFSET,
                    frameWidth + BUTTON_OFFSET/2,
                    frameHeight + BUTTON_OFFSET/2,
                    this);
        }
    }
}
