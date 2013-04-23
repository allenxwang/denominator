package denominator.model;

import static denominator.model.ResourceRecordSetWithConfigs.configTypeEqualTo;
import static denominator.model.ResourceRecordSetWithConfigs.toConfig;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;

import denominator.model.rdata.AData;

@Test
public class ResourceRecordSetWithConfigsTest {

    ResourceRecordSet<AData> aRRS = ResourceRecordSet.<AData> builder()
                                                     .name("www.denominator.io.")
                                                     .type("A")
                                                     .ttl(3600)
                                                     .add(AData.create("1.1.1.1")).build();

    Geo geo = Geo.create("US-East", ImmutableList.of("US-MD", "US-VA"), false);

    ResourceRecordSetWithConfig<AData> geoRRS = ResourceRecordSetWithConfig.<AData> builder()
                                                                           .rrset(aRRS)
                                                                           .putConfig("geo", geo).build();

    public void configTypeEqualToReturnsFalseOnNull() {
        assertFalse(configTypeEqualTo(Geo.class).apply(null));
    }

    public void configTypeEqualToReturnsFalseOnDifferentType() {
        assertFalse(configTypeEqualTo(String.class).apply(geoRRS));
    }

    public void configTypeEqualToReturnsFalseOnAbsent() {
        assertFalse(configTypeEqualTo(Geo.class).apply(
                ResourceRecordSetWithConfig.<AData> builder().rrset(aRRS).build()));
    }

    public void configTypeEqualToReturnsTrueOnSameType() {
        assertTrue(configTypeEqualTo(Geo.class).apply(geoRRS));
    }

    public void toConfigReturnsNullOnNull() {
        assertEquals(toConfig(Geo.class).apply(null), null);
    }

    static final class Foo extends ForwardingMap<String, Object> {

        @Override
        protected Map<String, Object> delegate() {
            return null;
        }

    }

    public void toConfigReturnsNullOnDifferentType() {
        assertEquals(toConfig(Foo.class).apply(geoRRS), null);
    }

    public void toConfigReturnsNullOnAbsent() {
        assertEquals(toConfig(Geo.class).apply(ResourceRecordSetWithConfig.<AData> builder().rrset(aRRS).build()), null);
    }

    public void toConfigReturnsConfigOnSameType() {
        assertEquals(toConfig(Geo.class).apply(geoRRS), geo);
    }
}
