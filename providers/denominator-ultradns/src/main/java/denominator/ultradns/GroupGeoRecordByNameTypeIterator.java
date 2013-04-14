package denominator.ultradns;

import static com.google.common.collect.Iterators.peekingIterator;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.domain.DirectionalRecord;
import org.jclouds.ultradns.ws.domain.DirectionalRecordDetail;
import org.jclouds.ultradns.ws.domain.DirectionalRecordType;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.PeekingIterator;

import denominator.model.Geo;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.ResourceRecordSetWithConfig;

/**
 * Generally, this iterator will produce {@link ResourceRecordSetWithConfig} for
 * only a single record type. However, there are special cases where this can
 * produce multiple. For example, {@link DirectionalRecordType#IPV4} and
 * {@link DirectionalRecordType#IPV6} emit both address ({@code A} or
 * {@code AAAA}) and {@code CNAME} records.
 * 
 * @author adrianc
 * 
 */
class GroupGeoRecordByNameTypeIterator implements Iterator<ResourceRecordSetWithConfig<?>> {

    static final class Factory {
        private final CacheLoader<String, List<String>> directionalGroupIdToTerritories;

        @Inject
        private Factory(CacheLoader<String, List<String>> directionalGroupIdToTerritories) {
            this.directionalGroupIdToTerritories = directionalGroupIdToTerritories;
        }

        /**
         * @param sortedIterator
         *            only contains records with the same
         *            {@link DirectionalRecordDetail#getName()}, sorted by
         *            {@link DirectionalRecord#getType()},
         *            {@link DirectionalRecordDetail#getGeolocationGroup()} or
         *            {@link DirectionalRecordDetail#getGroup()}
         */
        Iterator<ResourceRecordSetWithConfig<?>> create(Iterator<DirectionalRecordDetail> sortedIterator) {
            LoadingCache<String, List<String>> requestScopedGeoCache = CacheBuilder.newBuilder().build(
                    directionalGroupIdToTerritories);
            return new GroupGeoRecordByNameTypeIterator(requestScopedGeoCache, sortedIterator);
        }
    }

    private final Function<String, List<String>> directionalGroupIdToTerritories;
    private final PeekingIterator<DirectionalRecordDetail> peekingIterator;

    private GroupGeoRecordByNameTypeIterator(Function<String, List<String>> directionalGroupIdToTerritories,
            Iterator<DirectionalRecordDetail> sortedIterator) {
        this.directionalGroupIdToTerritories = directionalGroupIdToTerritories;
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    static Optional<IdAndName> group(DirectionalRecordDetail in) {
        return in.getGeolocationGroup().or(in.getGroup());
    }

    @Override
    public ResourceRecordSetWithConfig<?> next() {
        DirectionalRecordDetail directionalRecord = peekingIterator.next();
        DirectionalRecord record = directionalRecord.getRecord();

        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(directionalRecord.getName())
                                                                .type(record.getType())
                                                                .ttl(record.getTTL());

        if (!record.isNoResponseRecord())
            builder.add(forTypeAndRData(record.getType(), record.getRData()));

        IdAndName directionalGroup = group(directionalRecord).get();
        Geo config =  Geo.create(directionalGroup.getName(), 
                                 directionalGroupIdToTerritories.apply(directionalGroup.getId()),
                                 record.isNoResponseRecord());
        
        while (hasNext()) {
            DirectionalRecordDetail next = peekingIterator.peek();
            if (typeTTLAndGeoGroupEquals(next, directionalRecord)) {
                peekingIterator.next();
                builder.add(forTypeAndRData(record.getType(), next.getRecord().getRData()));
            } else {
                break;
            }
        }
        return ResourceRecordSetWithConfig.<Map<String, Object>> builder()
                                          .putConfig("geo", config)
                                          .rrset(builder.build())
                                          .build();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    static boolean typeTTLAndGeoGroupEquals(DirectionalRecordDetail actual, DirectionalRecordDetail expected) {
        return actual.getRecord().getType() == expected.getRecord().getType()
                && actual.getRecord().getTTL() == expected.getRecord().getTTL()
                && group(actual).equals(group(expected));
    }
}