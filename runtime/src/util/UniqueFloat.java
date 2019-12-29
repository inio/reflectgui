package util;

public class UniqueFloat implements Comparable<UniqueFloat> {
	public final float value;
	
	public UniqueFloat(float value) {
		this.value = value;
	}
	
	public int compareTo(UniqueFloat other) {
		if (value < other.value) return -1;
		if (value > other.value) return 1;
		if (hashCode() < other.hashCode()) return -1;
		if (hashCode() > other.hashCode()) return 1;
		return 0;
	}

}
