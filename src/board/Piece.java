package board;

public class Piece {
    protected Position position; // -> null. Meaning that, when a new piece is created, It doesn't have a position.
    private Board board;
    
    public Piece(Board board){
        this.board = board;
    }
    
    protected Board getBoard(){
        return board;
    }
}
