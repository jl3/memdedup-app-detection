package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

/**
 * This class models a code section from an executable binary.
 * 
 * A CodeSection object itself does not have a specified page size. Instead,
 * the page size must be specified when calling methods where page size
 * influences the method's results.
 * 
 * @author Jens Lindemann
 */
public class CodeSection implements Comparable<CodeSection> {
	private SoftwareVersion _swVersion;
	// Software is available through _swVersion. We could introduce a separate field here, but this may lead to inconsistencies.
	private String _sectionName;
	private byte[] _bytes; // unpadded section contents
	
	/**
	 * Creates a new CodeSection object.
	 * 
	 * @param sv the SoftwareVersion the section belongs to
	 * @param secName the section's name
	 * @param secFile the File containing the section data
	 */
	public CodeSection(SoftwareVersion sv, String secName, File secFile) {
		_sectionName = secName;
		_swVersion = sv;
		
		readFromFile(secFile);
	}
	
	/**
	 * Reads the section contents from file.
	 * 
	 * @param file File to read from
	 */
	private void readFromFile(File file) {
		try {
			_bytes = FileUtils.readFileToByteArray(file);
		} catch (IOException e) {
			System.err.println("Error when reading section file " + file.getAbsolutePath());
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the contents of the section without padding.
	 * 
	 * @return section bytes, unpadded
	 */
	public byte[] getBytes() {
		return _bytes;
	}
	
	/**
	 * Returns the contents of the sections. Content will be padded so that its length
	 * is a multiple of the specified page size.
	 * 
	 * @param pageSize page size (for padding)
	 * @return section bytes, padded to pageSize
	 */
	public byte[] getBytes(int pageSize) {
		return Arrays.copyOfRange(_bytes, 0, this.numberOfPages(pageSize)*pageSize);
	}
	
	/**
	 * Returns the name of the section.
	 * 
	 * @return the section's name
	 */
	public String getName() {
		return _sectionName;
	}
	
	/**
	 * Returns the contents of the section as an array of Page objects.
	 * 
	 * @param pageSize page size (for padding)
	 * @return Pages contained in the section
	 */
	public Page[] getPages(int pageSize) {
		Page[] pages = new Page[numberOfPages(pageSize)];
		for(int p = 0; p < numberOfPages(pageSize); p++) {
			pages[p] = getPage(p, pageSize);
		}
		return pages;
	}
	
	/**
	 * Gets a {@link Page} from the section.
	 * 
	 * @param page index of the {@link Page} to get
	 * @param pageSize page size
	 * @return the specified {@link Page} from the section
	 */
	public Page getPage(int page, int pageSize) {
		long pos = page*pageSize;
		byte[] bytes = getPageBytes(page, pageSize);
		Page p = new Page(bytes, this, pos);
		return p;
	}
	
	/**
	 * Returns the contents of the section as a 2D array, where the first dimension identifies
	 * the page number and the second the position within the page.
	 * 
	 * @param pageSize page size (for padding)
	 * @return contents of section as 2D array
	 */
	public byte[][] getPagesBytes(int pageSize) {
		int numPages = this.numberOfPages(pageSize);
		byte[][] pages = new byte[numPages][];
		
		for(int p = 0; p < numPages; p++) {
			pages[p] = this.getPageBytes(p, pageSize);
		}
		
		return pages;
	}
	
	/**
	 * Gets the contents of a specific page of the section.
	 * 
	 * @param page index of the page to get
	 * @param pageSize page size
	 * @return the specified page as byte[]
	 */
	public byte[] getPageBytes(int page, int pageSize) {
		int from = page*pageSize;
		int to = (page+1)*pageSize;
		return Arrays.copyOfRange(_bytes, from, to);
	}
	
	/**
	 * Returns the length of the section in bytes.
	 * 
	 * @return length of the section in bytes.
	 */
	public int getLength() {
		return _bytes.length;
	}
	
	/**
	 * Returns the number of pages of the section for the specified page size.
	 * 
	 * @param pageSize page size
	 * @return the number of pages in the section
	 */
	public int numberOfPages(int pageSize) {
		int numPages = _bytes.length / pageSize;
		if((_bytes.length % pageSize) > 0) {
			numPages++;
		}
		
		return numPages;
	}
	
	/**
	 * Checks whether the contents of the section are equal to those
	 * of another section o.
	 * 
	 * @param o CodeSection to compare to
	 * @return true if contents are equal, false otherwise
	 */
	public boolean contentsEqualTo(CodeSection o) {
		// TODO Possible performance improvement: compute and store hash 
		// when creating object, then compare hashes before the detailed comparison.
		return Arrays.equals(_bytes, o._bytes);
	}
	
	/**
	 * Checks whether a {@link page} whose contents are equal to those of o is
	 * contained within the section.
	 * 
	 * @param o {@link Page} to be searched for
	 * @return true if an identical page is contained in the section, false otherwise
	 */
	public boolean pageContentIsInSection(Page o) {
		Page[] pages = getPages(o.getPageSize());
		for(Page p : pages) {
			if(p.contentsEqualTo(o)) {
				return true;
			}
		}
		// ... else
		return false;
	}

	@Override
	public int compareTo(CodeSection o) {
		if(!(_swVersion.equals(o._swVersion))) {
			return _swVersion.compareTo(o._swVersion);
		} else {
			return _sectionName.compareTo(o._sectionName);
		}
	}

	/**
	 * This method will consider CodeSections associated to a SoftwareVersion with an
	 * identical section name to be identical, even if the section content is different!
	 * If you wish to compare the section contents, use {@link #contentsEqualTo(CodeSection)}
	 * instead or in addition. 
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof CodeSection)) {
			return false;
		} else {
			CodeSection o = (CodeSection)obj;
			if(!(_swVersion.equals(o._swVersion))) {
				return false;
			} else {
				return _sectionName.equals(o._sectionName);
			}
		}
	}
}
