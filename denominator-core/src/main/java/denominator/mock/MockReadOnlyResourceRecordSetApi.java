package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.ReadOnlyResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

public class MockReadOnlyResourceRecordSetApi implements denominator.ReadOnlyResourceRecordSetApi {

    protected final Multimap<String, ResourceRecordSet<?>> data;
    protected final String zoneName;

    MockReadOnlyResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> data, String zoneName) {
        this.data = data;
        this.zoneName = zoneName;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return from(data.get(zoneName))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        checkNotNull(name, "name");
        return from(data.get(zoneName))
                .filter(nameEqualTo(name))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(data.get(zoneName))
                .filter(nameAndTypeEqualTo(name, type))
                .toSortedList(usingToString())
                .iterator();
    }

    static Predicate<ResourceRecordSet<?>> nameAndTypeEqualTo(String name, String type) {
        return and(nameEqualTo(name), typeEqualTo(type));
    }

    static class Factory implements denominator.ReadOnlyResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> data;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> data) {
            this.data = Multimap.class.cast(data);
        }

        @Override
        public ReadOnlyResourceRecordSetApi create(String zoneName) {
            checkArgument(data.keySet().contains(zoneName), "zone %s not found", zoneName);
            return new MockReadOnlyResourceRecordSetApi(data, zoneName);
        }
    }
}
