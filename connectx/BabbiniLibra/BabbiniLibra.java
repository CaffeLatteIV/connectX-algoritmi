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
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.beans.beancontext.BeanContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Time;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

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
    if (((System.currentTimeMillis() - START) / 1000) >= TIMEOUT * (98.5 / 100.0)) {
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
    Integer[] L = B.getAvailableColumns();

    START = System.currentTimeMillis(); // Save starting time
    BESTMOVETMP = L[0];

    try {
      int move = chooseMove(B, L);
      TOTALMOVES++;
      TOTALTIME += System.currentTimeMillis() - START;
      B.markColumn(move);
      return move;
    } catch (TimeoutException e) {
      TOTALMOVES++;
      TOTALTIME += System.currentTimeMillis() - START;
      // System.err.println("timeout!!");
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
        score = 1000000; // one million
      } else if (result == CXGameState.DRAW) {
        score = 0;
      } else {
        score = abprouning(B, B.getAvailableColumns(), false, alpha, beta);
      }
      // System.out.println("evaluating column "+i);
      // System.out.println("score "+score);
      if (bestScore < score) {
        bestScore = score;
        move = i;
        BESTMOVETMP = i;
      }
      B.unmarkColumn();
    }
    // System.out.println("I choose column "+move);
    // System.out.println("score "+bestScore);
    // System.out.println();
    return move;
  }

  private int abprouning(CXBoard B, Integer[] L, boolean maximizer, int alpha, int beta) throws TimeoutException {
    checkTime();
    if (maximizer) {
      int maxScore = -1000000;
      int hash = B.getBoard().hashCode();
      if (transpositionTable.containsKey(hash)) {
        // System.out.println("transposition found");
        return transpositionTable.get(hash);
      }
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
        } else { // OPEN Board
          int cellWeight = (B.M + B.N) - (i + (1 - (i % 2))); // M+N valore di partenza, valore della cella
                                                                  // direttamente proporzionale alla priorità della
                                                                  // colonna. Non funziona se seguiamo ordine delle
                                                                  // colonne standard.
          score =  this.abprouning(B, B.getAvailableColumns(), false, alpha, beta);
        }
        maxScore = Math.max(maxScore, score);
        alpha = Math.max(alpha, maxScore);
        B.unmarkColumn();
        if (beta<=alpha) {
          break;
        }
      }
      transpositionTable.put(hash, maxScore);
      return maxScore;
    } else {
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

          score =  this.abprouning(B, B.getAvailableColumns(), true, alpha, beta);
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

  public String playerName() {
    return "Babbini-Libra";
  }
}
