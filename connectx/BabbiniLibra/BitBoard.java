package connectx.BabbiniLibra;

import java.util.BitSet;

public class BitBoard {
  /**
   * Board rows
   */
  private int HEIGHT;

  /**
   * Board columns
   */
  private int WIDTH;

  /**
   * Number of symbols to be aligned (horizontally, vertically, diagonally) for a
   * win
   */
  public final int X;
  BitSet[] CURRENTPLAYER;
  BitSet[] MASK;
  BitSet MASKK;

  public BitBoard(int M, int N, int X) {
    this.HEIGHT = M;
    this.WIDTH = N;
    this.X = X;
    CURRENTPLAYER = new BitSet[HEIGHT];
    MASK = new BitSet[HEIGHT];
    // inizializza la bitboard
    for (int i = 0; i < N; i++) {
      CURRENTPLAYER[i] = new BitSet(WIDTH + 1);
      MASK[i] = new BitSet(WIDTH + 1);
    }
  }

  boolean canPlay(int col){
  return (MASKK.and(top_mask(col)) == 0;
}

BitSet top_mask(int col) {
  return (new BitSet(1). << (HEIGHT - 1)) << col*(HEIGHT+1);
}

}
