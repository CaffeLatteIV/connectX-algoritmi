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
  private Integer[] columnOrder;
  private int BESTMOVETMP;
  private HashMap<Integer, Integer> transpositionTable;
  int desiredDepth;

  /* Default empty constructor */
  public BabbiniLibra() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    // New random seed for each game
    rand = new Random(System.currentTimeMillis());
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;
    columnOrder = new Integer[N];
    transpositionTable = new HashMap<>();

    // inizializzo desiredDepth
    int dim = Math.max(M, N);
    if (dim == 4) {
      desiredDepth = 30;
    } else if (dim <= 7) {
      desiredDepth = 12;
    } else if (dim > 7 && dim <= 10) {
      desiredDepth = 8;
    } else if (dim > 10 && dim <= 13) {
      desiredDepth = 6;
    } else if (dim > 13 && dim <= 15) {
      desiredDepth = 5;
    } else if (dim > 15 && dim <= 20) {
      desiredDepth = 4;
    } else if (dim > 20 && dim <= 30) {
      desiredDepth = 3;
    } else if (dim > 30) {
      desiredDepth = 2;
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
    int bestScore = -1_000_000_000;
    int alpha = -1_000_000_000;
    int beta = 1_000_000_000;
    int move =0;
     while (B.fullColumn(L[move])) {
      move++;
    }
    move = L[move];
    for (int i : L) {
      checkTime();
      if (B.fullColumn(i)) {
        System.out.println(i + " full");
        continue;
      }
      // checkTime();
      CXGameState state = B.markColumn(i);
      if (state == myWin) {
        B.unmarkColumn();
        return i;
      }
      int score = abprouning(B, false, alpha, beta, desiredDepth);
      B.unmarkColumn();
      if (score > bestScore) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
      alpha = Math.max(alpha, bestScore);
      if (beta <= alpha) {
        break;
      }
      System.out.println("Move " + i + " score " + score);
    }
    System.out.println("best move " + move + " score " + bestScore);
    System.out.println();
    return move;
  }

  private int abprouning(CXBoard B, boolean isMaximizer, int alpha, int beta, int depth) throws TimeoutException {
    checkTime();
    if (depth <= 0) {
    return evaluation(B, B.getLastMove(), depth); // heuristic evaluation of the
    // open board
    }
    if (isMaximizer) {
      int bestScore = -1_000_000_000;

      int hashBoard = Arrays.deepHashCode(B.getBoard());
      if (transpositionTable.containsKey(hashBoard)) {
        bestScore = transpositionTable.get(hashBoard);
      }
      for (int x : columnOrder) {
        if (B.fullColumn(x)) {
          continue;
        }
        int score;
        CXGameState status = B.markColumn(x);
        if (status == myWin) {
          score = 1_000_000_000;
        } else if (status == CXGameState.DRAW) {
          score = 0;
        } else {
          score = abprouning(B, false, alpha, beta, depth - 1);
        }
        B.unmarkColumn();
        bestScore = Math.max(score, bestScore);
        alpha = Math.max(alpha, bestScore);
        // in questo ramo viene scelta la mossa con punteggio più alto
        // quindi se nel turno prima (quello del minimizer)
        // il valore minimo già trovato (beta) è inferiore ad alpha appena calcolato
        // posso subito scartare gli altri rami a questo livello
        if (beta <= alpha) {
          break;
        }
      }
      transpositionTable.put(hashBoard, bestScore);
      return bestScore;
    } else { // minimizer
      int bestScore = 1_000_000_000;
      for (int x : columnOrder) {
        if (B.fullColumn(x)) {
          continue;
        }
        int score;
        CXGameState status = B.markColumn(x);
        if (status == yourWin) {
          score = -1_000_000_000;
        } else if (status == CXGameState.DRAW) {
          score = 0;
        } else {
          score = abprouning(B, true, alpha, beta, depth - 1);
        }
        B.unmarkColumn();
        bestScore = Math.min(score, bestScore);
        beta = Math.min(beta, bestScore);
        // in questo ramo viene scelta la mossa con punteggio più basso
        // quindi se nel turno prima (quello del maximizer)
        // il valore massimo già trovato (alpha) è maggiore di beta appena calcolato
        // posso subito scartare gli altri rami a questo livello
        if (beta <= alpha) {
          break;
        }
      }
      return bestScore;
    }

  }

  private int evaluateCounts(int countPlayer, int countOpponent) {
    if (countPlayer > 0 && countOpponent == 0) {
      // Winning potential
      return (int) Math.pow(10, countPlayer);
    } else if (countPlayer == 0 && countOpponent > 0) {
      // Losing potential
      return -(int) Math.pow(10, countOpponent);
    } else if (countPlayer > 0 && countOpponent > 0) {
      // Both have potential, check who has the better potential
      return (int) Math.pow(10, countPlayer) - (int) Math.pow(10, countOpponent);
    } else {
      // No one can win here, check positional advantage
      int score = 0;
      if (countPlayer > 0) {
        score += Math.pow(2, countPlayer - 1);
      }
      if (countOpponent > 0) {
        score -= Math.pow(2, countOpponent - 1);
      }

      return score;
    }
  }

  private int evaluation(CXBoard B, CXCell lastMove, int depth) throws TimeoutException {
    int score = 0;
    int rows = B.M;
    int columns = B.N;
    CXCellState player = (B.currentPlayer() == 0) ? CXCellState.P1 : CXCellState.P2;
    CXCellState[][] board = B.getBoard();

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
    return score;
  }

  public String playerName() {
    return "Babbini-Libra";
  }
}
