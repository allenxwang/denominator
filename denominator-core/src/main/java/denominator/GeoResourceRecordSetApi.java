package denominator;

import com.google.common.base.Optional;

import denominator.model.Geo;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSetWithConfig;

public interface GeoResourceRecordSetApi extends ResourceRecordSetWithConfigApi<Geo> {

    /**
     * retrieve a resource record set by name, type, and geo group
     * 
     * @param name
     *            {@link ResourceRecordSet#getName() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#getType() type} of the rrset
     * @param group
     *            {@link Geo#getGroupName() group name} of the rrset
     * 
     * @return present if a resource record exists with the same {@code name},
     *         {@code type}, and {@code group}
     * @throws IllegalArgumentException
     *             if the {@code zoneName} is not found.
     */
    Optional<ResourceRecordSetWithConfig<?>> getByNameTypeAndGroup(String name, String type, String group);

    static interface Factory {
        Optional<GeoResourceRecordSetApi> create(String zoneName);
    }
}
