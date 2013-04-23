package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Multimaps.filterValues;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.Geos.groupNameEqualTo;
import static denominator.model.ResourceRecordSetWithConfigs.configTypeEqualTo;
import static denominator.model.ResourceRecordSetWithConfigs.toConfig;
import static denominator.model.ResourceRecordSetWithConfigs.toResourceRecordSet;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.GeoResourceRecordSetApi;
import denominator.model.Geo;
import denominator.model.ResourceRecordSetWithConfig;

public final class MockGeoResourceRecordSetApi implements denominator.GeoResourceRecordSetApi {
    public static final class Factory implements denominator.GeoResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSetWithConfig<?>> data;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSetWithConfig> data) {
            this.data = Multimap.class.cast(filterValues(Multimap.class.cast(data), configTypeEqualTo(Geo.class)));
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String zoneName) {
            checkArgument(data.keySet().contains(zoneName), "zone %s not found", zoneName);
            return Optional.<GeoResourceRecordSetApi> of(new MockGeoResourceRecordSetApi(data, zoneName));
        }
    }

    private final Multimap<String, ResourceRecordSetWithConfig<?>> data;
    private final String zoneName;

    MockGeoResourceRecordSetApi(Multimap<String, ResourceRecordSetWithConfig<?>> data, String zoneName) {
        this.data = data;
        this.zoneName = zoneName;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSetWithConfig<?>> list() {
        return from(data.get(zoneName))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Iterator<ResourceRecordSetWithConfig<?>> listByName(String name) {
        checkNotNull(name, "name");
        return from(data.get(zoneName))
                .filter(compose(nameEqualTo(name), toResourceRecordSet()))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Iterator<ResourceRecordSetWithConfig<?>> listByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(data.get(zoneName))
                .filter(nameAndTypeEqualTo(name, type))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Optional<ResourceRecordSetWithConfig<?>> getByNameTypeAndGroup(String name, String type, String group) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(data.get(zoneName))
                .firstMatch(and(nameAndTypeEqualTo(name, type), geoGroupNameEqualTo(group)));
    }

    private static Predicate<ResourceRecordSetWithConfig<?>> geoGroupNameEqualTo(String group) {
        return compose(groupNameEqualTo(group), toConfig(Geo.class));
    }

    private static Predicate<ResourceRecordSetWithConfig<?>> nameAndTypeEqualTo(String name, String type) {
        return compose(and(nameEqualTo(name), typeEqualTo(type)), toResourceRecordSet());
    }
}