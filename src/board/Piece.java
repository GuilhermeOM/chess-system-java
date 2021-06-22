/*
    Gera uma peça que será colocada no tabuleiro, porém quando é gerado uma, 
    ela não terá local no tabuleiro. Assim recebe posição igual a null.
*/

package board;

public class Piece {
    protected Position piece; // -> null. Meaning that, when a new piece is created, It doesn't have a position.
    private Board board;
    
    public Piece(Board board){
        this.board = board;
    }
    
    protected Board getBoard(){
        return board;
    }
}
