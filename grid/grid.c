/*
 * Determine Grid Square
 *
 * Steve Buer, N7MKO
 *
 * The program is free.
 *
 * Based on the methods described at:
 *
 * http://www.newsvhf.com/my-grid.html
 * https://en.wikipedia.org/wiki/Maidenhead_Locator_System
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#define ASCII_UPPER 65
#define ASCII_LOWER 97

float longitude, latitude;

int main(int argc, char *argv[])
{
	int i;
	float f1, f2, f_lat, f_lon;
	char g1, g2, g3, g4, g5, g6;
	
	if (argc < 3)
	{
		fprintf(stderr, "usage: %s LON LAT\n", argv[0]);
		exit(1);
	}

	longitude = atof(argv[1]);
	latitude = atof(argv[2]);

	/* longitude */

	f_lon = 180 + longitude;

	i = (int) truncf(f_lon / 20);

	g1 = ASCII_UPPER + i;

	// 3rd char
	
	f1 = f_lon - (i * 20);

	f2 = truncf(f1/2);

	g3 = (int) f2;

	// 5th char
	
	g5 = ASCII_LOWER + (int) truncf((f1/2 - f2) * 24);
	
	/* latitude */

	f_lat = 90 + latitude;

	i = (int) truncf(f_lat / 10);

	g2 = ASCII_UPPER + i;

	// 4th char

	f1 = f_lat - (i * 10);

	f2 = truncf(f1);

	g4 = (int) f2;

	// 6th char
	
	g6 = ASCII_LOWER + (int) truncf((f1 - f2) * 24);

	/* print */
	
	printf("%c%c%d%d%c%c\n", g1, g2, g3, g4, g5, g6);

	return 0;
}
