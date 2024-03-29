/**
 * This code based on code at https://code.google.com/archive/p/force-metadata-jdbc-driver/ released under the New BSD Licence
 */

package com.stuntbyte.salesforce.jdbc.metaforce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ColumnMap<K, V> extends HashMap<K, V> {

    private static final long serialVersionUID = -4362348575904029532L;

    private List<K> columnNames = new ArrayList<K>();
	private int columnPostion;

	@Override
	public V put(K key, V value) {
        if (key instanceof String) {
            String k2 = (String) key;
            key = (K) k2.toUpperCase();
        }
		columnNames.add(columnPostion++, key);
		return super.put(key, value);
	};

	/**
	 * Get a column value by index, starting at 1, that represents the insertion order into the map.
	 */
	public V getValueByIndex(int index) {
		return get(columnNames.get(index - 1));
	}

    /**
     * Get a column name by index, starting at 1, that represents the insertion order into the map.
     */
    public V getColumnNameByIndex(int index) {
        return (V) columnNames.get(index - 1);
    }

}
