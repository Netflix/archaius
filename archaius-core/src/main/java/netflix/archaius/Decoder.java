package netflix.archaius;

/**
 * API for decoding properties to arbitrary types.
 *
 * @author spencergibb
 */
public interface Decoder {

	<T> T decode(Class<T> type, String encoded);
}
