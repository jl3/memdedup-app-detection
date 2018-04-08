package de.uni_hamburg.svs.memsig;

/**
 * This class models the results of a comparison between two {@link SoftwareVersion}s.
 * 
 * @author Jens Lindemann
 */
public class VersionComparisonResult {
	private SoftwareVersion _swVers;
	private SoftwareVersion _compVers;
	private int _matches; // number of matching pages
	private int _unique; // number of unique pages
	private int _internalDups; // number of internal duplicates
	
	/**
	 * Creates a new VersionComparisonResult.
	 * 
	 * @param swVers original {@link SoftwareVersion}
	 * @param compVers {@link SoftwareVersion} that swVers is compared to
	 * @param matches number of matching Pages
	 * @param unique number of unique Pages
	 * @param internalDups number of internal duplicate Pages
	 */
	public VersionComparisonResult(SoftwareVersion swVers, SoftwareVersion compVers, int matches, int unique, int internalDups) {
		_swVers = swVers;
		_compVers = compVers;
		_matches = matches;
		_unique = unique;
		_internalDups = internalDups;
	}

	/**
	 * Returns the original {@link SoftwareVersion}.
	 * 
	 * @return the original {@link SoftwareVersion}
	 */
	public SoftwareVersion getVersion() {
		return _swVers;
	}
	
	/**
	 * Returns the comparison {@link SoftwareVersion}.
	 * 
	 * @return the comparison {@link SoftwareVersion}
	 */
	public SoftwareVersion getCompVersion() {
		return _compVers;
	}
	
	/**
	 * Returns the number of matching {@link Page}s, i.e. the number of Pages
	 * in swVers that is also present in compVers. 
	 * 
	 * @return number of matching Pages
	 */
	public int numberOfMatches() {
		return _matches;
	}
	
	/**
	 * Returns the number of unique {@link Page}s, i.e. the number of Pages in
	 * swVers that are not also present in compVers.
	 * 
	 * @return number of unique Pages
	 */
	public int numberOfUniques() {
		return _unique;
	}
	
	/**
	 * Returns the number of internal duplicate {@link Page}s, i.e. Pages that
	 * are present more than once in swVers.
	 * 
	 * @return number of internal duplicate Pages
	 */
	public int numberOfInternalDuplicates() {
		return _internalDups;
	}
}
