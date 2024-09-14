package sprouts;

/**
 *  A subscriber may either be an {@link Observer} or an {@link Action}
 *  and it is used as a marker interface to allow for both types to be
 *  identified, grouped and used interchangeably.
 *  This interface does not have any methods and is only used for type checking.
 *  <p>
 *  <b>
 *      Do not implement this interface directly!
 *      Instead, implement either {@link Observer} or {@link Action}.
 * </b><br>
 * In future versions of Java, this interface may be made sealed to prevent
 * other classes from implementing it.
 */
public /*sealed*/ interface Subscriber /* permits Observer, Action */
{
    // This is a marker interface
}
