//PixelDrawing.cpp

void drawLine(byte* image, const int width, const int height, int x0, int y0, int x1, int y1, const int color) {
	int dx = 1;
	int a = x1 - x0;
	if(a < 0) {
		dx = -1;
		a = -a;
	}
	
	int dy = 1;
	int b = y1 - y0;
	if(b < 0) {
		dy = -1;
		b = -b;
	}
	
	int two_a = 2 * a, two_b = 2 * b;
	int xcrit = two_a - b;
	int eps = 0;
	
	for(;;) {
		if(x0 > 0 && x0 < width && y0 > 0 && y0 < height && image[x0 + width * y0] < color)
			image[x0 + width * y0] = color;
		if(x0 == x1 && y0 == y1)
			break;
		if(eps <= xcrit) {
			x0 += dx;
			eps += two_b;
		}
		if(eps >= a || a <= b) {
			y0 += dy;
			eps -= two_a;
		}
	}
}

/// Draw eight points corresponding to points on a circle
void drawCirclePoints(byte* image, const int width, const int height, const int xCenter, const int yCenter, const int x, const int y, const int color) {
	int xpix, ypix;
	
	xpix = xCenter + x;
	ypix = yCenter + y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - x;
	ypix = yCenter + y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter + x;
	ypix = yCenter - y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - x;
	ypix = yCenter - y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	if(x == y) //the mirror points are the same in this case, don't bother
		return;
	xpix = xCenter + y;
	ypix = yCenter + x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - y;
	ypix = yCenter + x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter + y;
	ypix = yCenter - x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
	xpix = xCenter - y;
	ypix = yCenter - x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height && image[xpix + ypix * width] < color)
		image[xpix + ypix * width] = color;
}

/// Draw a circle in pixels, using the Bresenham Algorithm
void drawCircle(byte* image, const int width, const int height, const int x0, const int y0, const int radius, const int color) {
	int x = 0;
	int y = radius;
	int p = 1 - radius;
	
	drawCirclePoints(image, width, height, x0, y0, x, y, color);
	while(x < y) {
		++x;
		if(p < 0)
			p += 2 * x + 1;
		else {
			--y;
			p += 2 * (x - y) + 1;
		}
		drawCirclePoints(image, width, height, x0, y0, x, y, color);
	}
}

void drawDisk(byte* image, const int width, const int height, const int x0, const int y0, const int radius, const int color) {
	int x = 0;
	int y = radius;
	int p = 1 - radius;
	
	drawCirclePoints(image, width, height, x0, y0, x, y, color);
	if(y > 0)
		drawLine(image, width, height, x0 - y + 1, y0 - x, x0 + y - 1, y0 - x, color);
	while(x < y) {
		++x;
		if(p < 0)
			p += 2 * x + 1;
		else {
			--y;
			p += 2 * (x - y) + 1;
		}
		drawCirclePoints(image, width, height, x0, y0, x, y, color);
		if(y > 0) {
			drawLine(image, width, height, x0 - y + 1, y0 - x, x0 + y - 1, y0 - x, color);
			drawLine(image, width, height, x0 - y + 1, y0 + x, x0 + y - 1, y0 + x, color);
		}
	}
}
