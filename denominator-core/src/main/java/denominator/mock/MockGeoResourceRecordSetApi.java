package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.configContainsType;
import static denominator.model.ResourceRecordSets.toConfig;
import static denominator.model.config.Geos.groupNameEqualTo;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.GeoResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.config.Geo;

public final class MockGeoResourceRecordSetApi extends MockReadOnlyResourceRecordSetApi implements denominator.GeoResourceRecordSetApi {

    MockGeoResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> data, String zoneName) {
        super(data, zoneName);
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(type, "group");
        return from(data.get(zoneName))
                .firstMatch(and(nameAndTypeEqualTo(name, type), geoGroupNameEqualTo(group)));
    }

    private static Predicate<ResourceRecordSet<?>> geoGroupNameEqualTo(String group) {
        return compose(groupNameEqualTo(group), toConfig(Geo.class));
    }

    public static final class Factory implements denominator.GeoResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> data;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> data) {
            this.data = Multimap.class.cast(filterValues(Multimap.class.cast(data), configContainsType(Geo.class)));
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String zoneName) {
            checkArgument(data.keySet().contains(zoneName), "zone %s not found", zoneName);
            return Optional.<GeoResourceRecordSetApi> of(new MockGeoResourceRecordSetApi(data, zoneName));
        }
    }
}
