// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

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
 */ /*
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
package org.locationtech.jts.geom

/**
 * A factory to create concrete instances of {link CoordinateSequence}s. Used to configure {link
 * GeometryFactory}s to provide specific kinds of CoordinateSequences.
 *
 * @version 1.7
 */
trait CoordinateSequenceFactory {

  /**
   * Returns a {link CoordinateSequence} based on the given array. Whether the array is copied or
   * simply referenced is implementation-dependent. This method must handle null arguments by
   * creating an empty sequence.
   *
   * @param coordinates
   *   the coordinates
   */
  def create(coordinates: Array[Coordinate]): CoordinateSequence

  /**
   * Creates a {link CoordinateSequence} which is a copy of the given {link CoordinateSequence}.
   * This method must handle null arguments by creating an empty sequence.
   *
   * @param coordSeq
   *   the coordinate sequence to copy
   */
  def create(coordSeq: CoordinateSequence): CoordinateSequence

  /**
   * Creates a {link CoordinateSequence} of the specified size and dimension. For this to be useful,
   * the {link CoordinateSequence} implementation must be mutable. <p> If the requested dimension is
   * larger than the CoordinateSequence implementation can provide, then a sequence of maximum
   * possible dimension should be created. An error should not be thrown.
   *
   * @param size
   *   the number of coordinates in the sequence
   * @param dimension
   *   the dimension of the coordinates in the sequence (if user-specifiable, otherwise ignored)
   */
  def create(size: Int, dimension: Int): CoordinateSequence

  /**
   * Creates a {link CoordinateSequence} of the specified size and dimension with measure support.
   * For this to be useful, the {link CoordinateSequence} implementation must be mutable. <p> If the
   * requested dimension or measures are larger than the CoordinateSequence implementation can
   * provide, then a sequence of maximum possible dimension should be created. An error should not
   * be thrown.
   *
   * @param size
   *   the number of coordinates in the sequence
   * @param dimension
   *   the dimension of the coordinates in the sequence (if user-specifiable, otherwise ignored)
   * @param measures
   *   the number of measures of the coordinates in the sequence (if user-specifiable, otherwise
   *   ignored)
   */
  def create(size: Int, dimension: Int, measures: Int): CoordinateSequence
}
