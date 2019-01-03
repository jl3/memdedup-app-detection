package de.uni_hamburg.svs.memsig;

import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

/**
 * This class represents a group of {@link SoftwareVersion}s.
 * 
 * @author Jens Lindemann
 */
public class SoftwareVersionGroup {
	private TreeSet<SoftwareVersion> _versions;
	
	/**
	 * Instantiates a new SoftwareVersionGroup with the specified 
	 * {@link SoftwareVersion}s.
	 * 
	 * @param versions {@link SoftwareVersions} to assign to group
	 */
	public SoftwareVersionGroup(SoftwareVersion[] versions) {
		_versions = new TreeSet<SoftwareVersion>();
		for(int i = 0; i < versions.length; i++) {
			this.addVersion(versions[i]);
		}
	}
	
	/**
	 * Computes the average distance between versions in the group according to
	 * the canonical ordering of version strings.
	 * 
	 * @return average distance between versions
	 */
	public double getAvgVersionDistance() {
		SoftwareVersion[] gvs = this.toArray();
		int[] gvsidx = new int[gvs.length];
		SoftwareVersion[] avs = gvs[0].getSoftware().getVersions().toArray(new SoftwareVersion[0]);
		
		for(int i = 0; i < gvs.length; i++) {
			gvsidx[i] = ArrayUtils.indexOf(avs, gvs[i]);
		}
		
		if(gvs.length < 2) {
			return 0;
		}
		
		long sum = 0;
		long count = 0;
		
		for(int i = 0; (i+1) < gvs.length; i++) {
			for(int j = i+1; j < gvs.length; j++) {
				long dist = Math.abs(gvsidx[j] - gvsidx[i]);
				sum += dist;
				count++;
			}
		}
		
		double avgDist = (double)sum / count;
		return avgDist;
	}
	
	/**
	 * Computes the maximum distance between two versions in the group
	 * according to the canonical ordering of version strings.
	 * 
	 * @return maximum distance between two versions in the group
	 */
	public int getMaxVersionDistance() {
		SoftwareVersion[] avs = _versions.first().getSoftware().getVersions().toArray(new SoftwareVersion[0]);
		
		SoftwareVersion first = _versions.first();
		SoftwareVersion last = _versions.last();
		
		int firstIdx = ArrayUtils.indexOf(avs, first);
		int lastIdx = ArrayUtils.indexOf(avs, last);
		
		int maxDist = Math.abs(lastIdx - firstIdx);
		return maxDist;
	}
	
	/**
	 * Computes how many versions exist between the first and last version
	 * of the group that are not part of this group (according to the canonical
	 * ordering of version strings).
	 * 
	 * @return number of skipped versions
	 */
	public int getSkippedVersionCount() {
		SoftwareVersion[] avs = _versions.first().getSoftware().getVersions().toArray(new SoftwareVersion[0]);
		SoftwareVersion[] gvs = this.toArray();
		
		int firstIdx = ArrayUtils.indexOf(avs, _versions.first());
		int lastIdx = ArrayUtils.indexOf(avs, _versions.last());
		
		int skippedVersions = 0;
		for(int i = firstIdx; i < lastIdx; i++) {
			if(!ArrayUtils.contains(gvs, avs[i])) {
				skippedVersions++;
			}
		}
		
		return skippedVersions;
	}
	
	/**
	 * @return a SoftwareVersion[] containing all {@link SoftwareVersion}s
	 * 			in the group.
	 */
	public SoftwareVersion[] toArray() {
		return _versions.toArray(new SoftwareVersion[0]);
	}
	
	/**
	 * Adds a {@link SoftwareVersion} to the group.
	 * 
	 * @param sv {@link SoftwareVersion} to add
	 */
	public void addVersion(SoftwareVersion sv) {
		_versions.add(sv);
	}
	
	/**
	 * Returns the number of versions in the group.
	 * 
	 * @return number of versions in group
	 */
	public int size() {
		return _versions.size();
	}
}
