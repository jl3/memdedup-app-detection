
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
	char *source = NULL;
	char *filename;
	char *filename2;
	long bufsize = 0;
	if(argc >= 3) {
		filename = argv[1];
		filename2 = argv[2];
	} else {
		filename = "random.dat";
		filename2 = "random2.dat";
	}
	FILE *fp = fopen(filename, "r");
	if(fp != NULL) {
		/* Go to the end of the file. */
		if (fseek(fp, 0L, SEEK_END) == 0) {
			/* Get the size of the file. */
			bufsize = ftell(fp);
			if(bufsize == -1) { /* Error */ }

			/* Allocate our buffer to that size. */
			source = malloc(sizeof(char) * (bufsize +1));

			/* Go back to the start of the file. */
			if(fseek(fp, 0L, SEEK_SET) != 0) { /* Error */ }

			/* Read the entire file into memory. */
			size_t newLen = fread(source, sizeof(char), bufsize, fp);
			if(newLen == 0) {
				fputs("Error reading file", stderr);
			} else {
				source[newLen] = '\0' ; /* Just to be safe. */
			}
		}
		fclose(fp);

		getchar(); // Wait for input before continuing...

		struct timespec starttime, endtime;

		FILE *fp2 = fopen(filename2, "r");
		if(fp2 != NULL) {
			clock_gettime(CLOCK_MONOTONIC, &starttime);
			size_t newLen = fread(source, sizeof(char), bufsize, fp2);
			clock_gettime(CLOCK_MONOTONIC, &endtime);

			if(newLen == 0) {
				fputs("Error reading file 2", stderr);
			} else {
				source[newLen] = '\0';
			}
		}
		fclose(fp2);

		// Calculate and display time needed for writing to memory
		uint64_t timeNeeded = ((endtime.tv_sec * 1000000000) + endtime.tv_nsec) - ((starttime.tv_sec * 1000000000) + starttime.tv_nsec);
		printf("Time: %i ns\n", timeNeeded);

		memset(source, 0, bufsize); // Nullify source
		free(source);
	}
}
