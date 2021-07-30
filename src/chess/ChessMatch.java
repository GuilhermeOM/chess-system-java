package chess;

import board.Board;
import board.Piece;
import board.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChessMatch {
    private int turn;
    private Color currentPlayer;
    private Board board;
    private boolean check;
    private boolean checkmate;
    private ChessPiece enPassantVulnerable;
    
    /*
    Letting it be a Piece type instead of ChessPiece in order to let it be more generic, 
    thus the list will be able to accept any type of piece.
    */
    private List<Piece> piecesOnTheBoard = new ArrayList<>(); //List of all pieces on the board
    private List<Piece> capturedPieces = new ArrayList<>(); //List of all captured pieces
    
    
    public ChessMatch(){
        board = new Board(8, 8);
        initialSetup();
        
        turn = 1;
        currentPlayer = Color.WHITE;
    }
    
    public int getTurn(){
        return turn;
    }
    
    public Color getCurrentPlayer(){
        return currentPlayer;
    }
     
    public boolean getCheck(){
        return check;
    }
    
    public boolean getCheckmate(){
        return checkmate;
    }
    
    public ChessPiece getEnPassantVulnerable(){
        return enPassantVulnerable;
    }
    
    public ChessPiece[][] getPieces(){
        ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
        
        for(int i = 0; i < board.getRows(); i++){
            for(int j = 0; j < board.getColumns(); j++){
                mat[i][j] = (ChessPiece) board.piece(i, j);
            }
        }
        return mat;
    }
    
    public boolean[][] possibleMoves(ChessPosition sourcePosition){
        Position position = sourcePosition.toPosition();
        validateSourcePosition(position);
        return board.piece(position).possibleMoves();
    }
    
    public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition){
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition();
        validateSourcePosition(source);
        validateTargetPosition(source, target);
        Piece capturedPiece = makeMove(source, target);
        
        //If the player puts himself in check.
        if(testCheck(currentPlayer)){
            undoMove(source, target, capturedPiece);
            throw new ChessException("You can not put yourself in check.");
        }
        
        ChessPiece movedPiece = (ChessPiece) board.piece(target);
        
        //Checks if the match is in check or not;
        check = (testCheck(opponent(currentPlayer))) ? true : false;
        
        if(testCheckMate(opponent(currentPlayer))){
            checkmate = true;
        }
        else{
            nextTurn();
        }
        
        //#Special move En Passant
        if(movedPiece instanceof Pawn && (target.getRow() == source.getRow() + 2 || target.getRow() == source.getRow() - 2)){
            enPassantVulnerable = movedPiece;
        }
        else{
            enPassantVulnerable = null;
        }
        
        return (ChessPiece) capturedPiece;
    }
    
    private Piece makeMove(Position source, Position target){
        /*Take the piece from the board.
          In case there is a piece on the target position, it'll be taken,
          then I place mine there.
        */
        ChessPiece p = (ChessPiece) board.removePiece(source);
        p.increaseMoveCount();
        
        Piece capturedPiece = board.removePiece(target);
        board.placePiece(p, target);
        
        if(capturedPiece != null){
            piecesOnTheBoard.remove(capturedPiece);
            capturedPieces.add(capturedPiece);
        }
        
        //#Special move castling kingside rook
        if(p instanceof King && target.getColumn() == source.getColumn() + 2){
            Position sourceT1 = new Position(source.getRow(), source.getColumn() + 3);
            Position targetT1 = new Position(source.getRow(), source.getColumn() + 1);
            
            ChessPiece rook = (ChessPiece)board.removePiece(sourceT1);
            board.placePiece(rook, targetT1);
            rook.increaseMoveCount();
        }
        
        //#Special move castling queenside rook
        if(p instanceof King && target.getColumn() == source.getColumn() - 2){
            Position sourceT2 = new Position(source.getRow(), source.getColumn() - 4);
            Position targetT2 = new Position(source.getRow(), source.getColumn() - 1);
            
            ChessPiece rook = (ChessPiece)board.removePiece(sourceT2);
            board.placePiece(rook, targetT2);
            rook.increaseMoveCount();
        }
        
        //#Special move En passant
        if(p instanceof Pawn){
            //if the pawn is able to move diagonal being there any piece it means that he did an En Passant
            if(source.getColumn() != target.getColumn() && capturedPiece == null){
                Position pawnPosition;
                if(p.getColor() == Color.WHITE){
                    pawnPosition = new Position(target.getRow() + 1, target.getColumn());
                }
                else{
                    pawnPosition = new Position(target.getRow() - 1, target.getColumn());
                }
                capturedPiece = board.removePiece(pawnPosition);
                capturedPieces.add(capturedPiece);
                piecesOnTheBoard.remove(capturedPiece);
            }
        }
        return capturedPiece;
    }
    
    //The opposite logic of the method makeMove.
    private void undoMove(Position source, Position target, Piece capturedPiece){
        ChessPiece p = (ChessPiece) board.removePiece(target);
        p.decreaseMoveCount();
        
        board.placePiece(p, source);
        
        if(capturedPiece != null){
            board.placePiece(capturedPiece, target);
            capturedPieces.remove(capturedPiece);
            piecesOnTheBoard.add(capturedPiece);
        }
        
        //#Special move castling kingside rook --> UNDO MOVEMENT
        if(p instanceof King && target.getColumn() == source.getColumn() + 2){
            Position sourceT1 = new Position(source.getRow(), source.getColumn() + 3);
            Position targetT1 = new Position(source.getRow(), source.getColumn() + 1);
            
            ChessPiece rook = (ChessPiece)board.removePiece(targetT1);
            board.placePiece(rook, sourceT1);
            rook.decreaseMoveCount();
        }
        
        //#Special move castling queenside rook --> UNDO MOVEMENT
        if(p instanceof King && target.getColumn() == source.getColumn() - 2){
            Position sourceT2 = new Position(source.getRow(), source.getColumn() - 4);
            Position targetT2 = new Position(source.getRow(), source.getColumn() - 1);
            
            ChessPiece rook = (ChessPiece)board.removePiece(targetT2);
            board.placePiece(rook, sourceT2);
            rook.decreaseMoveCount();
        }
        
        //#Special move En passant -->  UNDO MOVEMENT
        if(p instanceof Pawn){
            //if the pawn moved diagonal but got any piece it means that he did an En Passant
            if(source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable){
                ChessPiece pawn = (ChessPiece)board.removePiece(target);
                Position pawnPosition;
                if(p.getColor() == Color.WHITE){
                    pawnPosition = new Position(3, target.getColumn());
                }
                else{
                    pawnPosition = new Position(4, target.getColumn());
                }
                board.placePiece(pawn, pawnPosition);
            }
        }
    }
    
    private void validateSourcePosition(Position position){
        if(!board.thereIsAPiece(position)){
            throw new ChessException("There is no piece on source position.");
        }
        if(currentPlayer != ((ChessPiece)board.piece(position)).getColor()){
            throw new ChessException("The chosen piece isn't yours.");
        }
        if(!board.piece(position).isThereAnyPossibleMove()){
            throw new ChessException("There is no possible move for the chosen piece.");
        }
    }
    
    private void validateTargetPosition(Position source, Position target){
        if(!board.piece(source).possibleMove(target)){
            throw new ChessException("The chosen piece can't move to target position.");
        }
    }
    
    private void nextTurn(){
        turn++;
        currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
    
    private Color opponent(Color color){
        //return the opposite color given by the parameter;
        return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
    
    //Locates the king from certain color.
    private ChessPiece king(Color color){
        List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
        
        for(Piece p : list){
            if(p instanceof King){
                return (ChessPiece) p;
            }
        }
        throw new IllegalStateException("There is no " + color + " king in the board.");
    }
    
    private boolean testCheck(Color color){
        Position kingPosition = king(color).getChessPosition().toPosition(); //gets the king position in matrix type
        List<Piece> opponentPieces = piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == opponent(color)).collect(Collectors.toList());
        
        for(Piece p : opponentPieces){
            /*
            By getting all possible moves of this piece "p", I can see if the king is in check 
            if the king's value in the matrix is equals to true.
            */
            boolean[][] mat = p.possibleMoves();
            
            if(mat[kingPosition.getRow()][kingPosition.getColumn()]){
                return true;
            }
        }
        return false;
    }
    
    private boolean testCheckMate(Color color){
        if(!testCheck(color)){
            return false;
        }
        //get all the pieces according to the given color in the parameter
        List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
        
        /*
        The main idea is: if there is a movement that the king can do in order to escape from check, it means that
        this isn't checkmate, but if there is no movement allowing that, so he got checkmated
        */
        for(Piece p : list){
            boolean[][] mat = p.possibleMoves();
           
            for(int i = 0; i < board.getRows(); i++){
                for(int j = 0; j < board.getColumns(); j++){
                    if(mat[i][j]){
                        Position source = ((ChessPiece)p).getChessPosition().toPosition();
                        Position target = new Position(i, j);
                        Piece capturedPiece = makeMove(source, target);
                        
                        boolean testCheck = testCheck(color);
                        
                        undoMove(source, target, capturedPiece);
                        
                        if(!testCheck){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }   
    
    private void placeNewPiece(char column, int row, ChessPiece piece){
        board.placePiece(piece, new ChessPosition(column, row).toPosition());
        
        //add all instantiated pieces to the list
        piecesOnTheBoard.add(piece);
    }
    
    private void initialSetup(){
        placeNewPiece('a', 1, new Rook(board, Color.WHITE));
        placeNewPiece('h', 1, new Rook(board, Color.WHITE));
        placeNewPiece('e', 1, new King(board, Color.WHITE, this));
        placeNewPiece('a', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('b', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('c', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('d', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('e', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('f', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('g', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('h', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('b', 1, new Knight(board, Color.WHITE));
        placeNewPiece('g', 1, new Knight(board, Color.WHITE));
        placeNewPiece('d', 1, new Queen(board, Color.WHITE));

        placeNewPiece('a', 8, new Rook(board, Color.BLACK));
        placeNewPiece('h', 8, new Rook(board, Color.BLACK));
        placeNewPiece('e', 8, new King(board, Color.BLACK, this));
        placeNewPiece('a', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('b', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('c', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('d', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('e', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('f', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('g', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('h', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
        placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
        placeNewPiece('b', 8, new Knight(board, Color.BLACK));
        placeNewPiece('g', 8, new Knight(board, Color.BLACK));
        placeNewPiece('d', 8, new Queen(board, Color.BLACK));
    }
}