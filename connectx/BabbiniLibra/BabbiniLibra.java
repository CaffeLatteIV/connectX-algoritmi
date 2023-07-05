/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx.BabbiniLibra;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

import javax.swing.text.Position;

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class BabbiniLibra implements CXPlayer {
  private Random rand;
  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;
  private long TOTALTIME;
  private int TOTALMOVES;
  private LinkedList<Integer> columnOrder;
  private int BESTMOVETMP;
  private HashMap<Integer, Integer> transpositionTable;

  /* Default empty constructor */
  public BabbiniLibra() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    // New random seed for each game
    rand = new Random(System.currentTimeMillis());
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;
    TOTALMOVES = 0;
    TOTALTIME = 0;
    columnOrder = new LinkedList<Integer>();
    transpositionTable = new HashMap<>();
    for (int i = 0; i < N; i++) {
      columnOrder.addLast(N / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2); // inizializza l'ordine delle colonne partendo dal
    }
  }

  private void checkTime() throws TimeoutException {
    long time = System.currentTimeMillis() - START;
    if ((time / 1000) >= TIMEOUT * (90.0 / 100.0)) {

      System.out.println("time: " + time);
      throw new TimeoutException();
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
    Integer[] L = sortl(B, B.getAvailableColumns());
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
    int bestScore = -1_000_000; // one million
    int alpha = -1_000_000;
    int beta = 1_000_000;
    int move = L[0];
    for (int i : L) {
      checkTime();
      int score;
      CXGameState result = B.markColumn(i);
      if (result == myWin) {
        score = 1_000_000; // one million
      } else if (result == CXGameState.DRAW) {
        score = 0;
      } else {
        score = abprouning(B, B.getAvailableColumns(), false, alpha, beta);
      }
      if (bestScore < score) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
      B.unmarkColumn();
    }
    return move;
  }

  private Integer[] sortl(CXBoard B, Integer[] L) {
    Arrays.sort(L, new Comparator<Integer>() {
      @Override
      public int compare(Integer a, Integer b) {
        int m = ((int) Math.floor(B.N / 2));
        return Math.abs(m - a) - Math.abs(m - b);
      }
    });
    return L;
  }

  private int abprouning(CXBoard B, Integer[] L, boolean maximizer, int alpha, int beta) throws TimeoutException {
    checkTime();
    L = sortl(B, L);
    if (maximizer) {
      checkTime();
      int maxScore = -1_000_000;
      int hash = B.getBoard().hashCode();
      if (transpositionTable.containsKey(hash)) { // se mossa già nella transpositionTable la guardo da qui, altrimenti
                                                  // eseguo l'alphabeta
        return transpositionTable.get(hash);
      }
      for (int i : L) {
        checkTime();
        int score;
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          score = 1_000_000;
        } else if (result == yourWin) {
          score = -1_000_000;
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else { // OPEN Board
          int cellWeight = (B.M + B.N) - (i + (1 - (i % 2))); // M+N valore di partenza, valore della cella
                                                              // direttamente proporzionale alla priorità della
                                                              // colonna. Non funziona se seguiamo ordine delle
                                                              // colonne standard.
          score = this.abprouning(B, B.getAvailableColumns(), false, alpha, beta);
        }
        maxScore = Math.max(maxScore, score);
        alpha = Math.max(alpha, maxScore);
        B.unmarkColumn();
        if (beta <= alpha) {
          break;
        }
      }
      transpositionTable.put(hash, maxScore);
      return maxScore;
    } else {
      checkTime();
      // minimizer
      int minScore = 1000000;
      for (int i : L) {
        checkTime();
        int score;
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          score = 1000000;
        } else if (result == yourWin) {
          score = -1000000;
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else {
          int cellWeight = (B.M + B.N) - (i + (1 - (i % 2))); // M+N valore di partenza, valore della cella
                                                              // direttamente proporzionale alla priorità della
                                                              // colonna. Non funziona se seguiamo ordine delle
                                                              // colonne standard.

          score = this.abprouning(B, B.getAvailableColumns(), true, alpha, beta);
        }
        minScore = Math.min(minScore, score);
        beta = Math.min(score, beta);
        B.unmarkColumn();
        if (beta <= alpha) {
          break;
        }
      }
      return minScore;
    }
  }

  private int negamax(CXBoard B) {
    if (B.numOfMarkedCells() == B.M * B.N) { // check for draw game
      return 0;
    }
    for (int x = 0; x < B.N; x++) // check if current player can win next move
      if (!B.fullColumn(x)) {
        CXGameState move = B.markColumn(x);
        if (move == CXGameState.WINP1 || move == CXGameState.WINP2) {
          B.unmarkColumn();
          return (B.N * B.M + 1 - B.numOfMarkedCells()) / 2;
        }
      }

    int bestScore = -B.N * B.M; // init the best possible score with a lower bound of score.

    for (int x = 0; x < B.N; x++) { // compute the score of all possible next move and keep the best one
      if (!B.fullColumn(x)) {
        B.markColumn(x);
        int score = -negamax(B); // If current player plays col x, his score will be the opposite of opponent's
                                 // score after playing col x
        if (score > bestScore)
          bestScore = score; // keep track of best possible score so far.
      }
      B.unmarkColumn();
    }
    return bestScore;
  }

  // public int nullWindow() {
  //   int min = -(Position::WIDTH * Position::HEIGHT - P.nbMoves()) / 2;
  //   int max = (Position::WIDTH * Position::HEIGHT + 1 - P.nbMoves()) / 2;
  //   if (weak) {
  //     min = -1;
  //     max = 1;
  //   }
  //   while (min < max) { // iteratively narrow the min-max exploration window
  //     int med = min + (max - min) / 2;
  //     if (med <= 0 && min / 2 < med)
  //       med = min / 2;
  //     else if (med >= 0 && max / 2 > med)
  //       med = max / 2;
  //     int r = negamax(P, med, med + 1); // use a null depth window to know if the actual score is greater or smaller
  //                                       // than med
  //     if (r <= med)
  //       max = r;
  //     else
  //       min = r;
  //   }
  //   return min;
  // }

  public String playerName() {
    return "Babbini-Libra";
  }
}
