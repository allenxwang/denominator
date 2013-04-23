package denominator.dynect;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.GeoResourceRecordSetApi;

@Module(entryPoints = DNSApiManager.class, complete = false)
public class DynECTGeoSupport {

    @Provides
    @Singleton
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(DynECTGeoResourceRecordSetApi.Factory in) {
        return in;
    }
}
