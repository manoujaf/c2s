package c2s.mjk;

/**
 * Represents a key-value pair for HTTP request headers and properties.
 * Used in conjunction with C2S class to configure request properties such as
 * authentication headers, content types, and custom headers.
 * This immutable class stores a single request property and provides methods
 * to retrieve the key, value, and a SHA1 hash of the combined key-value pair.
 */
public class RequestProperty {

    /** The property key (e.g., "Authorization", "Content-Type") */
    private final String key;

    /** The property value (e.g., "Bearer token123", "application/json") */
    private final String value;

    /**
     * Constructor for RequestProperty.
     *
     * @param key the property key
     * @param value the property value
     */
    RequestProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Retrieves the property key.
     *
     * @return the key string
     */
    public String getKey() {
        return key;
    }

    /**
     * Retrieves the property value.
     *
     * @return the value string
     */
    public String getValue() {
        return value;
    }

}