package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import main.Piece;

/**
 * @author Maxim
 *
 */
class PieceTest {
	void test() {
		Piece piece = new Piece(0, "somehashofpiece");
		Piece piece1 = new Piece(1, "somehashofpiece");
		Piece piece2 = new Piece(2, "somehashofpiece");

		System.out.println(piece.equals(piece));
//		assertEquals("equals", true, piece.equals(piece));
		
		System.out.println(piece.equals(piece1));
//		assertEquals("equals", false, piece.equals(piece1));
				
		// returns == 0 then the two strings are lexicographically equivalent
		System.out.println("Compare piece to piece is : " + piece.compareTo(piece));
//		assertEquals("comparable", 0, piece.compareTo(piece));
		
		// returns > 0 then the parameter passed to the compareTo method is lexicographically first.
		System.out.println("Compare piece1 to piece is : " + piece1.compareTo(piece));
//		assertEquals("comparable", -1, piece.compareTo(piece1));
		
		// returns < 0 then the String calling the method is lexicographically first
		System.out.println("Compare piece to piece1 is : " + piece.compareTo(piece1));
//		assertEquals("comparable", 1, piece1.compareTo(piece));
		
		ArrayList<Piece> pieces = new ArrayList<>();
		pieces.add(piece);
		pieces.add(piece2);
		pieces.add(piece1);
		
		Collections.sort(pieces);
		for (Piece temp : pieces) {
			System.out.println(temp.getIndex());
		}
		
		Piece[] pieceArray = new Piece[3];
		pieceArray[0] = piece;
		pieceArray[1] = piece2;
		pieceArray[2] = piece1;
		
		Arrays.sort(pieceArray);
		
		for (Piece temp2 : pieceArray) {
			System.out.println(temp2.getIndex());
		}
	}
}
