/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */ /*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.math.Vector3D

/**
 * Basic computational geometry algorithms for geometry and coordinates defined in 3-dimensional
 * Cartesian space.
 *
 * @author
 *   mdavis
 */
object CGAlgorithms3D {
  def distance(p0: Coordinate, p1: Coordinate): Double = { // default to 2D distance if either Z is not set
    if (java.lang.Double.isNaN(p0.getZ) || java.lang.Double.isNaN(p1.getZ)) return p0.distance(p1)
    val dx = p0.x - p1.x
    val dy = p0.y - p1.y
    val dz = p0.getZ - p1.getZ
    Math.sqrt(dx * dx + dy * dy + dz * dz)
  }

  def distancePointSegment(p: Coordinate, A: Coordinate, B: Coordinate): Double = { // if start = end, then just compute distance to one of the endpoints
    if (A.equals3D(B)) return distance(p, A)
    // otherwise use comp.graphics.algorithms Frequently Asked Questions method
    /*
     * (1) r = AC dot AB
     *         ---------
     *         ||AB||^2
     *
     * r has the following meaning:
     *   r=0 P = A
     *   r=1 P = B
     *   r<0 P is on the backward extension of AB
     *   r>1 P is on the forward extension of AB
     *   0<r<1 P is interior to AB
     */
    val len2 =
      (B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y) + (B.getZ - A.getZ) * (B.getZ - A.getZ)
    if (java.lang.Double.isNaN(len2))
      throw new IllegalArgumentException("Ordinates must not be NaN")
    val r    =
      ((p.x - A.x) * (B.x - A.x) + (p.y - A.y) * (B.y - A.y) + (p.getZ - A.getZ) * (B.getZ - A.getZ)) / len2
    if (r <= 0.0) return distance(p, A)
    if (r >= 1.0) return distance(p, B)
    // compute closest point q on line segment
    val qx   = A.x + r * (B.x - A.x)
    val qy   = A.y + r * (B.y - A.y)
    val qz   = A.getZ + r * (B.getZ - A.getZ)
    // result is distance from p to q
    val dx   = p.x - qx
    val dy   = p.y - qy
    val dz   = p.getZ - qz
    Math.sqrt(dx * dx + dy * dy + dz * dz)
  }

  /**
   * Computes the distance between two 3D segments.
   *
   * @param A
   *   the start point of the first segment
   * @param B
   *   the end point of the first segment
   * @param C
   *   the start point of the second segment
   * @param D
   *   the end point of the second segment return the distance between the segments
   */
  def distanceSegmentSegment(A: Coordinate, B: Coordinate, C: Coordinate, D: Coordinate): Double = {

    /**
     * This calculation is susceptible to roundoff errors when passed large ordinate values. It may
     * be possible to improve this by using {link DD} arithmetic.
     */
    if (A.equals3D(B)) return distancePointSegment(A, C, D)
    if (C.equals3D(B)) return distancePointSegment(C, A, B)

    /**
     * Algorithm derived from http://softsurfer.com/Archive/algorithm_0106/algorithm_0106.htm
     */
    val a     = Vector3D.dot(A, B, A, B)
    val b     = Vector3D.dot(A, B, C, D)
    val c     = Vector3D.dot(C, D, C, D)
    val d     = Vector3D.dot(A, B, C, A)
    val e     = Vector3D.dot(C, D, C, A)
    val denom = a * c - b * b
    if (java.lang.Double.isNaN(denom))
      throw new IllegalArgumentException("Ordinates must not be NaN")
    var s     = .0
    var t     = .0
    if (denom <= 0.0) {

      /**
       * The lines are parallel. In this case solve for the parameters s and t by assuming s is 0.
       */
      s = 0
      // choose largest denominator for optimal numeric conditioning
      if (b > c) t = d / b
      else t = e / c
    } else {
      s = (b * e - c * d) / denom
      t = (a * e - b * d) / denom
    }
    if (s < 0) return distancePointSegment(A, C, D)
    else if (s > 1) return distancePointSegment(B, C, D)
    else if (t < 0) return distancePointSegment(C, A, B)
    else if (t > 1) return distancePointSegment(D, A, B)

    /**
     * The closest points are in interiors of segments, so compute them directly
     */
    val x1 = A.x + s * (B.x - A.x)
    val y1 = A.y + s * (B.y - A.y)
    val z1 = A.getZ + s * (B.getZ - A.getZ)
    val x2 = C.x + t * (D.x - C.x)
    val y2 = C.y + t * (D.y - C.y)
    val z2 = C.getZ + t * (D.getZ - C.getZ)
    // length (p1-p2)
    distance(new Coordinate(x1, y1, z1), new Coordinate(x2, y2, z2))
  }
}
