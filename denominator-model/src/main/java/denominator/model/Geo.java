package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to a directional record set configuration.
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * Geo config = Geo.create("east", "mail.jclouds.org");
 * </pre>
 */
public class Geo extends ForwardingMap<String, Object> {

    public static Geo create(String groupName, List<String> territories, boolean noResponse) {
        return new Geo(groupName, territories, noResponse);
    }

    private final String groupName;
    private final List<String> territories;
    private final boolean noResponse;

    @ConstructorProperties({ "groupName", "territories", "noResponse" })
    private Geo(String groupName, List<String> territories, boolean noResponse) {
        this.groupName = checkNotNull(groupName, "groupName");
        checkNotNull(territories, "territories");
        this.territories = ImmutableList.copyOf(territories);
        this.noResponse = noResponse;
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("groupName", groupName)
                                    .put("territories", territories)
                                    .put("noResponse", noResponse).build();
    }

    public String getGroupName() {
        return groupName;
    }

    public List<String> getTerritories() {
        return territories;
    }

    public boolean isNoResponse() {
        return noResponse;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
