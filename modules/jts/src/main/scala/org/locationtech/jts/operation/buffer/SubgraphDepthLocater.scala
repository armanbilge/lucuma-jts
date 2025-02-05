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
package org.locationtech.jts.operation.buffer

import java.util
import java.util.Collections

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.Position
import org.locationtech.jts.operation.buffer.SubgraphDepthLocater.DepthSegment

/**
 * Locates a subgraph inside a set of subgraphs, in order to determine the outside depth of the
 * subgraph. The input subgraphs are assumed to have had depths already calculated for their edges.
 *
 * @version 1.7
 */
object SubgraphDepthLocater {

  /**
   * A segment from a directed edge which has been assigned a depth value for its sides.
   */
  private[buffer] class DepthSegment(
    val seg:       LineSegment,
    var leftDepth: Int
  ) // upwardSeg.normalize();
      extends Comparable[DepthSegment] { // input seg is assumed to be normalized
    val upwardSeg = new LineSegment(seg)

    /**
     * Defines a comparison operation on DepthSegments which orders them left to right. Assumes the
     * segments are normalized. <p> The definition of the ordering is: <ul> <li>-1 : if DS1.seg is
     * left of or below DS2.seg (DS1 < DS2) <li>1 : if DS1.seg is right of or above DS2.seg (DS1 >
     * DS2) <li>0 : if the segments are identical </ul>
     *
     * KNOWN BUGS: <ul> <li>The logic does not obey the {link Comparator.compareTo} contract. This
     * is acceptable for the intended usage, but may cause problems if used with some utilities in
     * the Java standard library (e.g. {link Collections.sort()}. </ul>
     *
     * @param obj
     *   a DepthSegment return the comparison value
     */
    override def compareTo(other: DepthSegment): Int = {
      // fast check if segments are trivially ordered along X
      if (upwardSeg.minX >= other.upwardSeg.maxX) return 1
      if (upwardSeg.maxX <= other.upwardSeg.minX) return -1

      /**
       * try and compute a determinate orientation for the segments. Test returns 1 if other is left
       * of this (i.e. this > other)
       */
      var orientIndex = upwardSeg.orientationIndex(other.upwardSeg)
      if (orientIndex != 0) return orientIndex

      /**
       * If comparison between this and other is indeterminate, try the opposite call order. The
       * sign of the result needs to be flipped.
       */
      orientIndex = -1 * other.upwardSeg.orientationIndex(upwardSeg)
      if (orientIndex != 0) return orientIndex
      // otherwise, use standard lexocographic segment ordering
      upwardSeg.compareTo(other.upwardSeg)
    }

    /**
     * Compare two collinear segments for left-most ordering. If segs are vertical, use vertical
     * ordering for comparison. If segs are equal, return 0. Segments are assumed to be directed so
     * that the second coordinate is >= to the first (e.g. up and to the right).
     *
     * @param seg0
     *   a segment to compare
     * @param seg1
     *   a segment to compare return
     */
//    private def compareX(seg0: LineSegment, seg1: LineSegment): Int = {
//      val compare0 = seg0.p0.compareTo(seg1.p0)
//      if (compare0 != 0) return compare0
//      seg0.p1.compareTo(seg1.p1)
//    }

    override def toString = upwardSeg.toString
  }

}

class SubgraphDepthLocater(var subgraphs: util.Collection[BufferSubgraph]) {
  private val seg = new LineSegment

  def getDepth(p: Coordinate): Int = {
    val stabbedSegments = findStabbedSegments(p)
    // if no segments on stabbing line subgraph must be outside all others.
    if (stabbedSegments.size == 0) return 0
    val ds              = Collections.min(stabbedSegments)
    ds.leftDepth
  }

  /**
   * Finds all non-horizontal segments intersecting the stabbing line. The stabbing line is the ray
   * to the right of stabbingRayLeftPt.
   *
   * @param stabbingRayLeftPt
   *   the left-hand origin of the stabbing line return a List of { @link DepthSegments}
   *   intersecting the stabbing line
   */
  private def findStabbedSegments(stabbingRayLeftPt: Coordinate): util.List[DepthSegment] = {
    val stabbedSegments = new util.ArrayList[DepthSegment]
    val i               = subgraphs.iterator
    while (i.hasNext) {
      val bsg = i.next
      // optimization - don't bother checking subgraphs which the ray does not intersect
      val env = bsg.getEnvelope
      if (!(stabbingRayLeftPt.y < env.getMinY || stabbingRayLeftPt.y > env.getMaxY)) {
        findStabbedSegments(stabbingRayLeftPt, bsg.getDirectedEdges, stabbedSegments)
      }
    }
    stabbedSegments
  }

  /**
   * Finds all non-horizontal segments intersecting the stabbing line in the list of dirEdges. The
   * stabbing line is the ray to the right of stabbingRayLeftPt.
   *
   * @param stabbingRayLeftPt
   *   the left-hand origin of the stabbing line
   * @param stabbedSegments
   *   the current list of { @link DepthSegments} intersecting the stabbing line
   */
  private def findStabbedSegments(
    stabbingRayLeftPt: Coordinate,
    dirEdges:          util.List[DirectedEdge],
    stabbedSegments:   util.List[SubgraphDepthLocater.DepthSegment]
  ): Unit = {

    /**
     * Check all forward DirectedEdges only. This is still general, because each Edge has a forward
     * DirectedEdge.
     */
    val i = dirEdges.iterator
    while (i.hasNext) {
      val de = i.next
      if (de.isForward) {
        findStabbedSegments(stabbingRayLeftPt, de, stabbedSegments)
      }
    }
  }

  /**
   * Finds all non-horizontal segments intersecting the stabbing line in the input dirEdge. The
   * stabbing line is the ray to the right of stabbingRayLeftPt.
   *
   * @param stabbingRayLeftPt
   *   the left-hand origin of the stabbing line
   * @param stabbedSegments
   *   the current list of { @link DepthSegments} intersecting the stabbing line
   */
  private def findStabbedSegments(
    stabbingRayLeftPt: Coordinate,
    dirEdge:           DirectedEdge,
    stabbedSegments:   util.List[DepthSegment]
  ): Unit = {
    val pts = dirEdge.getEdge.getCoordinates
    var i   = 0
    while (i < pts.length - 1) {
      seg.p0 = pts(i)
      seg.p1 = pts(i + 1)
      // ensure segment always points upwards
      if (seg.p0.y > seg.p1.y) seg.reverse()
      // skip segment if it is left of the stabbing line
      val maxx = Math.max(seg.p0.x, seg.p1.x)
      if (maxx >= stabbingRayLeftPt.x) {
        // skip horizontal segments (there will be a non-horizontal one carrying the same depth info
        if (!seg.isHorizontal) {
          // skip if segment is above or below stabbing line
          if (!(stabbingRayLeftPt.y < seg.p0.y || stabbingRayLeftPt.y > seg.p1.y)) {
            // skip if stabbing ray is right of the segment
            if (Orientation.index(seg.p0, seg.p1, stabbingRayLeftPt) != Orientation.RIGHT) {
              // stabbing line cuts this segment, so record it
              var depth = dirEdge.getDepth(Position.LEFT)
              // if segment direction was flipped, use RHS depth instead
              if (!seg.p0.equals(pts(i))) depth = dirEdge.getDepth(Position.RIGHT)
              val ds    = new SubgraphDepthLocater.DepthSegment(seg, depth)
              stabbedSegments.add(ds)
            }
          }
        }
      }
      i += 1
    }
  }
}
