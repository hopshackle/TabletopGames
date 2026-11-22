package core.properties;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import utilities.Hash;

/** Written to JSON with its class name under {@code "class"}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class Property
{
    // Each property has a name associated, which gets hashed for storage in maps.
    protected final String hashString;
    // Hash of property name
    protected final int hashKey;

    /** For JSON: the reader sets the fields from the document. */
    protected Property() {
        this(null, 0);
    }

    public Property(String hashString, int hashKey) {
        this.hashString = hashString;
        this.hashKey = hashKey;
    }

    public Property(String hashString) {
        this.hashString = hashString;
        this.hashKey = Hash.GetInstance().hash(hashString);
    }

    // Getters
    public String getHashString() {return hashString;}
    public int getHashKey() {return  hashKey;}

    /* Methods to be implemented by subclass */

    /**
     * Creates a copy of this property.
     * @return - a new Property object with the same hashString and hashKey.
     */
    protected abstract Property _copy();

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object other);

    @Override
    public int hashCode() { return hashKey; }

    /* Final methods */

    /**
     * Creates a copy of this property.
     * @return - a new Property object with the same hashString and hashKey.
     */
    public final Property copy() {
        return _copy();
    }
}
