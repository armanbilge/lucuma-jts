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
package org.locationtech.jts.noding

import org.locationtech.jts.geom.Coordinate

/**
 * An interface for classes which represent a sequence of contiguous line segments. SegmentStrings
 * can carry a context object, which is useful for preserving topological or parentage information.
 *
 * @version 1.7
 */
trait SegmentString {

  /**
   * Gets the user-defined data for this segment string.
   *
   * return the user-defined data
   */
  def getData: Any

  /**
   * Sets the user-defined data for this segment string.
   *
   * @param data
   *   an Object containing user-defined data
   */
  def setData(data: Any): Unit

  def size: Int

  def getCoordinate(i: Int): Coordinate

  def getCoordinates: Array[Coordinate]

  def isClosed: Boolean
}
