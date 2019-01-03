package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class represents a signature for a {@link SoftwareVersion}.
 * 
 * @author Jens Lindemann
 */
public class VersionSignature implements Comparable<VersionSignature> {
	private SoftwareVersion[] _versions; // TODO might be better to put this in a Set. OR need to specify that this needs to be ordered
	private int _pageSize;
	private ArrayList<Page> _pages;
	private int _all01count;
	private int _intDupCount;
	private int _othVerDups;
	private int _notMatchingInGroupCount;
	
	/**
	 * Creates a new VersionSignature.
	 * 
	 * @param versions {@link SoftwareVersion}s the signature is for
	 * @param pageSize the page size
	 * @param all01count the number of {@link Page}s containing only 0- or 1-bits
	 * @param intDupCount the number of internal duplicate Pages
	 */
	public VersionSignature(SoftwareVersion[] versions, int pageSize, int all01count, int intDupCount, int notMatchingInGroupCount) {
		_versions = versions;
		_pageSize = pageSize;
		_pages = new ArrayList<Page>();
		_all01count = all01count;
		_intDupCount = intDupCount;
		_notMatchingInGroupCount = notMatchingInGroupCount;
	}
	
	/**
	 * Adds a new {@link Page} to the signature.
	 * 
	 * @param pageBytes contents of the Page
	 * @param section {@link CodePart} that the Page belongs to
	 * @param pos offset of the Page within the CodePart
	 */
	public void addPage(byte[] pageBytes, CodePart section, long pos) {
		Page page = new Page(pageBytes, section, pos);
		addPage(page);
	}
	
	/**
	 * Adds a new {@link Page} to the signature.
	 * 
	 * @param page the {@link Page} to add
	 */
	public void addPage(Page page) {
		_pages.add(page);
	}
	
	/**
	 * Returns the contents of the {@link Page}s in the signature as 2D byte array.
	 * 
	 * @return contents of the {@link Page}s in the signature
	 */
	public byte[][] getPagesBytes() {
		byte[][] pbytes = new byte[_pages.size()][_pageSize];
		for(int i = 0; i < numberOfPages(); i++) {
			pbytes[i] = getPageBytes(i);
		}
		return pbytes;
	}
	
	/**
	 * Returns the {@link Page}s in the signature.
	 * 
	 * @return the {@link Page}s in the signature
	 */
	public Page[] getPages() {
		return _pages.toArray(new Page[0]);
	}
	
	/**
	 * Returns the contents of a specific {@link Page} of the signature.
	 * 
	 * @param pageNum index of the Page to get
	 * @return contents of the specified Page
	 */
	public byte[] getPageBytes(int pageNum) {
		Page p = getPage(pageNum);
		return p.getBytes();
	}
	
	/**
	 * Returns a specific {@link Page} of the signature.
	 * 
	 * @param pageNum index of the Page to get
	 * @return the specified Page
	 */
	public Page getPage(int pageNum) {
		return _pages.get(pageNum);
	}
	
	/**
	 * Returns the number of {@link Page}s in the signature.
	 * 
	 * @return number of {@link Page}s in the signature
	 */
	public int numberOfPages() {
		return _pages.size();
	}
	
	/**
	 * Returns the @{SoftwareVersion}s that the signature is for.
	 * 
	 * @return @{SoftwareVersion}s that the signature is for
	 */
	public SoftwareVersion[] getSoftwareVersions() {
		return _versions;
	}
	
	/**
	 * Returns the page size used for creating the signature.
	 * 
	 * @return the page size
	 */
	public int getPageSize() {
		return _pageSize;
	}
	
	/**
	 * Returns the number of pages containing only 0- or 1-bits.
	 * 
	 * @return number of pages containing only 0- or 1-bits
	 */
	public int getAll01Count() {
		return _all01count;
	}
	
	/**
	 * Returns the number of internal duplicate {@link Page}s.
	 * 
	 * @return number of internal duplicate {@link Page}s
	 */
	public int getIntDupCount() {
		return _intDupCount;
	}
	
	/**
	 * Sets the number of {@link Page}s that are also present in other versions
	 * of the {@link Software}.
	 * 
	 * @param othVerDups number of Pages that are also present in other versions
	 */
	public void setOtherVersionDups(int othVerDups) {
		_othVerDups = othVerDups;
	}
	
	/**
	 * Returns the number of {@link Page}s that are also present in other versions
	 * of the {@link Software}.
	 * 
	 * @return number of Pages that are also present in other versions
	 */
	public int getOtherVersionDups() {
		return _othVerDups;
	}
	
	/**
	 * Returns the number of {@link Page}s that are present in the first version of the
	 * group, but that are not present in at least one other version in the group.
	 * 
	 * @return number of {@link Page}s present in first version, but not all versions
	 * of the group
	 */
	public int getNotMatchingInGroupCount() {
		return _notMatchingInGroupCount;
	}
	
	/**
	 * Writes the signature to a {@link File}.
	 * 
	 * @param f file to write to
	 * @throws FileNotFoundException
	 */
	public void writeToFile(File f) throws FileNotFoundException {
		FileOutputStream os = new FileOutputStream(f);
		try {
			for(Page p : _pages) {
				os.write(p.getBytes());
			}
		
			os.close();
		} catch (IOException e) {
			System.err.println("I/O error " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public int compareTo(VersionSignature o) {
		// TODO Auto-generated method stub
		int cmpres = 0;
		for(int i = 0; i < _versions.length; i++) {
			if(o._versions.length < (i+1)) {
				return -1;
			}
			
			cmpres = _versions[0].compareTo(o._versions[0]);
			if(cmpres != 0) {
				return cmpres;
			}
		}
		return cmpres;
	}
	

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof VersionSignature)) {
			return false;
		} else {
			VersionSignature o = (VersionSignature)obj;
			
			if(_pageSize != o._pageSize) {
				return false;
			}
			
			if(_versions.length != o._versions.length) {
				return false;
			}
			
			for(int i = 0; i < _versions.length; i++) {
				if(!_versions[i].equals(o._versions[i])) {
					return false;
				}
			}
			
			// We could check the signature pages and signature stats as well,
			// but these really should be identical...
			return true;
		}
	}
}
