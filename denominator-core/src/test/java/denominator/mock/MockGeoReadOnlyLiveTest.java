package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseGeoReadOnlyLiveTest;

@Test
public class MockGeoReadOnlyLiveTest extends BaseGeoReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new MockConnection().manager;
    }
}
