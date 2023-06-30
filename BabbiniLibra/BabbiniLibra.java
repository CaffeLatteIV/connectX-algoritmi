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
    int max = -1;
    int move = L[0];
    for (int i : L) {
      B.markColumn(i);
      int mm = minmax(B, B.getAvailableColumns(), false);
      if (max < mm) {
        max = mm;
        move = i;
      }
      if (max == 1)
        return i;
      B.unmarkColumn();
    }
    return move;
    // } catch (TimeoutException e) {
    // System.err.println("Timeout!!! Random column selected");
    // return save;
    // }
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }

  /**
   * Check if we can win in a single move
   *
   * Returns the winning column if there is one, otherwise -1
   */
  private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
    for (int i : L) {
      checktime(); // Check timeout at every iteration
      CXGameState state = B.markColumn(i);
      if (state == myWin)
        return i; // Winning column found: return immediately
      B.unmarkColumn();
    }
    return -1;
  }

  /**
   * Check if we can block adversary's victory
   *
   * Returns a blocking column if there is one, otherwise a random one
   * L = colonne disponibili
   */
  private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
    TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

    for (int i : L) {
      checktime();
      T.add(i); // We consider column i as a possible move
      B.markColumn(i);

      int j;
      boolean stop;

      for (j = 0, stop = false; j < L.length && !stop; j++) {
        // try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} //
        // Uncomment to test timeout
        checktime();
        if (!B.fullColumn(L[j])) {
          CXGameState state = B.markColumn(L[j]);
          if (state == yourWin) {
            T.remove(i); // We ignore the i-th column as a possible move
            stop = true; // We don't need to check more
          }
          B.unmarkColumn(); //
        }
      }
      B.unmarkColumn();
    }

    if (T.size() > 0) {
      Integer[] X = T.toArray(new Integer[T.size()]);
      return X[rand.nextInt(X.length)];
    } else {
      return L[rand.nextInt(L.length)];
    }
  }

  /**
   * 
   * @param B board
   * @param L lista colonne
   * @return best move
   */
  public int minmax(CXBoard B, Integer[] L, boolean maximizer) {
    if (maximizer) {
      int maxRes = -1;
      for (int i : L) {
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          return 1;
        }
        if (result == yourWin) {
          return 0;
        }
        if (result == CXGameState.DRAW) {
          return 0;
        }
        maxRes = Math.max(maxRes, this.minmax(B, B.getAvailableColumns(), false));
        B.unmarkColumn();
      }
      return maxRes;
    } else {
      // minimizer
      int minRes = 1;
      for (int i : L) {
        CXGameState result = B.markColumn(i);
        if (result == myWin) {
          return 1;
        }
        if (result == yourWin) {
          return -1;
        }
        if (result == CXGameState.DRAW) {
          return 0;
        }
        minRes = Math.min(minRes, this.minmax(B, B.getAvailableColumns(), true));
        B.unmarkColumn();
      }
      return minRes;
    }
  }

  public String playerName() {
    return "Babbini-Libra";
  }
}
