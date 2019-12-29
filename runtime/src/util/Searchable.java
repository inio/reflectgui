/*
 * Copyright 2009 Ian Rickard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
