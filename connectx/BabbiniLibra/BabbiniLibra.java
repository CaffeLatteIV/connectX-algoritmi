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
  private int MINSCORE;

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
    transpositionTable = new HashMap<>(N * M);
    CELLWEIGHT = new Integer[M][N];
    MINSCORE = -(N * M) / 2 + 3;

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
    // inizializzo columnOrder
    for (int i = 0; i < N; i++) {
      columnOrder[i] = N / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2;
    }
  }

  private void checkTime() throws TimeoutException {
    long time = System.currentTimeMillis() - START;
    if ((time / 1000) >= TIMEOUT * (90.0 / 100.0)) {

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
    int bestScore = -B.N * B.M - 1;
    int move = L[0];
    for (int i : columnOrder) {
      checkTime();
      if (B.fullColumn(i)) {
        continue;
      }
      // checkTime();
      CXGameState state = B.markColumn(i);
      if (state == myWin) {
        B.unmarkColumn();
        return i;
      }
      int score = -nullWindow(B);
      B.unmarkColumn();
      if (score > bestScore) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
      System.out.println("Column " + i + " Score " + score);
    }
    System.out.println("Best column " + move + " Best score " + bestScore);
    System.out.println();
    return move;
  }

  private int negamax(CXBoard B, int alpha, int beta) {
    CXGameState state = B.gameState();
    if (state == CXGameState.DRAW) { // check for draw game
      return 0;
    }
    for (int x=0; x < B.N; x++) {  // check if current player can win next move
      if (!B.fullColumn(x)) {
        CXGameState move = B.markColumn(x);
        B.unmarkColumn();
        if (move == myWin || move == yourWin) {
          return (B.N * B.M + 1 - B.numOfMarkedCells()) / 2;
        }
      }
    }
    int boardHashCode = Arrays.deepHashCode(B.getBoard());
    int max = (B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
    if (transpositionTable.containsKey(boardHashCode)) {
      int val = transpositionTable.get(boardHashCode);
      max = val + MINSCORE -1 ;
      // max = val;
    }
    if (beta > max) {
      beta = max;
      if (alpha >= beta) {
        return beta;
      }
    }
    for (int x : columnOrder) { // compute the score of all possible next move and keep the best one
      if (!B.fullColumn(x)) {
        B.markColumn(x);
        int score = -negamax(B, -beta, -alpha); // If current player plays col x, his score will be the opposite of the
                                                // other player
        B.unmarkColumn();
        if (score >= beta) {
          return score;
        }
        if (score > alpha) {
          alpha = score;
        }
      }
    }
    // transpositionTable.put(boardHashCode, alpha);
    transpositionTable.put(boardHashCode, alpha - MINSCORE + 1);
    return alpha;
  }

  public int nullWindow(CXBoard B) {
    int min = -(B.N * B.M - B.numOfMarkedCells()) / 2;
    int max = (B.N * B.M + 1 - B.numOfMarkedCells()) / 2;
    while (min < max) { // iteratively narrow the min-max exploration window
      int med = min + (max - min) / 2;
      if (med <= 0 && min / 2 < med)
        med = min / 2;
      else if (med >= 0 && max / 2 > med)
        med = max / 2;
      int r = negamax(B, med, med + 1); // use a null depth window to know if the
      // actual score is greater or smaller
      // than med
      if (r <= med) {
        max = r;
      } else {
        min = r;
      }
    }
    return min;
  }

  public String playerName() {
    return "Babbini-Libra";
  }
}
