package denominator.model;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

public class ResourceRecordSetWithConfig<D extends Map<String, Object>> extends
        ForwardingMap<String, Map<String, Object>> {

    private final ResourceRecordSet<D> rrset;
    private final Map<String, Map<String, Object>> config;

    @ConstructorProperties({ "rrset", "config" })
    private ResourceRecordSetWithConfig(ResourceRecordSet<D> rrset, Map<String, Map<String, Object>> config) {
        this.rrset = checkNotNull(rrset, "rrset");
        this.config = checkNotNull(config, "config");
    }

    public ResourceRecordSet<D> getResourceRecordSet() {
        return rrset;
    }

    public Map<String, Map<String, Object>> getConfig() {
        return config;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rrset, config);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ResourceRecordSetWithConfig))
            return false;
        ResourceRecordSetWithConfig<?> that = ResourceRecordSetWithConfig.class.cast(obj);
        return equal(this.rrset, that.rrset) && equal(this.config, that.config);
    }

    @Override
    public String toString() {
        return toStringHelper(this).omitNullValues().add("rrset", rrset).add("config", config).toString();
    }

    public static <D extends Map<String, Object>> Builder<D> builder() {
        return new Builder<D>();
    }

    public static class Builder<D extends Map<String, Object>> {

        private ResourceRecordSet<D> rrset;
        private ImmutableMap.Builder<String, Map<String, Object>> config = ImmutableMap.builder();

        /**
         * @see ResourceRecordSetWithConfig#getResourceRecordSet()
         */
        public Builder<D> rrset(ResourceRecordSet<D> rrset) {
            this.rrset = checkNotNull(rrset, "rrset");
            return this;
        }

        /**
         * adds a value to the builder.
         * 
         * ex.
         * 
         * <pre>
         * builder.putConfig("geo", geo);
         * </pre>
         */
        public Builder<D> putConfig(String key, Map<String, Object> config) {
            this.config.put(checkNotNull(key, "key"), checkNotNull(config, "config"));
            return this;
        }

        /**
         * adds config values in the builder
         * 
         * ex.
         * 
         * <pre>
         * 
         * builder.putAllConfig(otherConfig);
         * </pre>
         */
        public Builder<D> putAllConfig(Map<String, Map<String, Object>> config) {
            this.config.putAll(checkNotNull(config, "config"));
            return this;
        }

        /**
         * @see ResourceRecordSetWithConfig#getConfig()
         */
        public Builder<D> config(Map<String, Map<String, Object>> config) {
            this.config = ImmutableMap.<String, Map<String, Object>> builder().putAll(config);
            return this;
        }

        public ResourceRecordSetWithConfig<D> build() {
            return new ResourceRecordSetWithConfig<D>(rrset, config.build());
        }
    }

    @Override
    protected Map<String, Map<String, Object>> delegate() {
        return config;
    }
}
