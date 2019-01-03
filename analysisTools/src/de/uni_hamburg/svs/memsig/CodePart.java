package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

/**
 * This class models a code part from an executable binary. For ELF binaries,
 * this will typically be used to store loadable code segments. However, the
 * class can also be used to store code sections or non-loadable segments.
 * 
 * A CodePart object itself does not have a specified page size. Instead,
 * the page size must be specified when calling methods where page size
 * influences the method's results.
 * 
 * @author Jens Lindemann
 */
public class CodePart implements Comparable<CodePart> {
	private SoftwareVersion _swVersion;
	// Software is available through _swVersion. Thus, we do not need a separate field here.
	private String _partName;
	private byte[] _bytes; // unpadded contents
	
	/**
	 * Creates a new CodePart object.
	 * 
	 * @param sv the SoftwareVersion the part belongs to
	 * @param partName the part's name (e.g. the segment name)
	 * @param partFile the File containing the part data
	 */
	public CodePart(SoftwareVersion sv, String partName, File partFile) {
		_partName = partName;
		_swVersion = sv;
		
		readFromFile(partFile);
	}
	
	/**
	 * Reads the part contents from file.
	 * 
	 * @param file File to read from
	 */
	private void readFromFile(File file) {
		try {
			_bytes = FileUtils.readFileToByteArray(file);
		} catch (IOException e) {
			System.err.println("Error when reading file " + file.getAbsolutePath());
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the contents of the part without padding.
	 * 
	 * @return part bytes, unpadded
	 */
	public byte[] getBytes() {
		return _bytes;
	}
	
	/**
	 * Returns the contents of the parts. Content will be padded so that its length
	 * is a multiple of the specified page size.
	 * 
	 * @param pageSize page size (for padding)
	 * @return content bytes, padded to pageSize
	 */
	public byte[] getBytes(int pageSize) {
		return Arrays.copyOfRange(_bytes, 0, this.numberOfPages(pageSize)*pageSize);
	}
	
	/**
	 * Returns the name of the part (e.g. the segment name).
	 * 
	 * @return the part's name
	 */
	public String getName() {
		return _partName;
	}
	
	/**
	 * Returns the contents of the part as an array of Page objects.
	 * 
	 * @param pageSize page size (for padding)
	 * @return Pages contained in the part
	 */
	public Page[] getPages(int pageSize) {
		Page[] pages = new Page[numberOfPages(pageSize)];
		for(int p = 0; p < numberOfPages(pageSize); p++) {
			pages[p] = getPage(p, pageSize);
		}
		return pages;
	}
	
	/**
	 * Gets a {@link Page} from the part.
	 * 
	 * @param page index of the {@link Page} to get
	 * @param pageSize page size
	 * @return the specified {@link Page} from the part
	 */
	public Page getPage(int page, int pageSize) {
		long pos = page*pageSize;
		byte[] bytes = getPageBytes(page, pageSize);
		Page p = new Page(bytes, this, pos);
		return p;
	}
	
	/**
	 * Returns the contents of the part as a 2D array, where the first dimension identifies
	 * the page number and the second the position within the page.
	 * 
	 * @param pageSize page size (for padding)
	 * @return contents of part as 2D array
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
	 * Gets the contents of a specific page of the part.
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
	 * Returns the length of the part in bytes.
	 * 
	 * @return length of the part in bytes.
	 */
	public int getLength() {
		return _bytes.length;
	}
	
	/**
	 * Returns the number of pages in the part for the specified page size.
	 * 
	 * @param pageSize page size
	 * @return the number of pages in the part
	 */
	public int numberOfPages(int pageSize) {
		int numPages = _bytes.length / pageSize;
		if((_bytes.length % pageSize) > 0) {
			numPages++;
		}
		
		return numPages;
	}
	
	/**
	 * Checks whether the contents of the part are equal to those
	 * of another part o.
	 * 
	 * @param o CodePart to compare to
	 * @return true if contents are equal, false otherwise
	 */
	public boolean contentsEqualTo(CodePart o) {
		// TODO Possible performance improvement: compute and store hash 
		// when creating object, then compare hashes before the detailed comparison.
		return Arrays.equals(_bytes, o._bytes);
	}
	
	/**
	 * Checks whether a {@link page} whose contents are equal to those of o is
	 * contained within the part.
	 * 
	 * @param o {@link Page} to be searched for
	 * @return true if an identical page is contained in the part, false otherwise
	 */
	public boolean pageContentIsInPart(Page o) {
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
	public int compareTo(CodePart o) {
		if(!(_swVersion.equals(o._swVersion))) {
			return _swVersion.compareTo(o._swVersion);
		} else {
			return _partName.compareTo(o._partName);
		}
	}

	/**
	 * This method will consider CodeParts associated to a SoftwareVersion with an
	 * identical part name to be identical, even if the part content is different.
	 * If you wish to compare the part contents, use {@link #contentsEqualTo(CodePart)}
	 * instead or in addition. 
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof CodePart)) {
			return false;
		} else {
			CodePart o = (CodePart)obj;
			if(!(_swVersion.equals(o._swVersion))) {
				return false;
			} else {
				return _partName.equals(o._partName);
			}
		}
	}
}
