package connectx.BabbiniLibra;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import javax.swing.text.Position;

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
      return move;
    } catch (TimeoutException e) {
      return BESTMOVETMP;
    }

  }

  private Integer chooseMove(CXBoard B, Integer[] L) throws TimeoutException {
    int bestScore = -B.N * B.M;
    int move = L[0];
    for (int i : L) {
      // checkTime();
      CXGameState state = B.markColumn(i);
      if (state == myWin) {
        B.unmarkColumn();
      return move;
    }
      int score = -negamax(B);
      B.unmarkColumn();
      if (score > bestScore) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
    }
    return move;
  }

  private int negamax(CXBoard B) {
    CXGameState state = B.gameState();
    if (state == CXGameState.DRAW) { // check for draw game
      return 0;
    } 
    for (int x = 0; x < B.N; x++) { // check if current player can win next move
      if (!B.fullColumn(x)) {
        CXGameState move = B.markColumn(x);
        B.unmarkColumn();
        if (move == CXGameState.WINP1 || move == CXGameState.WINP2) {
          return (B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
        }
      }
    }
    // int max = (B.N * B.M - 1 - B.numOfMarkedCells()) / 2; // upper bound of our score as we cannot win immediately
    // if (beta > max) {
    //   beta = max; // there is no need to keep beta above our max possible score.
    //   if (alpha >= beta) {
    //     return beta; // prune the exploration if the [alpha;beta] window is empty.
    //   }
    // }
    int bestScore = -B.N*B.M;
    for (int x = 0; x < B.N; x++) { // compute the score of all possible next move and keep the best one
      if (!B.fullColumn(x)) {
        B.markColumn(x);
        int score = -negamax(B); // If current player plays col x, his score will be the opposite of the other player 
        B.unmarkColumn();
        if (score > bestScore) {
          bestScore = score; // prune the exploration if we find a possible move better than what we were
                        // looking for.
        }
      }
    }
    return bestScore;
  }

}
