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
import connectx.CXCellState;
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
  private Integer[] columnOrder;
  private int BESTMOVETMP;
  private int DEPTH;
  private HashMap<Integer, Integer> transpositionTable;
  private Integer[][] CELLWEIGHT;

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
    DEPTH = 0;
    TOTALTIME = 0;
    columnOrder = new Integer[N];
    transpositionTable = new HashMap<>();
    CELLWEIGHT = new Integer[M][N];

    // inizializzo CELLWEIGHT con valori più alti quanto più si è al centro
    int centerR = N / 2;
    int centerC = M / 2;

    for (int i = 0; i < M; i++) {
      for (int j = 0; j < N; j++) {
        int distToCenterR = (int) Math.pow(Math.abs(j - centerR) + 2, 2);
        int distToCenterC = (int) Math.pow(Math.abs(i - centerC) + 2, 2);
        CELLWEIGHT[i][j] = Math.max(((M * N) - (distToCenterC + distToCenterR)), 1); // non scendo sotto 1 perchè
                                                                                     // giocare una mossa è meglio del
                                                                                     // pareggio (0) o della sconfitta
                                                                                     // (valori negativi)
      }
    }
    for (int i = 0; i < M; i++) {
      for (int j = 0; j < N; j++) {
        System.out.print(CELLWEIGHT[i][j] + "\t");
      }
      System.out.println();
    }
    // inizializzo columnOrder
    for (int i = 0; i < N; i++) {
      columnOrder[i] = N / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2;
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
    DEPTH = 0;
    Integer[] L = columnOrder;
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
    for (int i : columnOrder) {
      if (B.fullColumn(i)) {
        continue;
      }
      checkTime();
      int score;
      CXGameState result = B.markColumn(i);
      if (result == myWin) {
        score = (B.M * B.N - 1 - DEPTH / 2);
      } else if (result == CXGameState.DRAW) {
        score = 0;
      } else {
        score = abprouningCount(B, false, alpha, beta);
      }
      alpha = Math.max(alpha, score);
      if (bestScore < score) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
      B.unmarkColumn();
      if (beta <= alpha) {
        break;
      }
    }
    return move;
  }

  private int abprouning(CXBoard B, boolean maximizer, int alpha, int beta) throws TimeoutException {
    checkTime();
    if (maximizer) {
      checkTime();
      int maxScore = -1_000_000;
      int hash = B.getBoard().hashCode();
      if (transpositionTable.containsKey(hash)) { // se mossa già nella transpositionTable la guardo da qui, altrimenti
                                                  // eseguo l'alphabeta
        return transpositionTable.get(hash);
      }
      for (int i : columnOrder) {
        checkTime();
        if (B.fullColumn(i)) {
          continue;
        }
        int score;
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          score = 1_000_000;
        } else if (result == yourWin) {
          score = -1_000_000;
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else { // OPEN Board
          score = this.abprouning(B, false, alpha, beta);
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
      int minScore = B.N * B.M;
      for (int i : columnOrder) {
        checkTime();
        if (B.fullColumn(i)) {
          continue;
        }
        int score;
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          score = 1_000_000;
        } else if (result == yourWin) {
          score = -1_000_000;
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else {
          score = this.abprouning(B, true, alpha, beta);
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

  private int abprouningCount(CXBoard B, boolean maximizer, int alpha, int beta) throws TimeoutException {
    if (maximizer) {
      checkTime();
      int maxScore = -B.N * B.M;
      int hash = B.getBoard().hashCode();
      if (transpositionTable.containsKey(hash)) { // se mossa già nella transpositionTable la guardo da qui, altrimenti
                                                  // eseguo l'alphabeta
        return transpositionTable.get(hash);
      }
      for (int i : columnOrder) {
        checkTime();
        if (B.fullColumn(i)) {
          continue;
        }
        int score;
        CXGameState result = B.markColumn(i);
        DEPTH++;
        if (result == myWin) {
          score = (B.M * B.N - 1 - DEPTH / 2);
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else { // OPEN Board
          score = this.abprouning(B, false, alpha, beta);
        }
        if (score > 0) {
          maxScore = Math.min(maxScore, score);
        } else {
          maxScore = Math.max(maxScore, score);
        }
        alpha = Math.max(alpha, maxScore);
        DEPTH--;
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
      int minScore = B.N * B.M;
      for (int i : columnOrder) {
        checkTime();
        if (B.fullColumn(i)) {
          continue;
        }
        int score;
        CXGameState result = B.markColumn(i);
        if (result == yourWin) {
          score = -(B.M * B.N - 1 - DEPTH / 2);
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else {
          score = this.abprouning(B, true, alpha, beta);
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

  // private int evaluation(CXBoard B, int col) {
  //   if (DEPTH < (B.M - B.X)) {
  //     // valutazione con valore della cella: O(1) ma euristica molto debole. Usata per
  //     // non appesantire troppo alphabeta da subito
  //     CXCellState[][] board = B.getBoard();
  //     int max = 0;
  //     for (int i = 0; i < B.M; i++) { // scorro le righe della colonna per trovare la prima libera, poi ritorno il
  //                                     // valore
  //       if (B.cellState(i, col) == CXCellState.FREE) {
  //         return CELLWEIGHT[i][col];
  //       }
  //     }
  //   } else {
  //     // valutazione con celle vicine: O(M*N(*K)) ma euristica forte.
  //   }
  // }

  // public int nullWindow() {
  // int min = -(Position::WIDTH * Position::HEIGHT - P.nbMoves()) / 2;
  // int max = (Position::WIDTH * Position::HEIGHT + 1 - P.nbMoves()) / 2;
  // if (weak) {
  // min = -1;
  // max = 1;
  // }
  // while (min < max) { // iteratively narrow the min-max exploration window
  // int med = min + (max - min) / 2;
  // if (med <= 0 && min / 2 < med)
  // med = min / 2;
  // else if (med >= 0 && max / 2 > med)
  // med = max / 2;
  // int r = negamax(P, med, med + 1); // use a null depth window to know if the
  // actual score is greater or smaller
  // // than med
  // if (r <= med)
  // max = r;
  // else
  // min = r;
  // }
  // return min;
  // }

  public String playerName() {
    return "Babbini-Libra";
  }
}
