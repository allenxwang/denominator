package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.ultradns.UltraDNSPredicates.isGeolocationPool;
import static org.jclouds.ultradns.ws.domain.DirectionalRecordType.IPV4;
import static org.jclouds.ultradns.ws.domain.DirectionalRecordType.IPV6;

import java.util.EnumSet;
import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.DirectionalGroupCoordinates;
import org.jclouds.ultradns.ws.domain.DirectionalPool;
import org.jclouds.ultradns.ws.domain.DirectionalRecordDetail;
import org.jclouds.ultradns.ws.domain.DirectionalRecordType;
import org.jclouds.ultradns.ws.domain.IdAndName;
import org.jclouds.ultradns.ws.features.DirectionalGroupApi;
import org.jclouds.ultradns.ws.features.DirectionalPoolApi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import denominator.GeoResourceRecordSetApi;
import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSetWithConfig;

public final class UltraDNSGeoResourceRecordSetApi implements denominator.GeoResourceRecordSetApi {

    private final DirectionalGroupApi groupApi;
    private final DirectionalPoolApi poolApi;
    private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;
    private final String zoneName;

    UltraDNSGeoResourceRecordSetApi(DirectionalGroupApi groupApi, DirectionalPoolApi poolApi,
            GroupGeoRecordByNameTypeIterator.Factory iteratorFactory, String zoneName) {
        this.groupApi = groupApi;
        this.poolApi = poolApi;
        this.iteratorFactory = iteratorFactory;
        this.zoneName = zoneName;
    }

    @Override
    public Iterator<ResourceRecordSetWithConfig<?>> list() {
        return concat(poolApi.list().filter(isGeolocationPool())
                .transform(new Function<DirectionalPool, Iterator<ResourceRecordSetWithConfig<?>>>() {
                    @Override
                    public Iterator<ResourceRecordSetWithConfig<?>> apply(DirectionalPool pool) {
                        return allByName(pool.getName());
                    }

                }).iterator());
    }

    private Iterator<ResourceRecordSetWithConfig<?>> allByName(final String name) {
        return concat(transform(EnumSet.allOf(DirectionalRecordType.class).iterator(),
                new Function<DirectionalRecordType, Iterator<ResourceRecordSetWithConfig<?>>>() {

                    @Override
                    public Iterator<ResourceRecordSetWithConfig<?>> apply(DirectionalRecordType input) {
                        return iteratorForNameAndDirectionalType(name, input);
                    }

                }));
    }

    @Override
    public Iterator<ResourceRecordSetWithConfig<?>> listByName(String name) {
        return allByName(checkNotNull(name, "name"));
    }

    @Override
    public Iterator<ResourceRecordSetWithConfig<?>> listByNameAndType(String name, final String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        if ("CNAME".equals(type)) {
            // retain original type (this will filter out A, AAAA)
            return filter(
                    concat(iteratorForNameAndDirectionalType(name, IPV4), iteratorForNameAndDirectionalType(name, IPV6)),
                    compose(typeEqualTo(type), ToResourceRecordSet.INSTANCE));
        } else if ("A".equals(type) || "AAAA".equals(type)) {
            DirectionalRecordType dirType = "AAAA".equals(type) ? IPV6 : IPV4;
            Iterator<ResourceRecordSetWithConfig<?>> iterator = iteratorForNameAndDirectionalType(name, dirType);
            // retain original type (this will filter out CNAMEs)
            return filter(iterator, compose(typeEqualTo(type), ToResourceRecordSet.INSTANCE));
        } else {
            return iteratorForNameAndDirectionalType(name, DirectionalRecordType.valueOf(type));
        }
    }

    private static enum IsCNAME implements Predicate<DirectionalRecordDetail> {
        INSTANCE;

        @Override
        public boolean apply(DirectionalRecordDetail input) {
            return "CNAME".equals(input.getRecord().getType());
        }

    }
    @Override
    public Optional<ResourceRecordSetWithConfig<?>> getByNameTypeAndGroup(String name, String type,
            String groupName) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(groupName, "groupName");
        Iterator<DirectionalRecordDetail> records;
        if ("CNAME".equals(type)) {
            records = filter(
                    concat(recordsForNameTypeAndGroup(name, "A", groupName),
                            recordsForNameTypeAndGroup(name, "AAAA", groupName)), IsCNAME.INSTANCE);
        } else {
            records = recordsForNameTypeAndGroup(name, type, groupName);
        }
        Iterator<ResourceRecordSetWithConfig<?>> iterator = iteratorFactory.create(records);
        if (iterator.hasNext())
            return Optional.<ResourceRecordSetWithConfig<?>> of(iterator.next());
        return Optional.absent();
    }

    private Iterator<DirectionalRecordDetail> recordsForNameTypeAndGroup(String name, String type, String groupName) {
        int typeValue = checkNotNull(new ResourceTypeToValue().get(type), "typeValue for %s", type);
        DirectionalGroupCoordinates group = DirectionalGroupCoordinates.builder()
                                                                       .zoneName(zoneName)
                                                                       .recordName(name)
                                                                       .recordType(typeValue)
                                                                       .groupName(groupName).build();
        return groupApi.listRecordsByGroupCoordinates(group).iterator();
    }

    private Iterator<ResourceRecordSetWithConfig<?>> iteratorForNameAndDirectionalType(String name,
            DirectionalRecordType dirType) {
        return iteratorFactory.create(poolApi.listRecordsByNameAndType(name, dirType.getCode())
                .toSortedList(byTypeAndGeoGroup).iterator());
    }

    static Optional<IdAndName> group(DirectionalRecordDetail in) {
        return in.getGeolocationGroup().or(in.getGroup());
    }

    private static final Ordering<DirectionalRecordDetail> byTypeAndGeoGroup = new Ordering<DirectionalRecordDetail>() {

        @Override
        public int compare(DirectionalRecordDetail left, DirectionalRecordDetail right) {
            checkState(group(left).isPresent(), "expected record to be in a geolocation group: %s", left);
            checkState(group(right).isPresent(), "expected record to be in a geolocation group: %s", right);
            return ComparisonChain.start().compare(left.getRecord().getType(), right.getRecord().getType())
                    .compare(group(left).get().getName(), group(right).get().getName()).result();
        }
    };

    private static enum ToResourceRecordSet implements
            Function<ResourceRecordSetWithConfig<?>, ResourceRecordSet<?>> {
        INSTANCE;

        @Override
        public ResourceRecordSet<?> apply(ResourceRecordSetWithConfig<?> input) {
            return input.getResourceRecordSet();
        }
    }

    static final class Factory implements denominator.GeoResourceRecordSetApi.Factory {
        private final UltraDNSWSApi api;
        private final Supplier<IdAndName> account;
        private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;

        @Inject
        Factory(UltraDNSWSApi api, Supplier<IdAndName> account, GroupGeoRecordByNameTypeIterator.Factory iteratorFactory) {
            this.api = api;
            this.account = account;
            this.iteratorFactory = iteratorFactory;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(final String zoneName) {
            checkNotNull(zoneName, "zoneName was null");
            return Optional.<GeoResourceRecordSetApi> of(new UltraDNSGeoResourceRecordSetApi(api
                    .getDirectionalGroupApiForAccount(account.get().getId()), api
                    .getDirectionalPoolApiForZone(zoneName), iteratorFactory, zoneName));
        }
    }

}
