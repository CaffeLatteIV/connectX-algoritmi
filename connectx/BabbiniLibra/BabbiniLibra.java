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
import connectx.CXCell;
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
  int countPass;

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
    countPass = 0;

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
    int i = 0;
    while (B.fullColumn(L[i])) {
      i++;
    }
    BESTMOVETMP = L[i];

    try {
      int move = chooseMove(B, L);
      B.markColumn(move);
      return move;
    } catch (TimeoutException e) {
      System.out.println("timeout. moves: " + (B.N * B.M - 1 - B.numOfMarkedCells()) / 2);
      return BESTMOVETMP;
    }

  }

  private Integer chooseMove(CXBoard B, Integer[] L) throws TimeoutException {
    int bestScore = -B.N * B.M;
    int beta = (B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
    int alpha = -(B.N * B.M - 1 - B.numOfMarkedCells()) / 2;
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
      int score = -negamax(B, -beta, -alpha, 10);
      B.unmarkColumn();
      if (score > bestScore) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
      if (score >= beta) {
        return i;
      }
      if (score > alpha) {
        alpha = score;
      }
      // System.out.println("Column " + i + " Score " + score);
    }
    // System.out.println("Best column " + move + " Best score " + bestScore);
    // System.out.println();
    countPass++;
    return move;
  }

  private int negamax(CXBoard B, int alpha, int beta, int depth) throws TimeoutException {
    CXGameState state = B.gameState();

    if (state == CXGameState.DRAW) { // check for draw game
      return 0;
    }
    for (int x : columnOrder) { // check if current player can win next move
      checkTime();
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

    


    if (depth <= 0) {
      return evaluation(B, B.getLastMove(), depth) ;
    }



    for (int x = 0; x < B.N; x++) { // compute the score of all possible next move and keep the best one
      checkTime();
      if (!B.fullColumn(x)) {
        B.markColumn(x);
        int score = -negamax(B, -beta, -alpha, depth-1); // If current player plays col x, his score will be the opposite of the
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

  private int evaluateCounts(int countPlayer, int countOpponent) {
    if (countPlayer > 0 && countOpponent == 0) {
      // Player has a potential winning position
      return (int) Math.pow(10, countPlayer);
    } else if (countPlayer == 0 && countOpponent > 0) {
      // Opponent has a potential winning position
      return -(int) Math.pow(10, countOpponent);
    }
    return 0;
  }

  private int evaluation(CXBoard B, CXCell lastMove, int depth) throws TimeoutException {
    checkTime();
    // if (depth < (B.X / 2)) {
    // // valutazione con valore della cella: O(1) ma euristica molto debole. Usata
    // per
    // // non appesantire troppo alphabeta da subito
    // for (int i = 0; i < B.M; i++) { // scorro le righe della colonna per trovare
    // la prima libera, poi ritorno il
    // // valore
    // int col = lastMove.j;
    // if (B.cellState(i, col) == CXCellState.FREE) {
    // return CELLWEIGHT[i][col];
    // }
    // }
    // return 0;
    // } else {
    // valutazione con celle vicine: O(M*N(*K)) ma euristica forte.
    int score = 0;
    int rows = B.M;
    int columns = B.N;
    CXCellState player = (B.currentPlayer() == 0) ? CXCellState.P1 : CXCellState.P2;
    CXCellState[][] board = B.getBoard();

    // System.out.println("moves: " + depth);
    checkTime();
    // Evaluate rows
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column <= columns - B.X; column++) {
        int countPlayer = 0;
        int countOpponent = 0;

        for (int k = 0; k < B.X; k++) {
          CXCellState cell = board[row][column + k];
          if (cell == player) {
            countPlayer++;
          } else if (cell != CXCellState.FREE) {
            countOpponent++;
          }
        }
        score += evaluateCounts(countPlayer, countOpponent);
      }
    }
    checkTime();
    // Evaluate columns
    for (int column = 0; column < columns; column++) {
      for (int row = 0; row <= rows - B.X; row++) {
        int countPlayer = 0;
        int countOpponent = 0;

        for (int k = 0; k < B.X; k++) {
          CXCellState cell = board[row + k][column];
          if (cell == player) {
            countPlayer++;
          } else if (cell != CXCellState.FREE) {
            countOpponent++;
          }
        }
        score += evaluateCounts(countPlayer, countOpponent);
      }
    }

    checkTime();
    // Evaluate diagonals (upward)
    for (int row = 0; row <= rows - B.X; row++) {
      for (int column = 0; column <= columns - B.X; column++) {
        int countPlayer = 0;
        int countOpponent = 0;

        for (int k = 0; k < B.X; k++) {
          CXCellState cell = board[row + k][column + k];
          ;
          if (cell == player) {
            countPlayer++;
          } else if (cell != CXCellState.FREE) {
            countOpponent++;
          }
        }
        score += evaluateCounts(countPlayer, countOpponent);
      }
    }

    checkTime();
    // Evaluate diagonals (downward)
    for (int row = B.X - 1; row < rows; row++) {
      for (int column = 0; column <= columns - B.X; column++) {
        int countPlayer = 0;
        int countOpponent = 0;

        for (int k = 0; k < B.X; k++) {
          CXCellState cell = board[row - k][column + k];
          if (cell == player) {
            countPlayer++;
          } else if (cell != CXCellState.FREE) {
            countOpponent++;
          }
        }
        score += evaluateCounts(countPlayer, countOpponent);
      }
    }
    if (score > BESTMOVETMP) {
    }
    return score;
  }
  // }

  public String playerName() {
    return "Babbini-Libra";
  }
}
