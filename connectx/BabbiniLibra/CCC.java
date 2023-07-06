package connectx.BabbiniLibra;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

public class CCC implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;
  private long TOTALTIME;
  private int TOTALMOVES;
  private int BESTMOVETMP;
  private Integer[] columnOrder;
  private HashMap<Integer, Integer> transpositionTable;

  public String playerName() {
    return "Niggamax";
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    // New random seed for each game
    rand = new Random(System.currentTimeMillis());
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;
    TOTALMOVES = 0;
    TOTALTIME = 0;
    columnOrder = new Integer[N];
    transpositionTable = new HashMap<>();
    for (int i = 0; i < N; i++) {
      columnOrder[i] = N / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2; // inizializza l'ordine delle colonne partendo dal
                                                                // centro
    }
  }

  /**
   * Selects a free colum on game board.
   * <p>
   * Selects a winning column (if any), otherwise selects a column (if any)
   * that prevents the adversary to win with his next move. If both previous
   * cases do not apply, selects a random column.
   * </p>
   */
  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis(); // Save starting time
    Integer[] L = B.getAvailableColumns();
    BESTMOVETMP = L[0];

    try {
      int move = chooseMove(B, L);
      B.markColumn(move);
      // if(TOTALMOVES ==0){
      //   TOTALTIME = System.currentTimeMillis() - START;
      //   System.out.println(TOTALTIME);
        
      // }
      TOTALMOVES++;
      return move;
    } catch (TimeoutException e) {
      return BESTMOVETMP;
    }

  }

  private Integer chooseMove(CXBoard B, Integer[] L) throws TimeoutException {
    int bestScore = -B.N * B.M;
    int beta = (B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
    int alpha = -(B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
    int move = L[0];
    for (int i : columnOrder) {
      if (B.fullColumn(i)) {
        continue;
      }
      // checkTime();
      CXGameState state = B.markColumn(i);
      if (state == myWin) {
        B.unmarkColumn();
        return i;
      }
      int score = -negamax(B, -beta, -alpha);
      B.unmarkColumn();
      if (score > bestScore) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }

      // System.out.println("Column " + i + " Score " + score);
    }
    // System.out.println("Best column " + move + " Best score " + bestScore);
    // System.out.println();
    return move;
  }

  private int negamax(CXBoard B, int alpha, int beta) {
    CXGameState state = B.gameState();
    if (state == CXGameState.DRAW) { // check for draw game
      return 0;
    }
    for (int x : columnOrder) { // check if current player can win next move
      if (!B.fullColumn(x)) {
        CXGameState move = B.markColumn(x);
        B.unmarkColumn();
        if (move == myWin || move == yourWin) {
          return (B.N * B.M + 1 - B.numOfMarkedCells()) / 2;
        }
      }
    }
    int max = (B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
    if (beta > max) {
      beta = max;
      if (alpha >= beta) {
        return beta;
      }
    }
    for (int x = 0; x < B.N; x++) { // compute the score of all possible next move and keep the best one
      if (!B.fullColumn(x)) {
        B.markColumn(x);
        int score = -negamax(B, -beta, -alpha); // If current player plays col x, his score will be the opposite of the
                                                // other
        // player
        B.unmarkColumn();
        if (score >= beta) {
          return score;
        }
        if (score > alpha) {
          alpha = score;
        }
      }
    }
    return alpha;
  }
}
