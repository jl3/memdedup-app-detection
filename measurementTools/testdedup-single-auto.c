/*
 * Reads file1 to a buffer. After the specified amount of time has passed,
 * file1 is overwritten by file2. The time to complete the overwrite
 * operation is measured and logged into a log file.
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
#include <stdbool.h>

int main(int argc, char **argv) {
	char *filemem = NULL;
	char *filename = NULL;
	char *filename2 = NULL;
	char *logfilename = NULL;
	unsigned int interval = 0;
	long bufsize = 0;
	long offset = 0;
	bool cache = false;
	int c;

	while((c = getopt(argc, argv, "1:2:i:l:o:c")) != -1) {
		switch(c) {
			case '1':
				filename = optarg;
				break;
			case '2':
				filename2 = optarg;
				break;
			case 'i':
				interval = atoi(optarg);
				break;
			case 'l':
				logfilename = optarg;
				break;
			case 'o':
				offset = atol(optarg);
				break;
			case 'c':
				cache = true;
				break;
			case '?':
				// TODO error messages for improper use
				return 4;
			default:
				// TODO error handling
				return 5;
		}
	}

	// Check for presence of parameters. Set defaults/print error.
	if(!filename) {
		fprintf(stderr, "Error: Argument -1 is required.");
		return 2;
	}

	if(!filename2) {
		fprintf(stderr, "Error: Argument -2 is required.");
		return 3;
	}

	if(!logfilename) {
		logfilename = "testdedup.log";
	}

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
			if(cache) {
				// Move to beginning of file
				if(fseek(fp2, 0, SEEK_SET) != 0) {
					// TODO error handling
					return 1;
				}
				
				// read file to tmp
				char *tmp = NULL;
				int maret = posix_memalign((void **)&tmp, sysconf(_SC_PAGESIZE), bufsize+offset);
				if(maret!=0) {
					// TODO error handling
					return 1;
				}
				size_t tmpLen = fread(tmp, sizeof(char), bufsize+offset, fp2);
				if(tmpLen == 0) {
					fputs("Error reading file 2", stderr);
				}
				char* srcPnt = tmp+offset;

				// get start time
				clock_gettime(CLOCK_MONOTONIC, &starttime);
				// copy from tmp to filemem
				memcpy((void*)filemem, (void*)srcPnt, bufsize);
				// get end time
				clock_gettime(CLOCK_MONOTONIC, &endtime);
			} else {
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
				}
			}
		}
		fclose(fp2);

		// Calculate and display time needed for writing to memory
		uint64_t timeNeeded = ((endtime.tv_sec * 1000000000) + endtime.tv_nsec) - ((starttime.tv_sec * 1000000000) + starttime.tv_nsec);

		printf("Time: %i ns\n", timeNeeded);

		FILE *lfp = fopen(logfilename, "a");
		fprintf(lfp, "%u\n", timeNeeded);
		fclose(lfp);

		// Overwrite source with zeroes to make sure that the data 
		// does not come to haunt us later...
		memset(filemem, 0, bufsize);
		free(filemem);

		return 0;
	} else {
		return 1;
	}
}
