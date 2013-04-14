package denominator.mock;

import static com.google.common.collect.Multimaps.synchronizedListMultimap;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ns;

import java.util.Map;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.GeoResourceRecordSetApi;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.NothingToClose;
import denominator.model.Geo;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSetWithConfig;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.SOAData;

/**
 * in-memory {@code Provider}, used for testing.
 */
@Module(entryPoints = DNSApiManager.class, includes = NothingToClose.class)
public class MockProvider extends Provider {

    @Provides
    protected Provider provideThis() {
        return this;
    }

    @Provides
    ZoneApi provideZoneApi(MockZoneApi in) {
        return in;
    }

    @Provides
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
        return in;
    }

    @Provides
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(MockGeoResourceRecordSetApi.Factory in) {
        return in;
    }

    // wildcard types are not currently injectable in dagger
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Provides
    @Singleton
    Multimap<String, ResourceRecordSet> provideData() {
        String zoneName = "denominator.io.";
        ListMultimap<String, ResourceRecordSet<?>> data = LinkedListMultimap.create();
        data = synchronizedListMultimap(data);
        data.put(zoneName, ResourceRecordSet.builder()
                                            .type("SOA")
                                            .name(zoneName)
                                            .ttl(3600)
                                            .add(SOAData.builder()
                                                        .mname("ns1." + zoneName)
                                                        .rname("admin." + zoneName)
                                                        .serial(1)
                                                        .refresh(3600)
                                                        .retry(600)
                                                        .expire(604800)
                                                        .minimum(60).build()).build());
        data.put(zoneName, ns(zoneName, 86400, "ns1." + zoneName));
        data.put(zoneName, a("www1." + zoneName, 3600, ImmutableSet.of("1.1.1.1", "1.1.1.2")));
        data.put(zoneName, a("www2." + zoneName, 3600, "2.2.2.2"));
        data.put(zoneName, cname("www." + zoneName, 3600, "www1." + zoneName));
        return Multimap.class.cast(data);
    }
    
    // wildcard types are not currently injectable in dagger
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Provides
    @Singleton
    Multimap<String, ResourceRecordSetWithConfig> provideSpecialData() {
        String zoneName = "denominator.io.";
        ListMultimap<String, ResourceRecordSetWithConfig<Map<String, Object>>> data = LinkedListMultimap.create();
        data = synchronizedListMultimap(data);
        data.put(zoneName, ResourceRecordSetWithConfig.<Map<String, Object>> builder()
                .putConfig("geo", Geo.create("alazona", ImmutableList.of("Alaska", "Arizona"), false))
                .rrset(ResourceRecordSet.<Map<String, Object>>builder()
                                        .name("www2.geo.denominator.io.")
                                        .type("A")
                                        .ttl(300)
                                        .add(AData.create("1.1.1.1")).build())
                .build());
        data.put(zoneName, ResourceRecordSetWithConfig.<Map<String, Object>> builder()
                .putConfig("geo", Geo.create("alazona", ImmutableList.of("Alaska", "Arizona"), false))
                .rrset(ResourceRecordSet.<Map<String, Object>>builder()
                                        .name("www.geo.denominator.io.")
                                        .type("CNAME")
                                        .ttl(300)
                                        .add(CNAMEData.create("a.denominator.io.")).build())
                .build());
        data.put(zoneName, ResourceRecordSetWithConfig.<Map<String, Object>> builder()
                .putConfig("geo", Geo.create("columbador", ImmutableList.of("Colombia", "Ecuador"), false))
                .rrset(ResourceRecordSet.<Map<String, Object>>builder()
                                        .name("www.geo.denominator.io.")
                                        .type("CNAME")
                                        .ttl(86400)
                                        .add(CNAMEData.create("b.denominator.io.")).build())
                .build());
        // no answers for antarctica
        data.put(zoneName, ResourceRecordSetWithConfig.<Map<String, Object>> builder()
                .putConfig("geo", Geo.create("antarctica", ImmutableList.<String> builder()
                                                    .add("Bouvet Island")
                                                    .add("French Southern Territories")
                                                    .add("Antarctica")
                                                    .build(),
                                   true))
                .rrset(ResourceRecordSet.<Map<String, Object>>builder()
                                        .name("www.geo.denominator.io.")
                                        .type("CNAME")
                                        .ttl(0).build())                                   
                .build());
        return Multimap.class.cast(data);
    }
}