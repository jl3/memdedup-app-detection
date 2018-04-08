
/*
 * Reads a file to a buffer and then continues to run until terminated
 * code taken from http://stackoverflow.com/questions/140029524/c-programming-how-to-read-the-whole-file-contents-into-a-buffer
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <time.h>

int main(int argc, char **argv) {
	char **filemem;
	char **filenames1;
	char **filenames2;
	char **lognames;
	size_t *filesizes;
	long bufsizes*;

	int interval;
	int numTests;
	int numFiles;

	FILE **fp1;
	FILE **fp2;
	FILE **lfp;

	if((argc >= 6) && ((argc % 3) == 0)) {
		numTests = argv[1];
		interval = argv[2];

		numFiles = (argc/3)-1;
		filenames1 = malloc(numFiles*sizeof(char*));
		filenames2 = malloc(numFiles*sizeof(char*));
		lognames = malloc(numFiles*sizeof(char*));
		filemem = malloc(numFiles*sizeof(char*));
		filesizes = malloc(numFiles*sizeof(size_t*));
		bufsizes = malloc(numFiles*sizeof(long*));

		fp1 = malloc(numFiles*sizeof(FILE*));
		fp2 = malloc(numFiles*sizeof(FILE*));
		lfp = malloc(numFiles*sizeof(FILE*));

		for(int i = 0; i < numFiles; i++) {
			filenames1[i] = argv[(i+1)*3];
			filenemes2[i] = argv[((i+1)*3)+1];
			lognames[i] = argv[((i+1)*3)+2];

			//fp1[i] = fopen(filenames1[i], "r");
			//fp2[i] = fopen(filenames2[i], "r");
			//lfp[i] = fopen(lognames1[i], "r");
		}
	} else {
		printf("Syntax: testdedup-multi <numTests> <interval> <file1_1> <file1_2> <log1> ... <fileN_1> <fileN_2> <logN>\n");
		exit(1);
	}

	for(int i = 0; i < numTests; i++) {
		// load file1
		for(int f = 0; f < numFiles; f++) {
			fp1[f] = fopen(filenames1[f], "r");
			if(fp1[f] != NULL) {
				/* Go to the end of the file. */
				if (fseek(fp1[f], 0L, SEEK_END) == 0) {
					/* Get the size of the file. */
					bufsizes[f] = ftell(fp1[f]);
					if(bufsizes[f] == -1) { /* Error */ }

					/* Allocate our buffer to that size. */
					filemem[f] = malloc(sizeof(char) * (bufsizes[f])); 
					// was originally bufsize+1, but we do not want an extra 0 byte...

					/* Go back to the start of the file. */
					if(fseek(fp1[f], 0L, SEEK_SET) != 0) { /* Error */ }

					/* Read the entire file into memory. */
					filesizes[f] = fread(source, sizeof(char), bufsizes[f], fp1[f]);
					if(newLen == 0) {
						fputs("Error reading file", stderr);
					} else {
						//filemem[f][newLen] = '\0' ; /* Just to be safe. */
					}
				}
			fclose(fp);
			}
		}

		// wait
		sleep(interval);

		// overwrite with file 2, measure time
		for(int f = 0; f < numFiles; f++) {
			struct timespec starttime, endtime;

			FILE fp2[f] = fopen(filenames2[f], "r");
			if(fp2[f] != NULL) {
				clock_gettime(CLOCK_MONOTONIC, &starttime);
				size_t newLen = fread(filemem[f], sizeof(char), bufsizes[f], fp2);
				clock_gettime(CLOCK_MONOTONIC, &endtime);
	
				if(newLen == 0) {
					fputs("Error reading file 2", stderr);
				} else {
					//source[newLen] = '\0';
				}
			}
			fclose(fp2[f]);
			
			uint64_t timeNeeded = ((endtime.tv_sec * 1000000000) + endtime.tv_nsec) - ((starttime.tv_sec * 1000000000) + starttime.tv_nsec);
			printf("Time: %i ns\n", timeNeeded);

			memset(filemem[f], 0, bufsizes[f]); // Nullify source
			free(filemem[f]);
		}
	}
}
