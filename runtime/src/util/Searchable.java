package util;

import java.util.Collection;

public interface Searchable<K, V, Q> {
	public static interface Entry<Ke, Ve, Qe> {
		public Ke getKey();
		public Ve getValue();
		public float distance(Qe other);
		public float keyDistance(Ke other);
	}

	public Entry<K,V,Q> add(K key, V value);
	public void remove(K key);
	public Entry<K,V,Q> nearest(Q q);
	public Entry<K,V,Q> keyNearest(K q);
	public Collection<? extends Entry<K,V,Q>> nearest(Q q, int n);
	public int count(Q q, float radius);
	public int count();
	public Collection<? extends Entry<K,V,Q>> collect();
	public void optimize();
	
	public Searchable<K,V,Q> emptyClone();
}
