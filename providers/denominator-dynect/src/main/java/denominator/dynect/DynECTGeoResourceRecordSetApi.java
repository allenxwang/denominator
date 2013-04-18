package denominator.dynect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.filter;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.domain.GeoRegionGroup;
import org.jclouds.dynect.v3.domain.GeoService;
import org.jclouds.dynect.v3.domain.Node;
import org.jclouds.dynect.v3.domain.RecordSet;
import org.jclouds.dynect.v3.domain.RecordSet.Value;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import denominator.GeoResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.config.Geo;

public final class DynECTGeoResourceRecordSetApi implements denominator.GeoResourceRecordSetApi {
    static final class Factory implements denominator.GeoResourceRecordSetApi.Factory {
        private final DynECTApi api;

        @Inject
        Factory(DynECTApi api) {
            this.api = api;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String zoneName) {
            checkNotNull(zoneName, "zoneName was null");
            return Optional.<GeoResourceRecordSetApi> of(new DynECTGeoResourceRecordSetApi(api, zoneName));
        }
    }

    private final DynECTApi api;
    private final String zoneFQDN;

    DynECTGeoResourceRecordSetApi(DynECTApi api, String zoneFQDN) {
        this.api = api;
        this.zoneFQDN = zoneFQDN;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        Predicate<Node> nodeFilter = zoneNameEqualTo(zoneFQDN);
        Predicate<GeoRegionGroup> geoGroupFilter = alwaysTrue();
        Predicate<RecordSet> rsetFilter = alwaysTrue();
        return transform(nodeFilter, geoGroupFilter, rsetFilter).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String fqdn) {
        checkNotNull(fqdn, "fqdn was null");
        Predicate<Node> nodeFilter = equalTo(Node.create(zoneFQDN, fqdn));
        Predicate<GeoRegionGroup> geoGroupFilter = alwaysTrue();
        Predicate<RecordSet> rsetFilter = alwaysTrue();
        return transform(nodeFilter, geoGroupFilter, rsetFilter).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String fqdn, String type) {
        checkNotNull(fqdn, "fqdn was null");
        checkNotNull(type, "type was null");
        Predicate<Node> nodeFilter = equalTo(Node.create(zoneFQDN, fqdn));
        Predicate<GeoRegionGroup> geoGroupFilter = alwaysTrue();
        Predicate<RecordSet> rsetFilter = typeEqualTo(type);
        return transform(nodeFilter, geoGroupFilter, rsetFilter).iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String fqdn, String type, String group) {
        checkNotNull(fqdn, "fqdn was null");
        checkNotNull(type, "type was null");
        checkNotNull(group, "group was null");
        Predicate<Node> nodeFilter = equalTo(Node.create(zoneFQDN, fqdn));
        Predicate<GeoRegionGroup> geoGroupFilter = nameEqualTo(group);
        Predicate<RecordSet> rsetFilter = typeEqualTo(type);
        return transform(nodeFilter, geoGroupFilter, rsetFilter).first();
    }

    /**
     * {@link GeoService} are an aggregation of nodes, which may not be in the
     * current zone. We need to filter out those not in the zone from our
     * results.
     */
    private FluentIterable<ResourceRecordSet<?>> transform(Predicate<Node> nodeFilter,
            Predicate<GeoRegionGroup> geoGroupFilter, Predicate<RecordSet> rsetFilter) {
        return api.getGeoServiceApi().list()
                  .transform(geoServiceById)
                  .filter(nodesMatching(nodeFilter))
                  .transformAndConcat(toIterator(nodeFilter, geoGroupFilter, rsetFilter));
    }

    private static Function<GeoService, Iterable<ResourceRecordSet<?>>> toIterator(
            final Predicate<Node> nodeFilter, final Predicate<GeoRegionGroup> groupFilter,
            final Predicate<RecordSet> rsetFilter) {
        return new Function<GeoService, Iterable<ResourceRecordSet<?>>>() {

            @Override
            public Iterable<ResourceRecordSet<?>> apply(GeoService input) {
                ImmutableList.Builder<ResourceRecordSet<?>> toReturn = ImmutableList.builder();
                Iterable<Node> nodes = filter(input.getNodes(), nodeFilter);
                for (GeoRegionGroup group : filter(input.getGroups(), groupFilter)) {
                    Geo geo = Geo.create(group.getName(), group.getCountries());
                    toReturn.addAll(FluentIterable.from(group.getRecordSets())
                                                  .filter(rsetFilter)
                                                  .transform(toResourceRecordSetBuilder(geo))
                                                  .transformAndConcat(buildForEachNodeFQDN(nodes)));
                }
                return toReturn.build();
            }
        };
    }

    private final Function<String, GeoService> geoServiceById = new Function<String, GeoService>() {
        @Override
        public GeoService apply(String input) {
            return api.getGeoServiceApi().get(input);
        }
    };

    private static Predicate<GeoRegionGroup> nameEqualTo(final String group) {
        return new Predicate<GeoRegionGroup>() {

            @Override
            public boolean apply(GeoRegionGroup input) {
                return group.equals(input.getName());
            }

        };
    }

    private static Predicate<GeoService> nodesMatching(final Predicate<Node> nodeFilter) {
        return new Predicate<GeoService>() {
            @Override
            public boolean apply(GeoService input) {
                return any(input.getNodes(), nodeFilter);
            }
        };
    }

    private static Predicate<Node> zoneNameEqualTo(final String zoneName) {
        return new Predicate<Node>() {
            @Override
            public boolean apply(Node input) {
                return zoneName.equals(input.getZone());
            }
        };
    }

    private static Predicate<RecordSet> typeEqualTo(final String type) {
        return new Predicate<RecordSet>() {
            @Override
            public boolean apply(RecordSet input) {
                return type.equals(input.getType());
            }
        };
    }

    private static Function<RecordSet, ResourceRecordSet.Builder<Map<String, Object>>> toResourceRecordSetBuilder(Geo geo){
        return new ToResourceRecordSetBuilder(geo);
    }

    /**
     * the dynect {@code RecordSet} doesn't include
     * {@link ResourceRecordSet#getName()}. This collects all details except the
     * name. The result of this function would be applied to all relevant
     * {@link GeoService#getNodes() nodes}.
     * 
     */
    private static class ToResourceRecordSetBuilder implements
            Function<RecordSet, ResourceRecordSet.Builder<Map<String, Object>>> {
        private final Geo geo;

        public ToResourceRecordSetBuilder(Geo geo) {
            this.geo = geo;
        }

        @Override
        public Builder<Map<String, Object>> apply(RecordSet in) {
            ResourceRecordSet.Builder<Map<String, Object>> builder = ResourceRecordSet.builder();
            builder.type(in.getType());
            builder.ttl(in.getTTL());
            for (Value val : in) {
                builder.add(val.getRData());
            }
            builder.addConfig(geo);
            return builder;
        }

    }

    private static BuildForEachNodeFQDN buildForEachNodeFQDN(Iterable<Node> nodes) {
        return new BuildForEachNodeFQDN(nodes);
    }

    private static class BuildForEachNodeFQDN
            implements
            Function<ResourceRecordSet.Builder<Map<String, Object>>, Iterable<ResourceRecordSet<Map<String, Object>>>> {
        private BuildForEachNodeFQDN(Iterable<Node> nodes) {
            this.nodes = nodes;
        }

        private final Iterable<Node> nodes;

        @Override
        public Iterable<ResourceRecordSet<Map<String, Object>>> apply(
                ResourceRecordSet.Builder<Map<String, Object>> in) {
            ImmutableList.Builder<ResourceRecordSet<Map<String, Object>>> rrsets = ImmutableList.builder();
            for (Node node : nodes) {
                rrsets.add(in.name(node.getFQDN()).build());
            }
            return rrsets.build();
        }
    }
}
