/**
 * 
 */
package animo.core.exceptions;

/**
 * This excpetion is thrown to indicate that something within the ANIMO framework
 * went wrong.
 * 
 * @author B. Wanders
 */
public class AnimoException extends Exception
{

    /**
     * 
     */
    private static final long serialVersionUID = 315015035048039668L;

    /**
     * Constructor with detail message.
     * 
     * @param message the detail message
     */
    public AnimoException(String message)
    {
        super(message);
    }

    /**
     * Constructor with detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public AnimoException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
