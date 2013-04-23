package denominator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSetWithConfigs.toConfig;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import denominator.model.Geo;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSetWithConfig;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseGeoReadOnlyLiveTest extends BaseProviderLiveTest {

    @Test
    public void testListZones() {
        skipIfNoCredentials();
        int zoneCount = size(zoneApi().list());
        getAnonymousLogger().info(format("%s ::: zones: %s", manager, zoneCount));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            for (Iterator<ResourceRecordSetWithConfig<?>> rrsIterator = geoApi(zoneName).list(); rrsIterator.hasNext();) {
                ResourceRecordSetWithConfig<?> geoRRS = rrsIterator.next();
                checkGeoRRS(geoRRS);
                getAnonymousLogger().info(format("%s ::: geoRRS: %s", manager, geoRRS));
                ResourceRecordSet<?> rrs = geoRRS.getResourceRecordSet();
                recordTypeCounts.getUnchecked(rrs.getType()).addAndGet(rrs.size());
                geoRecordCounts.getUnchecked(toConfig(Geo.class).apply(geoRRS)).addAndGet(rrs.size());
                
                Iterator<ResourceRecordSetWithConfig<?>> byNameAndType = geoApi(zoneName).listByNameAndType(rrs.getName(), rrs.getType());
                assertTrue(byNameAndType.hasNext(), "could not list by name and type: " + rrs);
                assertTrue(Iterators.elementsEqual(geoApi(zoneName).listByNameAndType(rrs.getName(), rrs.getType()), byNameAndType));
                
                Optional<ResourceRecordSetWithConfig<?>> byNameTypeAndGroup = geoApi(zoneName)
                        .getByNameTypeAndGroup(rrs.getName(), rrs.getType(), toConfig(Geo.class).apply(geoRRS).getGroupName());
                assertTrue(byNameTypeAndGroup.isPresent(), "could not lookup by name, type, and group: " + geoRRS);
                assertEquals(byNameTypeAndGroup.get(), geoRRS);
            }
        }
        logRecordSummary();
    }

    protected void checkGeoRRS(ResourceRecordSetWithConfig<?> geoRRS) {
        assertFalse(geoRRS.getConfig().isEmpty(), "Config absent: " + geoRRS);
        Geo geo = toConfig(Geo.class).apply(geoRRS);
        checkNotNull(geo.getGroupName(), "GroupName: Geo %s", geoRRS);
        assertTrue(!geo.getTerritories().isEmpty(), "Regions empty on Geo: " + geoRRS);
        
        ResourceRecordSet<?> rrs = geoRRS.getResourceRecordSet();
        checkNotNull(rrs.getName(), "Name: ResourceRecordSet %s", geoRRS);
        checkNotNull(rrs.getType(), "Type: ResourceRecordSet %s", geoRRS);
        checkNotNull(rrs.getTTL(), "TTL: ResourceRecordSet %s", geoRRS);
        
        if (geo.isNoResponse()) {
            assertTrue(rrs.isEmpty(), "Values present on no response ResourceRecordSet: " + geoRRS);
        } else {
            assertFalse(rrs.isEmpty(), "Values absent on ResourceRecordSet: " + geoRRS);
        }
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            Iterator<ResourceRecordSetWithConfig<?>> rrsIterator = geoApi(zoneName).list();
            if (!rrsIterator.hasNext())
                continue;
            ResourceRecordSetWithConfig<?> rrset = rrsIterator.next();
            String name = rrset.getResourceRecordSet().getName();
            List<ResourceRecordSetWithConfig<?>> withName = Lists.newArrayList();
            withName.add(rrset);
            while (rrsIterator.hasNext()) {
                rrset = rrsIterator.next();
                if (!name.equalsIgnoreCase(rrset.getResourceRecordSet().getName()))
                        break;
                withName.add(rrset);
            }
            List<ResourceRecordSetWithConfig<?>> fromApi = Lists.newArrayList(geoApi(zoneName).listByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertFalse(geoApi(zoneName).listByName("ARGHH." + zoneName).hasNext());
            break;
        }
    }

    @Test
    private void testListByNameAndTypeWhenNone() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertFalse(geoApi(zoneName).listByNameAndType("ARGHH." + zoneName, "TXT").hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameTypeAndGroupWhenAbsent() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertEquals(geoApi(zoneName).getByNameTypeAndGroup("ARGHH." + zoneName, "TXT", "Mars"), Optional.absent());
            break;
        }
    }
 
    private void logRecordSummary() {
        for (Entry<String, AtomicLong> entry : recordTypeCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
        for (Entry<Geo, AtomicLong> entry : geoRecordCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
    }

    private LoadingCache<String, AtomicLong> recordTypeCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<String, AtomicLong>() {
                public AtomicLong load(String key) throws Exception {
                    return new AtomicLong();
                }
            });

    private LoadingCache<Geo, AtomicLong> geoRecordCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<Geo, AtomicLong>() {
                public AtomicLong load(Geo key) throws Exception {
                    return new AtomicLong();
                }
            });

    protected GeoResourceRecordSetApi geoApi(String zoneName) {
        Optional<GeoResourceRecordSetApi> geoOption = manager.getApi().getGeoResourceRecordSetApiForZone(zoneName);
        if (!geoOption.isPresent())
            throw new SkipException("geo not available or not available in zone " + zoneName);
        return geoOption.get();
    }

}
