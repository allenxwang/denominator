package denominator.ultradns;

import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.GeoResourceRecordSetApi;

@Module(entryPoints = DNSApiManager.class, complete = false)
public class UltraDNSGeoSupport {

    @Provides
    @Singleton
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(UltraDNSGeoResourceRecordSetApi.Factory in) {
        return in;
    }

    @Provides
    @Singleton
    CacheLoader<String, List<String>> directionalGroupIdToTerritories(final UltraDNSWSApi api,
            final Supplier<IdAndName> account) {
        return new CacheLoader<String, List<String>>() {

            @Override
            public List<String> load(String key) throws Exception {
                return ImmutableList.copyOf(api.getDirectionalGroupApiForAccount(account.get().getId()).get(key)
                        .values());
            }

        };
    }
}
