/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.geomgraph

/**
 * A Position indicates the position of a Location relative to a graph component (Node, Edge, or
 * Area).
 *
 * @version 1.7
 */
object Position {

  /** An indicator that a Location is <i>on</i> a GraphComponent */
  val ON = 0

  /** An indicator that a Location is to the <i>left</i> of a GraphComponent */
  val LEFT = 1

  /** An indicator that a Location is to the <i>right</i> of a GraphComponent */
  val RIGHT = 2

  /**
   * Returns LEFT if the position is RIGHT, RIGHT if the position is LEFT, or the position
   * otherwise.
   */
  def opposite(position: Int): Int = {
    if (position == LEFT) return RIGHT
    if (position == RIGHT) return LEFT
    position
  }
}
