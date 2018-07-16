/*
 * Reads file1 to a buffer. After the specified amount of time has passed, file1 is overwritten by file2. The time to complete the overwrite operation is measured and logged into a log file.
 *
 * Usage: testdedup-single-auto <wait> <file1> <file2> <logfile> [offset]
 *
 * Author: Jens Lindemann
 */

#define _POSIX_C_SOURCE 200112L

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <time.h>

int main(int argc, char **argv) {
	char *filemem = NULL;
	char *filename;
	char *filename2;
	char *logfilename;
	unsigned int interval;
	long bufsize = 0;
	long offset = 0;
	// TODO implement argument parsing using argp/getopt
	if(argc >= 5) {
		interval = atoi(argv[1]);
		filename = argv[2];
		filename2 = argv[3];
		logfilename = argv[4];
		if(argc >= 6) {
			offset = atol(argv[5]);
		}
	} else {
		filename = "random.dat";
		filename2 = "random2.dat";
		logfilename = "testdedup.log";
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

			/* Allocate page-aligned buffer */
			int maret = posix_memalign((void **)&filemem, sysconf(_SC_PAGESIZE), bufsize);

			if(maret!=0) {
				// TODO error handling
				return 1;
			}

			/* Go back to the start of the file. */
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

		sleep(interval);

		struct timespec starttime, endtime;

		FILE *fp2 = fopen(filename2, "r");
		if(fp2 != NULL) {
			/* Move the pointer to the specified offset. */
			if(fseek(fp2, offset, SEEK_SET) != 0) { 
				// TODO error handling
				return 1;
			}

			clock_gettime(CLOCK_MONOTONIC, &starttime);
			size_t newLen = fread(filemem, sizeof(char), bufsize, fp2);
			clock_gettime(CLOCK_MONOTONIC, &endtime);

			if(newLen == 0) {
				fputs("Error reading file 2", stderr);
			} else {
				filemem[newLen] = '\0';
			}
		}
		fclose(fp2);

		// Calculate and display time needed for writing to memory
		uint64_t timeNeeded = ((endtime.tv_sec * 1000000000) + endtime.tv_nsec) - ((starttime.tv_sec * 1000000000) + starttime.tv_nsec);

		printf("Time: %i ns\n", timeNeeded);

		FILE *lfp = fopen(logfilename, "a");
		fprintf(lfp, "%u\n", timeNeeded);
		fclose(lfp);

		memset(filemem, 0, bufsize); // Nullify source
		free(filemem);

		return 0;
	} else {
		return 1;
	}
}
