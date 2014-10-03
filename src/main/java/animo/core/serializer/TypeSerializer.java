/**
 * 
 */
package animo.core.serializer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import animo.core.exceptions.SerializationException;

/**
 * This interface defines the necessary methods to serialize and deserialize a
 * type.
 * 
 * @author B. Wanders
 * @param <T> the value type
 */
public interface TypeSerializer<T>
{
    /**
     * Deserializes a value from the given XML node.
     * 
     * @param root the node from which to deserialize
     * @return the value itself
     * @throws SerializationException if the deserialization failed
     */
    T deserialize(Node root) throws SerializationException;

    /**
     * Serializes a type.
     * 
     * @param doc the document to serialize into
     * @param value the value to serialize
     * @return an element that describes the value
     * @throws SerializationException if the serialization failed
     */
    Node serialize(Document doc, Object value) throws SerializationException;
}
