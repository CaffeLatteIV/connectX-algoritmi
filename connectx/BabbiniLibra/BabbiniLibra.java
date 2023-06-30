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
import java.util.Arrays;
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

  /* Default empty constructor */
  public BabbiniLibra() {
  }

  public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
    // New random seed for each game
    rand = new Random(System.currentTimeMillis());
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
    TIMEOUT = timeout_in_secs;
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
    int save = L[rand.nextInt(L.length)]; // Save a random column

    // try {
    // int col = singleMoveWin(B, L);
    // if (col != -1)
    // return col;
    // else
    // return singleMoveBlock(B, L);
    int bestScore = -1000;
    int move = L[0];
    for (int i : L) {
      int score;
      CXGameState result = B.markColumn(i);
      if (result == myWin) {
        return i;
      } else if (result == CXGameState.DRAW) {
        score = 0;
      } else {
        score = minmax(B, B.getAvailableColumns(), false);
      }
      if (bestScore < score) {
        bestScore = score;
        move = i;
      }
      if (bestScore == 1)
        return i;
      B.unmarkColumn();
    }
    return move;
    // } catch (TimeoutException e) {
    // System.err.println("Timeout!!! Random column selected");
    // return save;
    // }
  }

  /**
   * 
   * @param B board
   * @param L lista colonne
   * @return best move
   */
  private int minmax(CXBoard B, Integer[] L, boolean maximizer) {
    if (maximizer) {
      int maxScore = -1000;
      for (int i : L) {
        int score;
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          score = 1;
        } else if (result == yourWin) {
          score = -1;
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else {
          score = this.minmax(B, B.getAvailableColumns(), false);
        }
        maxScore = Math.max(maxScore, score);
        B.unmarkColumn();
      }
      return maxScore;
    } else {
      // minimizer
      int minScore = 1000;
      for (int i : L) {
        int score;
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          score = 1;
        } else if (result == yourWin) {
          score = -1;
        } else if (result == CXGameState.DRAW) {
          score = 0;
        } else {
          score = this.minmax(B, B.getAvailableColumns(), true);
        }
        minScore = Math.min(minScore, score);
        B.unmarkColumn();
      }
      return minScore;
    }
  }

  public String playerName() {
    return "Babbini-Libra";
  }
}
