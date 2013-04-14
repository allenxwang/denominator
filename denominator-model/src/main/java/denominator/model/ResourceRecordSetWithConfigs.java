package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Static utility methods that build {@code ResourceRecordSetWithConfig}
 * instances.
 * 
 */
public class ResourceRecordSetWithConfigs {

    private ResourceRecordSetWithConfigs() {
    }

    /**
     * returns true if {@link ResourceRecordSetWithConfig#getConfig() config},
     * if matches the input {@code configType} and is not null;
     * 
     * @param configType
     *            expected type of configuration
     */
    public static Predicate<ResourceRecordSetWithConfig<?>> configTypeEqualTo(Class<?> configType) {
        return new ConfigTypeEqualToPredicate(configType);
    }

    private static final class ConfigTypeEqualToPredicate implements Predicate<ResourceRecordSetWithConfig<?>> {
        private final Class<?> configType;

        private ConfigTypeEqualToPredicate(Class<?> configType) {
            this.configType = checkNotNull(configType, "configType");
        }

        @Override
        public boolean apply(ResourceRecordSetWithConfig<?> input) {
            if (input == null)
                return false;
            if (input.getConfig().isEmpty())
                return false;
            for (Map<String, Object> config : input.getConfig().values()) {
                if (configType.isAssignableFrom(config.getClass()))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "ConfigTypeEqualTo(" + configType + ")";
        }
    }

    /**
     * returns value of {@link ResourceRecordSetWithConfig#getConfig() config},
     * if matches the input {@code configType} and is not null;
     * 
     * @param configType
     *            expected type of configuration
     */
    public static <C extends Map<String, Object>> Function<ResourceRecordSetWithConfig<?>, C> toConfig(
            Class<C> configType) {
        return new ToConfigFunction<C>(configType);
    }

    private static final class ToConfigFunction<C> implements Function<ResourceRecordSetWithConfig<?>, C> {
        private final Class<C> configType;

        private ToConfigFunction(Class<C> configType) {
            this.configType = checkNotNull(configType, "configType");
        }

        @Override
        public C apply(ResourceRecordSetWithConfig<?> input) {
            if (input == null)
                return null;
            if (input.getConfig().isEmpty())
                return null;
            for (Map<String, Object> config : input.getConfig().values()) {
                if (configType.isAssignableFrom(config.getClass()))
                    return configType.cast(config);
            }
            return null;
        }

        @Override
        public String toString() {
            return "ToConfig(" + configType + ")";
        }
    }

    /**
     * returns value of
     * {@link ResourceRecordSetWithConfig#getResourceRecordSet() rrset}
     */
    public static Function<ResourceRecordSetWithConfig<?>, ResourceRecordSet<?>> toResourceRecordSet() {
        return ToResourceRecordSetFunction.INSTANCE;
    }

    private static enum ToResourceRecordSetFunction implements
            Function<ResourceRecordSetWithConfig<?>, ResourceRecordSet<?>> {
        INSTANCE;

        @Override
        public ResourceRecordSet<?> apply(ResourceRecordSetWithConfig<?> input) {
            if (input == null)
                return null;
            return input.getResourceRecordSet();
        }

        @Override
        public String toString() {
            return "ToResourceRecordSet()";
        }
    }
}
