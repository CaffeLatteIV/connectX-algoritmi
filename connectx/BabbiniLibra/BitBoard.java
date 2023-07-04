package connectx.BabbiniLibra;

import java.util.BitSet;

public class BitBoard {
/**
	 * Board rows
	 */
	public final int M;

	/**
	 * Board columns
	 */
	public final int N;

	/**
	 * Number of symbols to be aligned (horizontally, vertically, diagonally) for a  win
	 */
	public final int X;
  BitSet CURRENTPLAYER;
  BitSet[] MASK;

  public BitBoard(int M, int N, int X){
    this.M=M;
    this.N=N;
    this.X=X;
    CURRENTPLAYER = new BitSet(N*(M+1));
    MASK = new BitSet[M];
  }

}
