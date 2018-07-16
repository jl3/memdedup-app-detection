/*
 * Reads a file to a buffer and then continues to run until terminated by pressing a key.
 *
 * Usage: loadfile <file> [offset]
 *
 * Author: Jens Lindemann
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>

int main(int argc, char **argv) {
	char *filemem = NULL;
	char *filename;
	long bufsize = 0;
	long offset = 0;
	if(argc >= 2) {
		filename = argv[1];
		if(argc >= 3) {
			offset = atol(argv[2]);
		}
	} else {
		filename = "random.dat";
	}

	// uses code for loading files into memory from http://stackoverflow.com/questions/140029524/c-programming-how-to-read-the-whole-file-contents-into-a-buffer
	FILE *fp = fopen(filename, "r");
	if(fp != NULL) {
		/* Go to the end of the file. */
		if (fseek(fp, 0L, SEEK_END) == 0) {
			/* Get the size of the file. */
			bufsize = ftell(fp);
			if(bufsize == -1) { /* Error */ }
			bufsize -= offset; // offset bytes will not be loaded

			/* Allocate our buffer to that size. */
			int maret = posix_memalign((void **)&filemem, sysconf(_SC_PAGESIZE), bufsize);

			if(maret!=0) {
				// TODO error handling
				return 1;
			}

			/* Go back to the start of the file, taking the offset into account. */
			if(fseek(fp, offset, SEEK_SET) != 0) { 
				// TODO error handling
				return 1;
			}

			/* Read the entire file into memory. */
			size_t newLen = fread(filemem, sizeof(char), bufsize, fp);
			if(newLen == 0) {
				fputs("Error reading file", stderr);
			}
		}
		fclose(fp);

		getchar(); // Wait for input before continuing...

		memset(&filemem, 0, strlen(filemem)); // Overwrite file memory
		free(filemem);
	}
}
