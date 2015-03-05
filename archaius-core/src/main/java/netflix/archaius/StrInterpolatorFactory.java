package netflix.archaius;

/**
 * SPI for specifying the {@link StrInterpolator} type used by ConfigManager.
 * This factory exists since the root config doesn't exist yet when the 
 * interpolator type is added to the {@link DefaultAppConfig#Builder}
 * 
 * @author elandau
 *
 */
public interface StrInterpolatorFactory {
    StrInterpolator create(Config rootConfig);
}
