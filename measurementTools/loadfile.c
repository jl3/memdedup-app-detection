
/*
 * Reads a file to a buffer and then continues to run until terminated
 * code taken from http://stackoverflow.com/questions/140029524/c-programming-how-to-read-the-whole-file-contents-into-a-buffer
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>

int main(int argc, char **argv) {
	char *source = NULL;
	char *filename;
	long bufsize = 0;
	if(argc >= 2) {
		filename = argv[1];
	} else {
		filename = "random.dat";
	}
	FILE *fp = fopen(filename, "r");
	if(fp != NULL) {
		/* Go to the end of the file. */
		if (fseek(fp, 0L, SEEK_END) == 0) {
			/* Get the size of the file. */
			bufsize = ftell(fp);
			if(bufsize == -1) { /* Error */ }

			/* Allocate our buffer to that size. */
			source = malloc(sizeof(char) * (bufsize));
			// was originally bufsize+1, but we do not want an extra 0 byte...

			/* Go back to the start of the file. */
			if(fseek(fp, 0L, SEEK_SET) != 0) { /* Error */ }

			/* Read the entire file into memory. */
			size_t newLen = fread(source, sizeof(char), bufsize, fp);
			if(newLen == 0) {
				fputs("Error reading file", stderr);
			} else {
				//source[++newLen] = '\0' ; /* Just to be safe. */
			}
		}
		fclose(fp);

		getchar(); // Wait for input before continuing...

		memset(&source, 0, strlen(source)); // Nullify source
		free(source);
	}
}
