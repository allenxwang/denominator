package denominator.model.config;

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

    public static Geo create(String groupName, List<String> territories) {
        return new Geo(groupName, territories);
    }

    private final String configType;
    private final String groupName;
    private final List<String> territories;

    @ConstructorProperties({ "groupName", "territories"})
    private Geo(String groupName, List<String> territories) {
        this.configType = "geo";
        this.groupName = checkNotNull(groupName, "groupName");
        checkNotNull(territories, "territories");
        this.territories = ImmutableList.copyOf(territories);
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("configType", configType)
                                    .put("groupName", groupName)
                                    .put("territories", territories).build();
    }

    public String getConfigType() {
        return configType;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<String> getTerritories() {
        return territories;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
