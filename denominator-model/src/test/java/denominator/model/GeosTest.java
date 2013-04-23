package denominator.model;

import static denominator.model.config.Geos.groupNameEqualTo;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import denominator.model.config.Geo;

@Test
public class GeosTest {

    Geo geo = Geo.create("US-East", ImmutableList.of("US-MD", "US-VA"));

    public void groupNameEqualToReturnsFalseOnNull() {
        assertFalse(groupNameEqualTo("US-East").apply(null));
    }

    public void groupNameEqualToReturnsFalseOnDifferentType() {
        assertFalse(groupNameEqualTo("US-West").apply(geo));
    }

    public void groupNameEqualToReturnsTrueOnSameType() {
        assertTrue(groupNameEqualTo("US-East").apply(geo));
    }
}
