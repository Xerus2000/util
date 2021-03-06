package xerus.ktutil.helpers

open class Rater<X> constructor(
	/** the current object of this Rater */
	@JvmField
	var obj: X?,
	/** the points to the current [obj] of this Rater */
	@JvmField
	var points: Double,
	/** if true, then objects with less points will be preferred */
	private val inverted: Boolean = false): Comparable<Rater<X>> {
	
	constructor(invert: Boolean = false): this(null, originalPoints(invert), invert)
	
	fun hasObj() = obj != null
	
	fun clear() {
		points = originalPoints(inverted)
		obj = null
	}
	
	/** replaces the object if the given points are higher than the saved ones
	 * @return if [other] became the current [obj] */
	open fun update(other: X?, otherPoints: Double) = update(otherPoints) { other }
	
	open fun update(otherPoints: Double, newValue: () -> X?): Boolean {
		if(!inverted && otherPoints > points || inverted && otherPoints < points) {
			obj = newValue()
			points = otherPoints
			return true
		}
		return false
	}
	
	fun update(other: Rater<X>, multiplier: Double = 1.0) = update(other.obj, other.points * multiplier)
	
	/** Compares the [points] of this Rater to the points of [other]
	 *
	 * **Note** If this returns 0, the obj's could still differ */
	override fun compareTo(other: Rater<X>) = points.compareTo(other.points)
	
	override fun toString() = "%s - Points: %.2f".format(obj, points)
	
	/** Checks if [points] and [obj] of this Rater and [other] are equal */
	override fun equals(other: Any?) =
		other is Rater<*> && other.points == this.points && other.obj == obj
	
	override fun hashCode() = (obj?.hashCode() ?: 0) * 17 + points.hashCode()
	
	companion object {
		private fun originalPoints(inverted: Boolean) = if(inverted) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
	}
	
}