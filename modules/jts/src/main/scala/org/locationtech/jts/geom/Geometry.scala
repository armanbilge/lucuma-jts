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

import java.io.Serializable
import java.util.Comparator

import org.locationtech.jts.algorithm.Centroid
import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.algorithm.InteriorPoint
import org.locationtech.jts.geom.util.GeometryCollectionMapper
import org.locationtech.jts.geom.util.GeometryMapper
//import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.operation.IsSimpleOp
import org.locationtech.jts.operation.buffer.BufferOp
import org.locationtech.jts.operation.distance.DistanceOp
import org.locationtech.jts.operation.overlay.OverlayOp
import org.locationtech.jts.operation.overlay.snap.SnapIfNeededOverlayOp
import org.locationtech.jts.operation.predicate.RectangleContains
import org.locationtech.jts.operation.predicate.RectangleIntersects
import org.locationtech.jts.operation.relate.RelateOp
import org.locationtech.jts.operation.union.UnaryUnionOp
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.util.Assert

/**
 * A representation of a planar, linear vector geometry. <P>
 *
 * <H3>Binary Predicates</H3> Because it is not clear at this time what semantics for spatial
 * analysis methods involving <code>GeometryCollection</code>s would be useful,
 * <code>GeometryCollection</code>s are not supported as arguments to binary predicates or the
 * <code>relate</code> method.
 *
 * <H3>Overlay Methods</H3>
 *
 * The overlay methods return the most specific class possible to represent the result. If the
 * result is homogeneous, a <code>Point</code>, <code>LineString</code>, or <code>Polygon</code>
 * will be returned if the result contains a single element; otherwise, a <code>MultiPoint</code>,
 * <code>MultiLineString</code>, or <code>MultiPolygon</code> will be returned. If the result is
 * heterogeneous a <code>GeometryCollection</code> will be returned. <P>
 *
 * Because it is not clear at this time what semantics for set-theoretic methods involving
 * <code>GeometryCollection</code>s would be useful, <code>GeometryCollections</code> are not
 * supported as arguments to the set-theoretic methods.
 *
 * <H4>Representation of Computed Geometries </H4>
 *
 * The SFS states that the result of a set-theoretic method is the "point-set" result of the usual
 * set-theoretic definition of the operation (SFS 3.2.21.1). However, there are sometimes many ways
 * of representing a point set as a <code>Geometry</code>. <P>
 *
 * The SFS does not specify an unambiguous representation of a given point set returned from a
 * spatial analysis method. One goal of JTS is to make this specification precise and unambiguous.
 * JTS uses a canonical form for <code>Geometry</code>s returned from overlay methods. The canonical
 * form is a <code>Geometry</code> which is simple and noded: <UL> <LI> Simple means that the
 * Geometry returned will be simple according to the JTS definition of <code>isSimple</code>. <LI>
 * Noded applies only to overlays involving <code>LineString</code>s. It means that all intersection
 * points on <code>LineString</code>s will be present as endpoints of <code>LineString</code>s in
 * the result. </UL> This definition implies that non-simple geometries which are arguments to
 * spatial analysis methods must be subjected to a line-dissolve process to ensure that the results
 * are simple.
 *
 * <H4> Constructed Points And The Precision Model </H4>
 *
 * The results computed by the set-theoretic methods may contain constructed points which are not
 * present in the input <code>Geometry</code> s. These new points arise from intersections between
 * line segments in the edges of the input <code>Geometry</code>s. In the general case it is not
 * possible to represent constructed points exactly. This is due to the fact that the coordinates of
 * an intersection point may contain twice as many bits of precision as the coordinates of the input
 * line segments. In order to represent these constructed points explicitly, JTS must truncate them
 * to fit the <code>PrecisionModel</code>. <P>
 *
 * Unfortunately, truncating coordinates moves them slightly. Line segments which would not be
 * coincident in the exact result may become coincident in the truncated representation. This in
 * turn leads to "topology collapses" -- situations where a computed element has a lower dimension
 * than it would in the exact result. <P>
 *
 * When JTS detects topology collapses during the computation of spatial analysis methods, it will
 * throw an exception. If possible the exception will report the location of the collapse. <P>
 *
 * <h3>Geometry Equality</h3>
 *
 * There are two ways of comparing geometries for equality: <b>structural equality</b> and
 * <b>topological equality</b>.
 *
 * <h4>Structural Equality</h4>
 *
 * Structural Equality is provided by the {link #equalsExact(Geometry)} method. This implements a
 * comparison based on exact, structural pointwise equality. The {link #equals(Object)} is a synonym
 * for this method, to provide structural equality semantics for use in Java collections. It is
 * important to note that structural pointwise equality is easily affected by things like ring order
 * and component order. In many situations it will be desirable to normalize geometries before
 * comparing them (using the {link #norm()} or {link #normalize()} methods). {link
 * #equalsNorm(Geometry)} is provided as a convenience method to compute equality over normalized
 * geometries, but it is expensive to use. Finally, {link #equalsExact(Geometry, double)} allows
 * using a tolerance value for point comparison.
 *
 * <h4>Topological Equality</h4>
 *
 * Topological Equality is provided by the {link #equalsTopo(Geometry)} method. It implements the
 * SFS definition of point-set equality defined in terms of the DE-9IM matrix. To support the SFS
 * naming convention, the method {link #equals(Geometry)} is also provided as a synonym. However,
 * due to the potential for confusion with {link #equals(Object)} its use is discouraged. <p> Since
 * {link #equals(Object)} and {link #hashCode()} are overridden, Geometries can be used effectively
 * in Java collections.
 *
 * @version 1.7
 */
@SerialVersionUID(8763622679187376702L)
object Geometry {
  private[geom] val SORTINDEX_POINT              = 0
  private[geom] val SORTINDEX_MULTIPOINT         = 1
  private[geom] val SORTINDEX_LINESTRING         = 2
  private[geom] val SORTINDEX_LINEARRING         = 3
  private[geom] val SORTINDEX_MULTILINESTRING    = 4
  private[geom] val SORTINDEX_POLYGON            = 5
  private[geom] val SORTINDEX_MULTIPOLYGON       = 6
  private[geom] val SORTINDEX_GEOMETRYCOLLECTION = 7
  private val geometryChangedFilter              = new GeometryComponentFilter() {
    override def filter(geom: Geometry): Unit = geom.geometryChangedAction()
  }

  /**
   * Returns true if the array contains any non-empty <code>Geometry</code>s.
   *
   * @param geometries
   *   an array of <code>Geometry</code>s; no elements may be <code>null</code> return
   *   <code>true</code> if any of the <code>Geometry</code>s <code>isEmpty</code> methods return
   *   <code>false</code>
   */
  def hasNonEmptyElements(geometries: Array[Geometry]): Boolean = {
    var i = 0
    while (i < geometries.length) {
      if (!geometries(i).isEmpty) return true
      i += 1; i - 1
    }
    false
  }

  /**
   * Returns true if the array contains any <code>null</code> elements.
   *
   * @param array
   *   an array to validate return <code>true</code> if any of <code>array</code>s elements are
   *   <code>null</code>
   */
  def hasNullElements(array: Array[AnyRef]): Boolean = {
    var i = 0
    while (i < array.length) {
      if (array(i) == null) return true
      i += 1
    }
    false
  }

  /**
   * Throws an exception if <code>g</code>'s type is a <code>GeometryCollection</code>. (Its
   * subclasses do not trigger an exception).
   *
   * @param g
   *   the <code>Geometry</code> to check throws IllegalArgumentException if <code>g</code> is a
   *   <code>GeometryCollection</code> but not one of its subclasses
   */
  def checkNotGeometryCollection(g: Geometry): Unit = if (g.isGeometryCollection)
    throw new IllegalArgumentException("Operation does not support GeometryCollection arguments")
}

@SerialVersionUID(8763622679187376702L)
abstract class Geometry(
  /**
   * The {link GeometryFactory} used to create this Geometry
   */
  val factory: GeometryFactory
)

/**
 * Creates a new <code>Geometry</code> via the specified GeometryFactory.
 *
 * @param factory
 */
    extends Cloneable
    with Comparable[Geometry]
    with Serializable {

  /**
   * The bounding box of this <code>Geometry</code>.
   */
  protected var envelope: Envelope = null

  /**
   * The ID of the Spatial Reference System used by this <code>Geometry</code>
   */
  protected var SRID = factory.getSRID

  /**
   * An object reference which can be used to carry ancillary data defined by the client.
   */
  private var userData: AnyRef = null

  /**
   * Returns the name of this Geometry's actual class.
   *
   * return the name of this <code>Geometry</code>s actual class
   */
  def getGeometryType: String

  /**
   * Returns the ID of the Spatial Reference System used by the <code>Geometry</code>. <P>
   *
   * JTS supports Spatial Reference System information in the simple way defined in the SFS. A
   * Spatial Reference System ID (SRID) is present in each <code>Geometry</code> object.
   * <code>Geometry</code> provides basic accessor operations for this field, but no others. The
   * SRID is represented as an integer.
   *
   * return the ID of the coordinate space in which the <code>Geometry</code> is defined.
   */
  def getSRID: Int = SRID

  /**
   * Sets the ID of the Spatial Reference System used by the <code>Geometry</code>. <p> <b>NOTE:</b>
   * This method should only be used for exceptional circumstances or for backwards compatibility.
   * Normally the SRID should be set on the {link GeometryFactory} used to create the geometry.
   * SRIDs set using this method will <i>not</i> be propagated to geometries returned by
   * constructive methods.
   *
   * @see
   *   GeometryFactory
   */
  def setSRID(SRID: Int): Unit = this.SRID = SRID

  /**
   * Gets the factory which contains the context in which this geometry was created.
   *
   * return the factory for this geometry
   */
  def getFactory: GeometryFactory = factory

  /**
   * Gets the user data object for this geometry, if any.
   *
   * return the user data object, or <code>null</code> if none set
   */
  def getUserData: Object = userData

  /**
   * Returns the number of {link Geometry}s in a {link GeometryCollection} (or 1, if the geometry is
   * not a collection).
   *
   * return the number of geometries contained in this geometry
   */
  def getNumGeometries = 1

  /**
   * Returns an element {link Geometry} from a {link GeometryCollection} (or <code>this</code>, if
   * the geometry is not a collection).
   *
   * @param n
   *   the index of the geometry element return the n'th geometry contained in this geometry
   */
  def getGeometryN(n: Int): Geometry = this

  /**
   * A simple scheme for applications to add their own custom data to a Geometry. An example use
   * might be to add an object representing a Coordinate Reference System. <p> Note that user data
   * objects are not present in geometries created by construction methods.
   *
   * @param userData
   *   an object, the semantics for which are defined by the application using this Geometry
   */
  def setUserData(userData: AnyRef): Unit = this.userData = userData

  /**
   * Returns the <code>PrecisionModel</code> used by the <code>Geometry</code>.
   *
   * return the specification of the grid of allowable points, for this <code>Geometry</code> and
   * all other <code>Geometry</code>s
   */
  def getPrecisionModel: PrecisionModel = factory.getPrecisionModel

  /**
   * Returns a vertex of this <code>Geometry</code> (usually, but not necessarily, the first one).
   * The returned coordinate should not be assumed to be an actual Coordinate object used in the
   * internal representation.
   *
   * return a { @link Coordinate} which is a vertex of this <code>Geometry</code>. return null if
   * this Geometry is empty
   */
  def getCoordinate: Coordinate

  /**
   * Returns an array containing the values of all the vertices for this geometry. If the geometry
   * is a composite, the array will contain all the vertices for the components, in the order in
   * which the components occur in the geometry. <p> In general, the array cannot be assumed to be
   * the actual internal storage for the vertices. Thus modifying the array may not modify the
   * geometry itself. Use the {link CoordinateSequence#setOrdinate} method (possibly on the
   * components) to modify the underlying data. If the coordinates are modified, {link
   * #geometryChanged} must be called afterwards.
   *
   * return the vertices of this <code>Geometry</code>
   * @see
   *   #geometryChanged
   * @see
   *   CoordinateSequence#setOrdinate
   */
  def getCoordinates: Array[Coordinate]

  /**
   * Returns the count of this <code>Geometry</code>s vertices. The <code>Geometry</code> s
   * contained by composite <code>Geometry</code>s must be Geometry's; that is, they must implement
   * <code>getNumPoints</code>
   *
   * return the number of vertices in this <code>Geometry</code>
   */
  def getNumPoints: Int

  /**
   * Tests whether this {link Geometry} is simple. The SFS definition of simplicity follows the
   * general rule that a Geometry is simple if it has no points of self-tangency, self-intersection
   * or other anomalous points. <p> Simplicity is defined for each {link Geometry} subclass as
   * follows: <ul> <li>Valid polygonal geometries are simple, since their rings must not
   * self-intersect. <code>isSimple</code> tests for this condition and reports <code>false</code>
   * if it is not met. (This is a looser test than checking for validity). <li>Linear rings have the
   * same semantics. <li>Linear geometries are simple iff they do not self-intersect at points other
   * than boundary points. <li>Zero-dimensional geometries (points) are simple iff they have no
   * repeated points. <li>Empty <code>Geometry</code>s are always simple. </ul>
   *
   * return <code>true</code> if this <code>Geometry</code> is simple
   * @see
   *   #isValid
   */
  def isSimple: Boolean = {
    val op = new IsSimpleOp(this)
    op.isSimple
  }

  /**
   * Tests whether this <code>Geometry</code> is topologically valid, according to the OGC SFS
   * specification. <p> For validity rules see the Javadoc for the specific Geometry subclass.
   *
   * return <code>true</code> if this <code>Geometry</code> is valid
   * @see
   *   IsValidOp
   */
  def isValid: Boolean = IsValidOp.isValid(this)

  /**
   * Tests whether the set of points covered by this <code>Geometry</code> is empty.
   *
   * return <code>true</code> if this <code>Geometry</code> does not cover any points
   */
  def isEmpty: Boolean

  /**
   * Returns the minimum distance between this <code>Geometry</code> and another
   * <code>Geometry</code>.
   *
   * @param g
   *   the <code>Geometry</code> from which to compute the distance return the distance between the
   *   geometries return 0 if either input geometry is empty throws IllegalArgumentException if g is
   *   null
   */
  def distance(g: Geometry): Double = DistanceOp.distance(this, g)

  /**
   * Tests whether the distance from this <code>Geometry</code> to another is less than or equal to
   * a specified value.
   *
   * @param geom
   *   the Geometry to check the distance to
   * @param distance
   *   the distance value to compare return <code>true</code> if the geometries are less than
   *   <code>distance</code> apart.
   */
  def isWithinDistance(geom: Geometry, distance: Double): Boolean =
    DistanceOp.isWithinDistance(this, geom, distance)

  /**
   * Tests whether this is a rectangular {link Polygon}.
   *
   * return true if the geometry is a rectangle.
   */
  def isRectangle: Boolean = // Polygon overrides to check for actual rectangle
    false

  /**
   * Returns the area of this <code>Geometry</code>. Areal Geometries have a non-zero area. They
   * override this function to compute the area. Others return 0.0
   *
   * return the area of the Geometry
   */
  def getArea = 0.0

  /**
   * Returns the length of this <code>Geometry</code>. Linear geometries return their length. Areal
   * geometries return their perimeter. They override this function to compute the area. Others
   * return 0.0
   *
   * return the length of the Geometry
   */
  def getLength = 0.0

  /**
   * Computes the centroid of this <code>Geometry</code>. The centroid is equal to the centroid of
   * the set of component Geometries of highest dimension (since the lower-dimension geometries
   * contribute zero "weight" to the centroid). <p> The centroid of an empty geometry is <code>POINT
   * EMPTY</code>.
   *
   * return a { @link Point} which is the centroid of this Geometry
   */
  def getCentroid: Point = {
    if (isEmpty) return factory.createPoint
    val centPt = Centroid.getCentroid(this)
    createPointFromInternalCoord(centPt, this)
  }

  /**
   * Computes an interior point of this <code>Geometry</code>. An interior point is guaranteed to
   * lie in the interior of the Geometry, if it possible to calculate such a point exactly.
   * Otherwise, the point may lie on the boundary of the geometry. <p> The interior point of an
   * empty geometry is <code>POINT EMPTY</code>.
   *
   * return a { @link Point} which is in the interior of this Geometry
   */
  def getInteriorPoint: Point = {
    if (isEmpty) return factory.createPoint
    val pt = InteriorPoint.getInteriorPoint(this)
    createPointFromInternalCoord(pt, this)
  }

  /**
   * Returns the dimension of this geometry. The dimension of a geometry is is the topological
   * dimension of its embedding in the 2-D Euclidean plane. In the JTS spatial model, dimension
   * values are in the set {0,1,2}. <p> Note that this is a different concept to the dimension of
   * the vertex {link Coordinate}s. The geometry dimension can never be greater than the coordinate
   * dimension. For example, a 0-dimensional geometry (e.g. a Point) may have a coordinate dimension
   * of 3 (X,Y,Z).
   *
   * return the topological dimension of this geometry.
   */
  def getDimension: Int

  /**
   * Returns the boundary, or an empty geometry of appropriate dimension if this
   * <code>Geometry</code> is empty. (In the case of zero-dimensional geometries, ' an empty
   * GeometryCollection is returned.) For a discussion of this function, see the OpenGIS Simple
   * Features Specification. As stated in SFS Section 2.1.13.1, "the boundary of a Geometry is a set
   * of Geometries of the next lower dimension."
   *
   * return the closure of the combinatorial boundary of this <code>Geometry</code>
   */
  def getBoundary: Geometry

  /**
   * Returns the dimension of this <code>Geometry</code>s inherent boundary.
   *
   * return the dimension of the boundary of the class implementing this interface, whether or not
   * this object is the empty geometry. Returns <code>Dimension.FALSE</code> if the boundary is the
   * empty geometry.
   */
  def getBoundaryDimension: Int

  /**
   * Gets a Geometry representing the envelope (bounding box) of this <code>Geometry</code>. <p> If
   * this <code>Geometry</code> is: <ul> <li>empty, returns an empty <code>Point</code>. <li>a
   * point, returns a <code>Point</code>. <li>a line parallel to an axis, a two-vertex
   * <code>LineString</code> <li>otherwise, returns a <code>Polygon</code> whose vertices are (minx
   * miny, maxx miny, maxx maxy, minx maxy, minx miny). </ul>
   *
   * return a Geometry representing the envelope of this Geometry
   * @see
   *   GeometryFactory#toGeometry(Envelope)
   */
  def getEnvelope: Geometry = getFactory.toGeometry(getEnvelopeInternal)

  /**
   * Gets an {link Envelope} containing the minimum and maximum x and y values in this
   * <code>Geometry</code>. If the geometry is empty, an empty <code>Envelope</code> is returned.
   * <p> The returned object is a copy of the one maintained internally, to avoid aliasing issues.
   * For best performance, clients which access this envelope frequently should cache the return
   * value.
   *
   * return the envelope of this <code>Geometry</code>. return an empty Envelope if this Geometry is
   * empty
   */
  def getEnvelopeInternal: Envelope = {
    if (envelope == null) envelope = computeEnvelopeInternal
    new Envelope(envelope)
  }

  /**
   * Notifies this geometry that its coordinates have been changed by an external party (for
   * example, via a {link CoordinateFilter}). When this method is called the geometry will flush
   * and/or update any derived information it has cached (such as its {link Envelope} ). The
   * operation is applied to all component Geometries.
   */
  def geometryChanged(): Unit = applyF(Geometry.geometryChangedFilter)

  /**
   * Notifies this Geometry that its Coordinates have been changed by an external party. When
   * #geometryChanged is called, this method will be called for this Geometry and its component
   * Geometries.
   *
   * @see
   *   #apply(GeometryComponentFilter)
   */
  protected def geometryChangedAction(): Unit = envelope = null

  /**
   * Tests whether this geometry is disjoint from the argument geometry. <p> The
   * <code>disjoint</code> predicate has the following equivalent definitions: <ul> <li>The two
   * geometries have no point in common <li>The DE-9IM Intersection Matrix for the two geometries
   * matches <code>[FF*FF****]</code> <li><code>! g.intersects(this) = true</code>
   * <br>(<code>disjoint</code> is the inverse of <code>intersects</code>) </ul>
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if the two <code>Geometry</code>s are disjoint
   * @see
   *   Geometry#intersects
   */
  def disjoint(g: Geometry): Boolean = !intersects(g)

  /**
   * Tests whether this geometry touches the argument geometry. <p> The <code>touches</code>
   * predicate has the following equivalent definitions: <ul> <li>The geometries have at least one
   * point in common, but their interiors do not intersect. <li>The DE-9IM Intersection Matrix for
   * the two geometries matches at least one of the following patterns <ul>
   * <li><code>[FT*******]</code> <li><code>[F**T*****]</code> <li><code>[F***T****]</code> </ul>
   * </ul> If both geometries have dimension 0, the predicate returns <code>false</code>, since
   * points have only interiors. This predicate is symmetric.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if the two <code>Geometry</code>s touch; Returns <code>false</code> if both
   *   <code>Geometry</code>s are points
   */
  def touches(g: Geometry): Boolean = { // short-circuit test
    if (!getEnvelopeInternal.intersects(g.getEnvelopeInternal)) return false
    relate(g).isTouches(getDimension, g.getDimension)
  }

  /**
   * Tests whether this geometry intersects the argument geometry. <p> The <code>intersects</code>
   * predicate has the following equivalent definitions: <ul> <li>The two geometries have at least
   * one point in common <li>The DE-9IM Intersection Matrix for the two geometries matches at least
   * one of the patterns <ul> <li><code>[T********]</code> <li><code>[*T*******]</code>
   * <li><code>[***T*****]</code> <li><code>[****T****]</code> </ul> <li><code>! g.disjoint(this) =
   * true</code> <br>(<code>intersects</code> is the inverse of <code>disjoint</code>) </ul>
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if the two <code>Geometry</code>s intersect
   * @see
   *   Geometry#disjoint
   */
  def intersects(g: Geometry): Boolean = { // short-circuit envelope test
    if (!getEnvelopeInternal.intersects(g.getEnvelopeInternal)) return false

    /**
     * TODO: (MD) Add optimizations:
     *
     *   - for P-A case: If P is in env(A), test for point-in-poly
     *
     *   - for A-A case: If env(A1).overlaps(env(A2)) test for overlaps via point-in-poly first
     *     (both ways) Possibly optimize selection of point to test by finding point of A1 closest
     *     to centre of env(A2). (Is there a test where we shouldn't bother - e.g. if env A is much
     *     smaller than env B, maybe there's no point in testing pt(B) in env(A)?
     */
    // optimization for rectangle arguments
    if (isRectangle) return RectangleIntersects.intersects(this.asInstanceOf[Polygon], g)
    if (g.isRectangle) return RectangleIntersects.intersects(g.asInstanceOf[Polygon], this)
    if (isGeometryCollection || g.isGeometryCollection) {
      var i = 0
      while (i < getNumGeometries) {
        var j = 0
        while (j < g.getNumGeometries) {
          if (getGeometryN(i).intersects(g.getGeometryN(j))) return true
          j += 1
        }
        i += 1
      }
      return false
    }
    // general case
    relate(g).isIntersects
  }

  /**
   * Tests whether this geometry crosses the argument geometry. <p> The <code>crosses</code>
   * predicate has the following equivalent definitions: <ul> <li>The geometries have some but not
   * all interior points in common. <li>The DE-9IM Intersection Matrix for the two geometries
   * matches one of the following patterns: <ul> <li><code>[T*T******]</code> (for P/L, P/A, and L/A
   * situations) <li><code>[T*****T**]</code> (for L/P, A/P, and A/L situations)
   * <li><code>[0********]</code> (for L/L situations) </ul> </ul> For any other combination of
   * dimensions this predicate returns <code>false</code>. <p> The SFS defined this predicate only
   * for P/L, P/A, L/L, and L/A situations. In order to make the relation symmetric, JTS extends the
   * definition to apply to L/P, A/P and A/L situations as well.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if the two <code>Geometry</code>s cross.
   */
  def crosses(g: Geometry): Boolean = {
    if (!getEnvelopeInternal.intersects(g.getEnvelopeInternal)) return false
    relate(g).isCrosses(getDimension, g.getDimension)
  }

  /**
   * Tests whether this geometry is within the specified geometry. <p> The <code>within</code>
   * predicate has the following equivalent definitions: <ul> <li>Every point of this geometry is a
   * point of the other geometry, and the interiors of the two geometries have at least one point in
   * common. <li>The DE-9IM Intersection Matrix for the two geometries matches
   * <code>[T*F**F***]</code> <li><code>g.contains(this) = true</code> <br>(<code>within</code> is
   * the converse of {link #contains}) </ul> An implication of the definition is that "The boundary
   * of a Geometry is not within the Geometry". In other words, if a geometry A is a subset of the
   * points in the boundary of a geometry B, <code>A.within(B) = false</code> (As a concrete
   * example, take A to be a LineString which lies in the boundary of a Polygon B.) For a predicate
   * with similar behaviour but avoiding this subtle limitation, see {link #coveredBy}.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if this <code>Geometry</code> is within <code>g</code>
   * @see
   *   Geometry#contains
   * @see
   *   Geometry#coveredBy
   */
  def within(g: Geometry): Boolean = g.contains(this)

  /**
   * Tests whether this geometry contains the argument geometry. <p> The <code>contains</code>
   * predicate has the following equivalent definitions: <ul> <li>Every point of the other geometry
   * is a point of this geometry, and the interiors of the two geometries have at least one point in
   * common. <li>The DE-9IM Intersection Matrix for the two geometries matches the pattern
   * <code>[T*****FF*]</code> <li><code>g.within(this) = true</code> <br>(<code>contains</code> is
   * the converse of {link #within} ) </ul> An implication of the definition is that "Geometries do
   * not contain their boundary". In other words, if a geometry A is a subset of the points in the
   * boundary of a geometry B, <code>B.contains(A) = false</code>. (As a concrete example, take A to
   * be a LineString which lies in the boundary of a Polygon B.) For a predicate with similar
   * behaviour but avoiding this subtle limitation, see {link #covers}.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if this <code>Geometry</code> contains <code>g</code>
   * @see
   *   Geometry#within
   * @see
   *   Geometry#covers
   */
  def contains(g: Geometry): Boolean = { // optimization - lower dimension cannot contain areas
    if (g.getDimension == 2 && getDimension < 2) return false
    // optimization - P cannot contain a non-zero-length L
    // Note that a point can contain a zero-length lineal geometry,
    // since the line has no boundary due to Mod-2 Boundary Rule
    if (g.getDimension == 1 && getDimension < 1 && g.getLength > 0.0) return false
    // optimization - envelope test
    if (!getEnvelopeInternal.contains(g.getEnvelopeInternal)) return false
    if (isRectangle) return RectangleContains.contains(this.asInstanceOf[Polygon], g)
    relate(g).isContains
  }

  /**
   * Tests whether this geometry overlaps the specified geometry. <p> The <code>overlaps</code>
   * predicate has the following equivalent definitions: <ul> <li>The geometries have at least one
   * point each not shared by the other (or equivalently neither covers the other), they have the
   * same dimension, and the intersection of the interiors of the two geometries has the same
   * dimension as the geometries themselves. <li>The DE-9IM Intersection Matrix for the two
   * geometries matches <code>[T*T***T**]</code> (for two points or two surfaces) or
   * <code>[1*T***T**]</code> (for two curves) </ul> If the geometries are of different dimension
   * this predicate returns <code>false</code>. This predicate is symmetric.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if the two <code>Geometry</code>s overlap.
   */
  def overlaps(g: Geometry): Boolean = {
    if (!getEnvelopeInternal.intersects(g.getEnvelopeInternal)) return false
    relate(g).isOverlaps(getDimension, g.getDimension)
  }

  /**
   * Tests whether this geometry covers the argument geometry. <p> The <code>covers</code> predicate
   * has the following equivalent definitions: <ul> <li>Every point of the other geometry is a point
   * of this geometry. <li>The DE-9IM Intersection Matrix for the two geometries matches at least
   * one of the following patterns: <ul> <li><code>[T*****FF*]</code> <li><code>[*T****FF*]</code>
   * <li><code>[***T**FF*]</code> <li><code>[****T*FF*]</code> </ul> <li><code>g.coveredBy(this) =
   * true</code> <br>(<code>covers</code> is the converse of {link #coveredBy}) </ul> If either
   * geometry is empty, the value of this predicate is <code>false</code>. <p> This predicate is
   * similar to {link #contains}, but is more inclusive (i.e. returns <code>true</code> for more
   * cases). In particular, unlike <code>contains</code> it does not distinguish between points in
   * the boundary and in the interior of geometries. For most situations, <code>covers</code> should
   * be used in preference to <code>contains</code>. As an added benefit, <code>covers</code> is
   * more amenable to optimization, and hence should be more performant.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if this <code>Geometry</code> covers <code>g</code>
   * @see
   *   Geometry#contains
   * @see
   *   Geometry#coveredBy
   */
  def covers(g: Geometry): Boolean = { // optimization - lower dimension cannot cover areas
    if (g.getDimension == 2 && getDimension < 2) return false
    // optimization - P cannot cover a non-zero-length L
    // Note that a point can cover a zero-length lineal geometry
    if (g.getDimension == 1 && getDimension < 1 && g.getLength > 0.0) return false
    if (!getEnvelopeInternal.covers(g.getEnvelopeInternal)) return false
    if (isRectangle) { // since we have already tested that the test envelope is covered
      return true
    }
    relate(g).isCovers
  }

  /**
   * Tests whether this geometry is covered by the argument geometry. <p> The <code>coveredBy</code>
   * predicate has the following equivalent definitions: <ul> <li>Every point of this geometry is a
   * point of the other geometry. <li>The DE-9IM Intersection Matrix for the two geometries matches
   * at least one of the following patterns: <ul> <li><code>[T*F**F***]</code>
   * <li><code>[*TF**F***]</code> <li><code>[**FT*F***]</code> <li><code>[**F*TF***]</code> </ul>
   * <li><code>g.covers(this) = true</code> <br>(<code>coveredBy</code> is the converse of {link
   * #covers}) </ul> If either geometry is empty, the value of this predicate is <code>false</code>.
   * <p> This predicate is similar to {link #within}, but is more inclusive (i.e. returns
   * <code>true</code> for more cases).
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if this <code>Geometry</code> is covered by <code>g</code>
   * @see
   *   Geometry#within
   * @see
   *   Geometry#covers
   */
  def coveredBy(g: Geometry): Boolean = g.covers(this)

  /**
   * Tests whether the elements in the DE-9IM {link IntersectionMatrix} for the two
   * <code>Geometry</code>s match the elements in <code>intersectionPattern</code>. The pattern is a
   * 9-character string, with symbols drawn from the following set: <UL> <LI> 0 (dimension 0) <LI> 1
   * (dimension 1) <LI> 2 (dimension 2) <LI> T ( matches 0, 1 or 2) <LI> F ( matches FALSE) <LI> * (
   * matches any value) </UL> For more information on the DE-9IM, see the <i>OpenGIS Simple Features
   * Specification</i>.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @param intersectionPattern
   *   the pattern against which to check the intersection matrix for the two <code>Geometry</code>s
   *   return <code>true</code> if the DE-9IM intersection matrix for the two <code>Geometry</code>s
   *   match <code>intersectionPattern</code>
   * @see
   *   IntersectionMatrix
   */
  def relate(g: Geometry, intersectionPattern: String): Boolean =
    relate(g).matches(intersectionPattern)

  /**
   * Returns the DE-9IM {link IntersectionMatrix} for the two <code>Geometry</code>s.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return an { @link
   *   IntersectionMatrix} describing the intersections of the interiors, boundaries and exteriors
   *   of the two <code>Geometry</code>s
   */
  def relate(g: Geometry): IntersectionMatrix = {
    Geometry.checkNotGeometryCollection(this)
    Geometry.checkNotGeometryCollection(g)
    RelateOp.relate(this, g)
  }

  /**
   * Tests whether this geometry is topologically equal to the argument geometry. <p> This method is
   * included for backward compatibility reasons. It has been superseded by the {link
   * #equalsTopo(Geometry)} method, which has been named to clearly denote its functionality. <p>
   * This method should NOT be confused with the method {link #equals(Object)}, which implements an
   * exact equality comparison.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return true if the
   *   two <code>Geometry</code>s are topologically equal
   * @see
   *   #equalsTopo(Geometry)
   */
  def equals(g: Geometry): Boolean = {
    if (g == null) return false
    equalsTopo(g)
  }

  /**
   * Tests whether this geometry is topologically equal to the argument geometry as defined by the
   * SFS <code>equals</code> predicate. <p> The SFS <code>equals</code> predicate has the following
   * equivalent definitions: <ul> <li>The two geometries have at least one point in common, and no
   * point of either geometry lies in the exterior of the other geometry. <li>The DE-9IM
   * Intersection Matrix for the two geometries matches the pattern <code>T*F**FFF*</code> <pre> T*F
   * **F FF* </pre> </ul> <b>Note</b> that this method computes <b>topologically equality</b>. For
   * structural equality, see {link #equalsExact(Geometry)}.
   *
   * @param g
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if the two <code>Geometry</code>s are topologically equal
   * @see
   *   #equalsExact(Geometry)
   */
  def equalsTopo(g: Geometry): Boolean = {
    if (!(getEnvelopeInternal == g.getEnvelopeInternal)) return false
    relate(g).isEquals(getDimension, g.getDimension)
  }

  /**
   * Tests whether this geometry is structurally and numerically equal to a given
   * <code>Object</code>. If the argument <code>Object</code> is not a <code>Geometry</code>, the
   * result is <code>false</code>. Otherwise, the result is computed using {link
   * #equalsExact(Geometry)}. <p> This method is provided to fulfill the Java contract for
   * value-based object equality. In conjunction with {link #hashCode()} it provides semantics which
   * are most useful for using <code>Geometry</code>s as keys and values in Java collections. <p>
   * Note that to produce the expected result the input geometries should be in normal form. It is
   * the caller's responsibility to perform this where required (using {link Geometry#norm()} or
   * {link #normalize()} as appropriate).
   *
   * @param o
   *   the Object to compare return true if this geometry is exactly equal to the argument
   * @see
   *   #equalsExact(Geometry)
   * @see
   *   #hashCode()
   * @see
   *   #norm()
   * @see
   *   #normalize()
   */
  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[Geometry]) return false
    val g = o.asInstanceOf[Geometry]
    equalsExact(g)
  }

  /**
   * Gets a hash code for the Geometry.
   *
   * return an integer value suitable for use as a hashcode
   */
  override def hashCode: Int = getEnvelopeInternal.hashCode

  override def toString: String = "toText"

  /**
   * Returns the Well-known Text representation of this <code>Geometry</code>. For a definition of
   * the Well-known Text format, see the OpenGIS Simple Features Specification.
   *
   * return the Well-known Text representation of this <code>Geometry</code>
   */
//  def toText: String = {
//    val writer = new WKTWriter
//    writer.write(this)
//  }

  /**
   * Computes a buffer area around this geometry having the given width. The buffer of a Geometry is
   * the Minkowski sum or difference of the geometry with a disc of radius
   * <code>abs(distance)</code>. <p> Mathematically-exact buffer area boundaries can contain
   * circular arcs. To represent these arcs using linear geometry they must be approximated with
   * line segments. The buffer geometry is constructed using 8 segments per quadrant to approximate
   * the circular arcs. The end cap style is <code>CAP_ROUND</code>. <p> The buffer operation always
   * returns a polygonal result. The negative or zero-distance buffer of lines and points is always
   * an empty {link Polygon}. This is also the result for the buffers of degenerate (zero-area)
   * polygons.
   *
   * @param distance
   *   the width of the buffer (may be positive, negative or 0) return a polygonal geometry
   *   representing the buffer region (which may be empty) throws TopologyException if a robustness
   *   error occurs
   * @see
   *   #buffer(double, int)
   * @see
   *   #buffer(double, int, int)
   */
  def buffer(distance: Double): Geometry = BufferOp.bufferOp(this, distance)

  /**
   * Computes a buffer area around this geometry having the given width and with a specified
   * accuracy of approximation for circular arcs. <p> Mathematically-exact buffer area boundaries
   * can contain circular arcs. To represent these arcs using linear geometry they must be
   * approximated with line segments. The <code>quadrantSegments</code> argument allows controlling
   * the accuracy of the approximation by specifying the number of line segments used to represent a
   * quadrant of a circle <p> The buffer operation always returns a polygonal result. The negative
   * or zero-distance buffer of lines and points is always an empty {link Polygon}. This is also the
   * result for the buffers of degenerate (zero-area) polygons.
   *
   * @param distance
   *   the width of the buffer (may be positive, negative or 0)
   * @param quadrantSegments
   *   the number of line segments used to represent a quadrant of a circle return a polygonal
   *   geometry representing the buffer region (which may be empty) throws TopologyException if a
   *   robustness error occurs
   * @see
   *   #buffer(double)
   * @see
   *   #buffer(double, int, int)
   */
  def buffer(distance: Double, quadrantSegments: Int): Geometry =
    BufferOp.bufferOp(this, distance, quadrantSegments)

  /**
   * Computes a buffer area around this geometry having the given width and with a specified
   * accuracy of approximation for circular arcs, and using a specified end cap style. <p>
   * Mathematically-exact buffer area boundaries can contain circular arcs. To represent these arcs
   * using linear geometry they must be approximated with line segments. The
   * <code>quadrantSegments</code> argument allows controlling the accuracy of the approximation by
   * specifying the number of line segments used to represent a quadrant of a circle <p> The end cap
   * style specifies the buffer geometry that will be created at the ends of linestrings. The styles
   * provided are: <ul> <li><code>BufferOp.CAP_ROUND</code> - (default) a semi-circle
   * <li><code>BufferOp.CAP_BUTT</code> - a straight line perpendicular to the end segment
   * <li><code>BufferOp.CAP_SQUARE</code> - a half-square </ul> <p> The buffer operation always
   * returns a polygonal result. The negative or zero-distance buffer of lines and points is always
   * an empty {link Polygon}. This is also the result for the buffers of degenerate (zero-area)
   * polygons.
   *
   * @param distance
   *   the width of the buffer (may be positive, negative or 0)
   * @param quadrantSegments
   *   the number of line segments used to represent a quadrant of a circle
   * @param endCapStyle
   *   the end cap style to use return a polygonal geometry representing the buffer region (which
   *   may be empty) throws TopologyException if a robustness error occurs
   * @see
   *   #buffer(double)
   * @see
   *   #buffer(double, int)
   * @see
   *   BufferOp
   */
  def buffer(distance: Double, quadrantSegments: Int, endCapStyle: Int): Geometry =
    BufferOp.bufferOp(this, distance, quadrantSegments, endCapStyle)

  /**
   * Computes the smallest convex <code>Polygon</code> that contains all the points in the
   * <code>Geometry</code>. This obviously applies only to <code>Geometry</code> s which contain 3
   * or more points; the results for degenerate cases are specified as follows: <TABLE> <TR> <TH>
   * Number of <code>Point</code>s in argument <code>Geometry</code> </TH> <TH>
   * <code>Geometry</code> class of result </TH> </TR> <TR> <TD> 0 </TD> <TD> empty
   * <code>GeometryCollection</code> </TD> </TR> <TR> <TD> 1 </TD> <TD> <code>Point</code> </TD>
   * </TR> <TR> <TD> 2 </TD> <TD> <code>LineString</code> </TD> </TR> <TR> <TD> 3 or more </TD> <TD>
   * <code>Polygon</code> </TD> </TR> </TABLE>
   *
   * return the minimum-area convex polygon containing this <code>Geometry</code>' s points
   */
  def convexHull: Geometry = new ConvexHull(this).getConvexHull

  /**
   * Computes a new geometry which has all component coordinate sequences in reverse order (opposite
   * orientation) to this one.
   *
   * return a reversed geometry
   */
  def reverse: Geometry = {
    val res: Geometry = reverseInternal
    if (this.envelope != null) res.envelope = this.envelope.copy
    res.setSRID(getSRID)
    res
  }

  protected def reverseInternal: Geometry

  /**
   * Computes a <code>Geometry</code> representing the point-set which is common to both this
   * <code>Geometry</code> and the <code>other</code> Geometry. <p> The intersection of two
   * geometries of different dimension produces a result geometry of dimension less than or equal to
   * the minimum dimension of the input geometries. The result geometry may be a heterogeneous {link
   * GeometryCollection}. If the result is empty, it is an atomic geometry with the dimension of the
   * lowest input dimension. <p> Intersection of {link GeometryCollection}s is supported only for
   * homogeneous collection types. <p> Non-empty heterogeneous {link GeometryCollection} arguments
   * are not supported.
   *
   * @param other
   *   the <code>Geometry</code> with which to compute the intersection return a Geometry
   *   representing the point-set common to the two <code>Geometry</code>s throws TopologyException
   *   if a robustness error occurs throws IllegalArgumentException if the argument is a non-empty
   *   heterogeneous <code>GeometryCollection</code>
   */
  def intersection(other: Geometry): Geometry = {

    /**
     * TODO: MD - add optimization for P-A case using Point-In-Polygon
     */
    // special case: if one input is empty ==> empty
    if (this.isEmpty || other.isEmpty)
      return OverlayOp.createEmptyResult(OverlayOp.INTERSECTION, this, other, factory)
    // compute for GCs
    // (An inefficient algorithm, but will work)
    // TODO: improve efficiency of computation for GCs
    if (this.isGeometryCollection) {
      val g2 = other
      return GeometryCollectionMapper.map(this.asInstanceOf[GeometryCollection],
                                          new GeometryMapper.MapOp() {
                                            override def map(g: Geometry): Geometry =
                                              g.intersection(g2)
                                          }
      )
    }
    // No longer needed since GCs are handled by previous code
    // checkNotGeometryCollection(this);
    // checkNotGeometryCollection(other);
    SnapIfNeededOverlayOp.overlayOp(this, other, OverlayOp.INTERSECTION)
  }

  /**
   * Computes a <code>Geometry</code> representing the point-set which is contained in both this
   * <code>Geometry</code> and the <code>other</code> Geometry. <p> The union of two geometries of
   * different dimension produces a result geometry of dimension equal to the maximum dimension of
   * the input geometries. The result geometry may be a heterogeneous {link GeometryCollection}. If
   * the result is empty, it is an atomic geometry with the dimension of the highest input
   * dimension. <p> Unioning {link LineString}s has the effect of <b>noding</b> and
   * <b>dissolving</b> the input linework. In this context "noding" means that there will be a node
   * or endpoint in the result for every endpoint or line segment crossing in the input.
   * "Dissolving" means that any duplicate (i.e. coincident) line segments or portions of line
   * segments will be reduced to a single line segment in the result. If <b>merged</b> linework is
   * required, the {link LineMerger} class can be used. <p> Non-empty {link GeometryCollection}
   * arguments are not supported.
   *
   * @param other
   *   the <code>Geometry</code> with which to compute the union return a point-set combining the
   *   points of this <code>Geometry</code> and the points of <code>other</code> throws
   *   TopologyException if a robustness error occurs throws IllegalArgumentException if either
   *   input is a non-empty GeometryCollection
   * @see
   *   LineMerger
   */
  def union(other: Geometry): Geometry = { // handle empty geometry cases
    if (this.isEmpty || other.isEmpty) {
      if (this.isEmpty && other.isEmpty)
        return OverlayOp.createEmptyResult(OverlayOp.UNION, this, other, factory)
      // special case: if either input is empty ==> other input
      if (this.isEmpty) return other.copy
      if (other.isEmpty) return copy
    }
    // TODO: optimize if envelopes of geometries do not intersect
    Geometry.checkNotGeometryCollection(this)
    Geometry.checkNotGeometryCollection(other)
    SnapIfNeededOverlayOp.overlayOp(this, other, OverlayOp.UNION)
  }

  /**
   * Computes a <code>Geometry</code> representing the closure of the point-set of the points
   * contained in this <code>Geometry</code> that are not contained in the <code>other</code>
   * Geometry. <p> If the result is empty, it is an atomic geometry with the dimension of the
   * left-hand input. <p> Non-empty {link GeometryCollection} arguments are not supported.
   *
   * @param other
   *   the <code>Geometry</code> with which to compute the difference return a Geometry representing
   *   the point-set difference of this <code>Geometry</code> with <code>other</code> throws
   *   TopologyException if a robustness error occurs throws IllegalArgumentException if either
   *   input is a non-empty GeometryCollection
   */
  def difference(other: Geometry): Geometry = { // special case: if A.isEmpty ==> empty; if B.isEmpty ==> A
    if (this.isEmpty) return OverlayOp.createEmptyResult(OverlayOp.DIFFERENCE, this, other, factory)
    if (other.isEmpty) return copy
    Geometry.checkNotGeometryCollection(this)
    Geometry.checkNotGeometryCollection(other)
    SnapIfNeededOverlayOp.overlayOp(this, other, OverlayOp.DIFFERENCE)
  }

  /**
   * Computes a <code>Geometry </code> representing the closure of the point-set which is the union
   * of the points in this <code>Geometry</code> which are not contained in the <code>other</code>
   * Geometry, with the points in the <code>other</code> Geometry not contained in this
   * <code>Geometry</code>. If the result is empty, it is an atomic geometry with the dimension of
   * the highest input dimension. <p> Non-empty {link GeometryCollection} arguments are not
   * supported.
   *
   * @param other
   *   the <code>Geometry</code> with which to compute the symmetric difference return a Geometry
   *   representing the point-set symmetric difference of this <code>Geometry</code> with
   *   <code>other</code> throws TopologyException if a robustness error occurs throws
   *   IllegalArgumentException if either input is a non-empty GeometryCollection
   */
  def symDifference(other: Geometry): Geometry = {
    if (this.isEmpty || other.isEmpty) { // both empty - check dimensions
      if (this.isEmpty && other.isEmpty)
        return OverlayOp.createEmptyResult(OverlayOp.SYMDIFFERENCE, this, other, factory)
      // special case: if either input is empty ==> result = other arg
      if (this.isEmpty) return other.copy
      if (other.isEmpty) return copy
    }
    Geometry.checkNotGeometryCollection(this)
    Geometry.checkNotGeometryCollection(other)
    SnapIfNeededOverlayOp.overlayOp(this, other, OverlayOp.SYMDIFFERENCE)
  }

  /**
   * Computes the union of all the elements of this geometry. <p> This method supports {link
   * GeometryCollection}s (which the other overlay operations currently do not). <p> The result
   * obeys the following contract: <ul> <li>Unioning a set of {link LineString}s has the effect of
   * fully noding and dissolving the linework. <li>Unioning a set of {link Polygon}s always returns
   * a {link Polygonal} geometry (unlike {link #union(Geometry)}, which may return geometries of
   * lower dimension if a topology collapse occurred). </ul>
   *
   * return the union geometry throws TopologyException if a robustness error occurs
   * @see
   *   UnaryUnionOp
   */
  def union: Geometry = UnaryUnionOp.union(this)

  /**
   * Returns true if the two <code>Geometry</code>s are exactly equal, up to a specified distance
   * tolerance. Two Geometries are exactly equal within a distance tolerance if and only if: <ul>
   * <li>they have the same structure <li>they have the same values for their vertices, within the
   * given tolerance distance, in exactly the same order. </ul> This method does <i>not</i> test the
   * values of the <code>GeometryFactory</code>, the <code>SRID</code>, or the <code>userData</code>
   * fields. <p> To properly test equality between different geometries, it is usually necessary to
   * {link #normalize()} them first.
   *
   * @param other
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @param tolerance
   *   distance at or below which two <code>Coordinate</code>s are considered equal return
   *   <code>true</code> if this and the other <code>Geometry</code> have identical structure and
   *   point values, up to the distance tolerance.
   * @see
   *   #equalsExact(Geometry)
   * @see
   *   #normalize()
   * @see
   *   #norm()
   */
  def equalsExact(other: Geometry, tolerance: Double): Boolean

  /**
   * Returns true if the two <code>Geometry</code>s are exactly equal. Two Geometries are exactly
   * equal iff: <ul> <li>they have the same structure <li>they have the same values for their
   * vertices, in exactly the same order. </ul> This provides a stricter test of equality than {link
   * #equalsTopo(Geometry)}, which is more useful in certain situations (such as using geometries as
   * keys in collections). <p> This method does <i>not</i> test the values of the
   * <code>GeometryFactory</code>, the <code>SRID</code>, or the <code>userData</code> fields. <p>
   * To properly test equality between different geometries, it is usually necessary to {link
   * #normalize()} them first.
   *
   * @param other
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> return
   *   <code>true</code> if this and the other <code>Geometry</code> have identical structure and
   *   point values.
   * @see
   *   #equalsExact(Geometry, double)
   * @see
   *   #normalize()
   * @see
   *   #norm()
   */
  def equalsExact(other: Geometry): Boolean = (this eq other) || equalsExact(other, 0)

  /**
   * Tests whether two geometries are exactly equal in their normalized forms. This is a convenience
   * method which creates normalized versions of both geometries before computing {link
   * #equalsExact(Geometry)}. <p> This method is relatively expensive to compute. For maximum
   * performance, the client should instead perform normalization on the individual geometries at an
   * appropriate point during processing.
   *
   * @param g
   *   a Geometry return true if the input geometries are exactly equal in their normalized form
   */
  def equalsNorm(g: Geometry): Boolean = {
    if (g == null) return false
    norm.equalsExact(g.norm)
  }

  /**
   * Performs an operation with or on this <code>Geometry</code>'s coordinates. If this method
   * modifies any coordinate values, {link #geometryChanged} must be called to update the geometry
   * state. Note that you cannot use this method to modify this Geometry if its underlying
   * CoordinateSequence's #get method returns a copy of the Coordinate, rather than the actual
   * Coordinate stored (if it even stores Coordinate objects at all).
   *
   * @param filter
   *   the filter to apply to this <code>Geometry</code>'s coordinates
   */
  def applyF(filter: CoordinateFilter): Unit

  /**
   * Performs an operation on the coordinates in this <code>Geometry</code>'s {link
   * CoordinateSequence}s. If the filter reports that a coordinate value has been changed, {link
   * #geometryChanged} will be called automatically.
   *
   * @param filter
   *   the filter to apply
   */
  def applyF(filter: CoordinateSequenceFilter): Unit

  /**
   * Performs an operation with or on this <code>Geometry</code> and its subelement
   * <code>Geometry</code>s (if any). Only GeometryCollections and subclasses have subelement
   * Geometry's.
   *
   * @param filter
   *   the filter to apply to this <code>Geometry</code> (and its children, if it is a
   *   <code>GeometryCollection</code>).
   */
  def applyF(filter: GeometryFilter): Unit

  /**
   * Performs an operation with or on this Geometry and its component Geometry's. Only
   * GeometryCollections and Polygons have component Geometry's; for Polygons they are the
   * LinearRings of the shell and holes.
   *
   * @param filter
   *   the filter to apply to this <code>Geometry</code>.
   */
  def applyF(filter: GeometryComponentFilter): Unit

  /**
   * Creates and returns a full copy of this {link Geometry} object (including all coordinates
   * contained by it). Subclasses are responsible for overriding this method and copying their
   * internal data. Overrides should call this method first.
   *
   * return a clone of this instance
   * @deprecated
   */
  override def clone: AnyRef = try {
    val clone = super.clone.asInstanceOf[Geometry]
    if (clone.envelope != null) clone.envelope = new Envelope(clone.envelope)
    clone
  } catch {
    case _: CloneNotSupportedException =>
      Assert.shouldNeverReachHere()
      null
  }

  /**
   * Creates a deep copy of this {link Geometry} object. Coordinate sequences contained in it are
   * copied. All instance fields are copied (i.e. <code>envelope</code>, <tt>SRID</tt> and
   * <tt>userData</tt>). <p> <b>NOTE:</b> the userData object reference (if present) is copied, but
   * the value itself is not copied. If a deep copy is required this must be performed by the
   * caller.
   *
   * return a deep copy of this geometry
   */
  def copy: Geometry = {
    val copy: Geometry = copyInternal
    copy.envelope =
      if (envelope == null) null
      else envelope.copy
    copy.SRID = this.SRID
    copy.userData = this.userData
    copy
  }

  /**
   * An internal method to copy subclass-specific geometry data.
   *
   * return a copy of the target geometry object.
   */
  protected def copyInternal: Geometry

  /**
   * Converts this <code>Geometry</code> to <b>normal form</b> (or <b> canonical form</b> ). Normal
   * form is a unique representation for <code>Geometry</code> s. It can be used to test whether two
   * <code>Geometry</code>s are equal in a way that is independent of the ordering of the
   * coordinates within them. Normal form equality is a stronger condition than topological
   * equality, but weaker than pointwise equality. The definitions for normal form use the standard
   * lexicographical ordering for coordinates. "Sorted in order of coordinates" means the obvious
   * extension of this ordering to sequences of coordinates. <p> NOTE that this method mutates the
   * value of this geometry in-place. If this is not safe and/or wanted, the geometry should be
   * cloned prior to normalization.
   */
  def normalize(): Unit

  /**
   * Creates a new Geometry which is a normalized copy of this Geometry.
   *
   * return a normalized copy of this geometry.
   * @see
   *   #normalize()
   */
  def norm: Geometry = {
    val cop = copy
    cop.normalize()
    cop
  }

  /**
   * Returns whether this <code>Geometry</code> is greater than, equal to, or less than another
   * <code>Geometry</code>. <P>
   *
   * If their classes are different, they are compared using the following ordering: <UL> <LI> Point
   * (lowest) <LI> MultiPoint <LI> LineString <LI> LinearRing <LI> MultiLineString <LI> Polygon <LI>
   * MultiPolygon <LI> GeometryCollection (highest) </UL> If the two <code>Geometry</code>s have the
   * same class, their first elements are compared. If those are the same, the second elements are
   * compared, etc.
   *
   * @param o
   *   a <code>Geometry</code> with which to compare this <code>Geometry</code> return a positive
   *   number, 0, or a negative number, depending on whether this object is greater than, equal to,
   *   or less than <code>o</code>, as defined in "Normal Form For Geometry" in the JTS Technical
   *   Specifications
   */
  override def compareTo(other: Geometry): Int = {
    if (getSortIndex != other.getSortIndex) return getSortIndex - other.getSortIndex
    if (isEmpty && other.isEmpty) return 0
    if (isEmpty) return -1
    if (other.isEmpty) return 1
    compareToSameClass(other)
  }

  /**
   * Returns whether this <code>Geometry</code> is greater than, equal to, or less than another
   * <code>Geometry</code>, using the given {link CoordinateSequenceComparator}. <P>
   *
   * If their classes are different, they are compared using the following ordering: <UL> <LI> Point
   * (lowest) <LI> MultiPoint <LI> LineString <LI> LinearRing <LI> MultiLineString <LI> Polygon <LI>
   * MultiPolygon <LI> GeometryCollection (highest) </UL> If the two <code>Geometry</code>s have the
   * same class, their first elements are compared. If those are the same, the second elements are
   * compared, etc.
   *
   * @param o
   *   a <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @param comp
   *   a <code>CoordinateSequenceComparator</code> return a positive number, 0, or a negative
   *   number, depending on whether this object is greater than, equal to, or less than
   *   <code>o</code>, as defined in "Normal Form For Geometry" in the JTS Technical Specifications
   */
  def compareTo(other: Geometry, comp: CoordinateSequenceComparator): Int = {
    if (getSortIndex != other.getSortIndex) return getSortIndex - other.getSortIndex
    if (isEmpty && other.isEmpty) return 0
    if (isEmpty) return -1
    if (other.isEmpty) return 1
    compareToSameClass(other, comp)
  }

  /**
   * Returns whether the two <code>Geometry</code>s are equal, from the point of view of the
   * <code>equalsExact</code> method. Called by <code>equalsExact</code> . In general, two
   * <code>Geometry</code> classes are considered to be "equivalent" only if they are the same
   * class. An exception is <code>LineString</code> , which is considered to be equivalent to its
   * subclasses.
   *
   * @param other
   *   the <code>Geometry</code> with which to compare this <code>Geometry</code> for equality
   *   return <code>true</code> if the classes of the two <code>Geometry</code> s are considered to
   *   be equal by the <code>equalsExact</code> method.
   */
  protected def isEquivalentClass(other: Geometry): Boolean =
    this.getClass.getName == other.getClass.getName

  /**
   * Tests whether this is an instance of a general {link GeometryCollection}, rather than a
   * homogeneous subclass.
   *
   * return true if this is a heterogeneous GeometryCollection
   */
  protected def isGeometryCollection: Boolean =
    getSortIndex == Geometry.SORTINDEX_GEOMETRYCOLLECTION

  /**
   * Returns the minimum and maximum x and y values in this <code>Geometry</code> , or a null
   * <code>Envelope</code> if this <code>Geometry</code> is empty. Unlike
   * <code>getEnvelopeInternal</code>, this method calculates the <code>Envelope</code> each time it
   * is called; <code>getEnvelopeInternal</code> caches the result of this method.
   *
   * return this <code>Geometry</code>s bounding box; if the <code>Geometry</code> is empty,
   * <code>Envelope#isNull</code> will return <code>true</code>
   */
  protected def computeEnvelopeInternal: Envelope

  /**
   * Returns whether this <code>Geometry</code> is greater than, equal to, or less than another
   * <code>Geometry</code> having the same class.
   *
   * @param o
   *   a <code>Geometry</code> having the same class as this <code>Geometry</code> return a positive
   *   number, 0, or a negative number, depending on whether this object is greater than, equal to,
   *   or less than <code>o</code>, as defined in "Normal Form For Geometry" in the JTS Technical
   *   Specifications
   */
  protected def compareToSameClass(o: Geometry): Int

  /**
   * Returns whether this <code>Geometry</code> is greater than, equal to, or less than another
   * <code>Geometry</code> of the same class. using the given {link CoordinateSequenceComparator}.
   *
   * @param o
   *   a <code>Geometry</code> having the same class as this <code>Geometry</code>
   * @param comp
   *   a <code>CoordinateSequenceComparator</code> return a positive number, 0, or a negative
   *   number, depending on whether this object is greater than, equal to, or less than
   *   <code>o</code>, as defined in "Normal Form For Geometry" in the JTS Technical Specifications
   */
  def compareToSameClass(o: Geometry, comp: CoordinateSequenceComparator): Int

  /**
   * Returns the first non-zero result of <code>compareTo</code> encountered as the two
   * <code>Collection</code>s are iterated over. If, by the time one of the iterations is complete,
   * no non-zero result has been encountered, returns 0 if the other iteration is also complete. If
   * <code>b</code> completes before <code>a</code>, a positive number is returned; if a before b, a
   * negative number.
   *
   * @param a
   *   a <code>Collection</code> of <code>Comparable</code>s
   * @param b
   *   a <code>Collection</code> of <code>Comparable</code>s return the first non-zero
   *   <code>compareTo</code> result, if any; otherwise, zero
   */
  def compare[A](
    a:          java.util.Collection[A],
    b:          java.util.Collection[A],
    comparator: Comparator[A]
  ): Int = {
    val i = a.iterator
    val j = b.iterator
    while (i.hasNext && j.hasNext) {
      val aElement   = i.next
      val bElement   = j.next
      // This is a BUG on jts upstream
      val comparison = comparator.compare(aElement, bElement)
      if (comparison != 0) return comparison
    }
    if (i.hasNext) return 1
    if (j.hasNext) return -1
    0
  }

  protected def equal(a: Coordinate, b: Coordinate, tolerance: Double): Boolean = {
    if (tolerance == 0) return a == b
    a.distance(b) <= tolerance
  }

  protected def getSortIndex: Int

  private def createPointFromInternalCoord(coord: Coordinate, exemplar: Geometry) = {
    exemplar.getPrecisionModel.makePrecise(coord)
    exemplar.getFactory.createPoint(coord)
  }
}
