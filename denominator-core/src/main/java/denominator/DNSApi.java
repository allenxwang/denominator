package denominator;

import javax.inject.Inject;

import com.google.common.base.Optional;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
 */
public class DNSApi {
    private final ZoneApi zoneApi;
    private final ResourceRecordSetApi.Factory rrsetApiFactory;
    private final ReadOnlyResourceRecordSetApi.Factory rrsetWithConfigApiFactory;
    private final GeoResourceRecordSetApi.Factory geoApiFactory;

    @Inject
    DNSApi(ZoneApi zoneApi, ResourceRecordSetApi.Factory rrsetApiFactory,
            ReadOnlyResourceRecordSetApi.Factory rrsetWithConfigApiFactory,
            GeoResourceRecordSetApi.Factory geoApiFactory) {
        this.zoneApi = zoneApi;
        this.rrsetApiFactory = rrsetApiFactory;
        this.rrsetWithConfigApiFactory = rrsetWithConfigApiFactory;
        this.geoApiFactory = geoApiFactory;
    }

    /**
     * controls DNS zones, such as {@code netflix.com.}, availing information
     * about name servers and extended configuration.
     */
    public ZoneApi getZoneApi() {
        return zoneApi;
    }

    /**
     * controls DNS records as a set,
     */
    public ResourceRecordSetApi getResourceRecordSetApiForZone(String zoneName) {
        return rrsetApiFactory.create(zoneName);
    }

    /**
     * allows you to list all resource record sets regardless of their configuration.
     */
    public ReadOnlyResourceRecordSetApi getReadOnlyResourceRecordSetApiForZone(String zoneName) {
        return rrsetWithConfigApiFactory.create(zoneName);
    }

    /**
     * controls DNS records which take into consideration the territory of the
     * caller. These are otherwise known as Directional records.
     */
    public Optional<GeoResourceRecordSetApi> getGeoResourceRecordSetApiForZone(String zoneName) {
        return geoApiFactory.create(zoneName);
    }
}
